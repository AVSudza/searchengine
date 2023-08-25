package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.CompleteIndexingResponse;
import searchengine.dto.indexing.ErrorReadPageResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.UserStopIndexingResponse;
import searchengine.exeptions.StopIndexingException;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;
import searchengine.model.StatusType;
import searchengine.repositories.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    public static volatile boolean isIndexing = false;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final JdbcTemplate jdbcTemplate;
    private final IndexPageService indexPageService;
    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (isIndexing) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        isIndexing = true;

        List<ExecutorService> serviceList = new ArrayList<>();
        List<Future<IndexingResponse>> listFuture = new ArrayList<>();

        for (searchengine.config.Site site : sites.getSites()) {

            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            serviceList.add(service);

            SiteDB indexingSiteDB = cleanAndSetSiteInfo(site);
            Future<IndexingResponse> listPageDB = service.submit(() -> {
                IndexingResponse indexingResponse = crawlingSitePages(indexingSiteDB);
                indexingSiteDB.setStatus((isIndexing) ? StatusType.INDEXED : StatusType.FAILED);
                siteRepository.save(indexingSiteDB);
                return indexingResponse;
            });
            listFuture.add(listPageDB);
        }

        listFuture.forEach(indexingResponseFuture -> {
            try {
                indexingResponseFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

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

    private IndexingResponse crawlingSitePages(SiteDB siteDB) {
        PageDB rootPageDB = new PageDB();
        rootPageDB.setSite(siteDB);
        rootPageDB.setPath(siteDB.getUrl());
        PageServiceImpl rootPage;
        try {
            rootPage = new PageServiceImpl(rootPageDB, siteRepository, pageRepository, indexPageService);
            rootPage.findChildren();
            rootPage.savePageToDB();
        } catch (StopIndexingException e) {
            return new UserStopIndexingResponse();
        } catch (IOException e) {
            return new ErrorReadPageResponse();
        }

        ForkJoinPool pagePool = new ForkJoinPool();
        IndexingResponse pageResponse = pagePool
                .invoke(new SiteServiceImpl(rootPageDB, rootPage.getChildren(),
                        pageRepository, siteRepository, indexPageService));

        boolean failIndexing = pageResponse.getClass().equals(UserStopIndexingResponse.class) ||
                               pageResponse.getClass().equals(ErrorReadPageResponse.class);
        if (failIndexing) {
            siteDB.setStatus(StatusType.FAILED);
        }
        if (pageResponse.getClass().equals(CompleteIndexingResponse.class)) {
            siteDB.setStatus(StatusType.INDEXED);
        }
        siteDB.setLastError(pageResponse.getError());
        siteRepository.save(siteDB);

        Logger.getLogger(this.getClass().getSimpleName()).info("rootPage: " + rootPageDB.getPath() + " complete");

        return pageResponse;
    }

    private SiteDB cleanAndSetSiteInfo(searchengine.config.Site site) {
        int idSite = getIdSiteByUrl(site.getUrl());
        if (idSite > 0) {
            siteRepository.deleteById(idSite);
        }

        SiteDB addedSiteDB = new SiteDB();
        addedSiteDB.setName(site.getName());
        addedSiteDB.setUrl(site.getUrl());
        addedSiteDB.setStatus(StatusType.INDEXING);
        addedSiteDB.setStatusTime(new Date());
        addedSiteDB.setLastError("");
        siteRepository.save(addedSiteDB);

        return addedSiteDB;
    }

    private int getIdSiteByUrl(String url) {
        String query = "SELECT id FROM Site WHERE url = '" + url + "'";
        List<Integer> siteList = jdbcTemplate.query(query,
                (ResultSet rs, int id) -> rs.getInt("Id"));
        return (siteList.size() > 0) ? siteList.get(0) : 0;
    }
}
