package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.CompleteIndexingResponse;
import searchengine.dto.indexing.ErrorReadPageResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.UserStopIndexingResponse;
import searchengine.model.PageDB;
import searchengine.repositories.PageRepository;
import searchengine.exeptions.StopIndexingException;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;

@Service
//@RequiredArgsConstructor
public class SiteServiceImpl extends RecursiveTask<IndexingResponse> implements SiteService {
    private PageDB pageDB;
    private List<PageDB> pageChildren;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexPageService indexPageService;

    public SiteServiceImpl(PageDB pageDB, List<PageDB> pageChildren,
                           PageRepository pageRepository, SiteRepository siteRepository, IndexPageService indexPageService) {
        this.pageDB = pageDB;
        this.pageChildren = pageChildren;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.indexPageService = indexPageService;
    }

    @Override
    protected IndexingResponse compute() {

        try {
            PageServiceImpl pageService = new PageServiceImpl(pageDB, siteRepository, pageRepository, indexPageService);
            pageService.savePageToDB();
        } catch (StopIndexingException e) {
            return new UserStopIndexingResponse();
        }

        List<SiteServiceImpl> childTaskList = new ArrayList<>();
        for (PageDB child : pageChildren) {

            SiteServiceImpl childTask;
            try {
                PageServiceImpl childService = new PageServiceImpl(child, siteRepository, pageRepository, indexPageService);
                if (childService.existsPageInDB()) {
                    continue;
                }
                childTask = new SiteServiceImpl(child, childService.findChildren(), pageRepository, siteRepository, indexPageService);
            } catch (StopIndexingException e) {
                return new UserStopIndexingResponse();
            } catch (IOException e) {
                return new ErrorReadPageResponse();
            }
            childTask.fork();
            childTaskList.add(childTask);
        }

        List<IndexingResponse> pageDBS = new ArrayList<>();
        for (SiteServiceImpl page : childTaskList) {
            pageDBS.add(page.join());
        }
        return new CompleteIndexingResponse();
    }
}
