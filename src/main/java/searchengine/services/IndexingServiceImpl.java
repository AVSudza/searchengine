package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.CompleteIndexingResponse;
import searchengine.dto.indexing.ErrorReadPageResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.UserStopIndexingResponse;
import searchengine.exeptions.StopIndexingException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    public static volatile boolean isIndexing = false;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final JdbcTemplate jdbcTemplate;
    private final IndexPageServiceImpl indexPageService;

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        List<IndexingResponse> indexingResponseList = new ArrayList<>();
        if (isIndexing) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        isIndexing = true;

        List<ExecutorService> serviceList = new ArrayList<>();

        for (searchengine.config.Site site : sites.getSites()) {

            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            serviceList.add(service);

            SiteEntity indexingSiteEntity = cleanAndSetSiteInfo(site);
            service.execute(() -> {
                crawlingSitePages(indexingSiteEntity);
            });
        }

        response.setResult(true);
        response.setError("");
        serviceList.forEach(ExecutorService::shutdown);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (!isIndexing) {
            response.setResult(false);
            response.setError("Индексация не была запущена");
            return response;
        }

        isIndexing = false;

        response.setResult(true);
        response.setError("Индексация остановлена пользователем");
        return response;
    }

    private IndexingResponse crawlingSitePages(SiteEntity siteEntity) {
        PageEntity rootPageEntity = new PageEntity();
        rootPageEntity.setSite(siteEntity);
        rootPageEntity.setPath(siteEntity.getUrl());

        PageServiceImpl rootPage;
        try {
            rootPage = new PageServiceImpl(rootPageEntity, siteRepository, pageRepository);
            rootPage.getPageInfo();
            rootPage.savePageToDB();
            rootPage.findChildren();
        } catch (StopIndexingException e) {
            return new UserStopIndexingResponse();
        } catch (IOException e) {
            return new ErrorReadPageResponse(rootPageEntity.getPath());
        }

        ForkJoinPool pagePool = new ForkJoinPool();
        IndexingResponse pageResponse = pagePool
                .invoke(new SiteServiceImpl(rootPageEntity, rootPage.getChildren(),
                        pageRepository, siteRepository, indexPageService));

        boolean failIndexing = pageResponse.getClass().equals(UserStopIndexingResponse.class) ||
                               pageResponse.getClass().equals(ErrorReadPageResponse.class);
        if (failIndexing) {
            siteEntity.setStatus(StatusType.FAILED);
        }
        if (pageResponse.getClass().equals(CompleteIndexingResponse.class)) {
            siteEntity.setStatus(StatusType.INDEXED);
        }
        siteEntity.setLastError(pageResponse.getError());
        siteRepository.save(siteEntity);
        return pageResponse;
    }

    private SiteEntity cleanAndSetSiteInfo(searchengine.config.Site site) {
        int idSite = getIdSiteByUrl(site.getUrl());
        if (idSite > 0) {
            siteRepository.deleteById(idSite);
        }

        SiteEntity addedSiteEntity = new SiteEntity(StatusType.INDEXING,
                new Date(),"",site.getUrl(),site.getName());
//        addedSiteEntity.setSetPageEntity(Set.of()); //todo delete
//        addedSiteEntity.setSetLemma(Set.of()); //todo delete

        synchronized (siteRepository) {
            if (!siteRepository.existsSiteEntityByUrl(addedSiteEntity.getUrl())) {
                siteRepository.save(addedSiteEntity);
            }
        }
        return addedSiteEntity;
    }

    private int getIdSiteByUrl(String url) {
        String query = "SELECT id FROM site WHERE url = '" + url + "'";
        List<Integer> siteList = jdbcTemplate.query(query,
                (ResultSet rs, int id) -> rs.getInt("Id"));
        return (!siteList.isEmpty()) ? siteList.get(0) : 0;
    }
}
