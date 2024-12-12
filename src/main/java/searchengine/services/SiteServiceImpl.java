package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.CompleteIndexingResponse;
import searchengine.dto.indexing.ErrorReadPageResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.UserStopIndexingResponse;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;
import searchengine.exeptions.StopIndexingException;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@Service
@Slf4j
public class SiteServiceImpl extends RecursiveTask<IndexingResponse> {
    private final PageEntity pageEntity;
    private final List<PageEntity> pageChildren;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexPageServiceImpl indexPageService;

    public SiteServiceImpl(PageEntity pageEntity, List<PageEntity> pageChildren,
                           PageRepository pageRepository, SiteRepository siteRepository,
                           IndexPageServiceImpl indexPageService) {
        this.pageEntity = pageEntity;
        this.pageChildren = pageChildren;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.indexPageService = indexPageService;
    }

    @Override
    protected IndexingResponse compute() {

        List<SiteServiceImpl> childTaskList = new ArrayList<>();
        for (PageEntity child : pageChildren) {

            SiteServiceImpl childTask;
            try {
                PageServiceImpl childService = new PageServiceImpl(child, siteRepository, pageRepository);
                synchronized (pageRepository) {
                    if (childService.existsPageInDB()) {
                        continue;
                    }
                    childService.getPageInfo();
                    childService.savePageToDB();
                }
                childService.findChildren();
                indexPageService.setPageService(childService);
                indexPageService.setPageEntity(childService.getPageEntity());
                indexPageService.setSiteEntity(childService.getPageEntity().getSite());
                indexPageService.indexingPage(childService);

                childTask = new SiteServiceImpl(child, childService.findChildren(), pageRepository, siteRepository, indexPageService);
            } catch (StopIndexingException e) {
                return new UserStopIndexingResponse();
            } catch (IOException e) {
                return new ErrorReadPageResponse(child.getPath());
            }
            childTask.fork();
            childTaskList.add(childTask);
        }
        return makeIndexingResponse(childTaskList);
    }

    private IndexingResponse makeIndexingResponse(List<SiteServiceImpl> taskList) {
        List<IndexingResponse> responseList = new ArrayList<>();
        for (SiteServiceImpl page : taskList) {
            responseList.add(page.join());
        }

        for (IndexingResponse response : responseList) {
            if (response.getClass().equals(UserStopIndexingResponse.class)) {
                return new UserStopIndexingResponse();
            }
            if (response.getClass().equals(ErrorReadPageResponse.class)) {
                return new ErrorReadPageResponse(((ErrorReadPageResponse) response).getPage());
            }
        }
        return new CompleteIndexingResponse();
    }
}
