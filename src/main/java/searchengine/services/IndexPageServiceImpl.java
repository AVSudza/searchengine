package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exeptions.StopIndexingException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexPageServiceImpl implements IndexPageService {
    private final SitesList sites;
    @Getter
    @Setter
    private PageServiceImpl pageService;
    @Getter
    @Setter
    private SiteEntity siteEntity;
    @Getter
    @Setter
    private PageEntity pageEntity;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public IndexingResponse addPage(String url) {
        IndexingResponse response = new IndexingResponse();
        try {
            Site siteForUrl = checkPageOnSites(url, sites);
            if (siteForUrl == null) {
                response.setResult(false);
                response.setError("Страница: " + url + " находится за пределами сайтов, \n" +
                        "указанных в конфигурационном файле\n");
            }
            pageService = makePageServiceForUrl(pageEntity.getSite(), pageEntity.getPath());
            pageService = deletePageLemmaIndexInfo(pageService);
            pageService.getPageInfo();
            pageService.savePageToDB();
            response = indexingPage(pageService);

        } catch (StopIndexingException e) {
            response.setResult(false);
            response.setError("Индексация остановлена пользователем");
        } catch (IOException e) {
            response.setResult(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    IndexingResponse indexingPage(PageServiceImpl pageService) {
        IndexingResponse response = new IndexingResponse();
        int codePage = pageService.getPageEntity().getCode();
        if (codePage < 400) {
            try {
                savePageLemmasToDB(pageService.getPageEntity());
                response.setResult(true);
                response.setError("");
                return response;
            } catch (IOException e) {
                response.setResult(false);
                response.setError("Ошибка при чтении страницы: " + pageService.getPageEntity().getPath() + "\n");
                return response;
            }
        } else {
            response.setResult(false);
            response.setError("Ошибка " + pageService.getPageEntity().getCode() +
                    " на странице: " + pageService.getPageEntity().getPath());
            return response;
        }
    }

    PageServiceImpl makePageServiceForUrl(SiteEntity site, String url) throws StopIndexingException, IOException {
        siteEntity = getSiteEntityByUrl(site.getUrl(), jdbcTemplate);
        if (siteEntity.getId() == 0) {
            siteEntity = makeSiteEntity(site.getUrl(), site.getName());
        }
        pageEntity = getPageEntityByUrl(url, jdbcTemplate);
        if (pageEntity.getId() == 0) {
            pageEntity = makePageEntity(url, siteEntity);
        }
        return new PageServiceImpl(pageEntity, siteRepository, pageRepository);
    }

    void savePageLemmasToDB(PageEntity pageEntity) throws IOException {
        if (pageEntity.getCode() == 0) {
            throw new IOException();
        }
        LemmaDetector lemmaDetector = new LemmaDetector(pageEntity.getContent());
        HashMap<String, Integer> pageLemmas = lemmaDetector.getMapLemmas();
        for (Map.Entry<String, Integer> pageLemma : pageLemmas.entrySet()) {
            Lemma lemma = new Lemma();
            lemma.setLemma(pageLemma.getKey());
            lemma.setSite(siteEntity);
            lemma.setIndexSet(Set.of());

            List<Lemma> lemmaList = findLemmaSite(pageLemma.getKey(), siteEntity.getId());

            if (!lemmaList.isEmpty()) {
                Lemma lemmaEntity = lemmaList.get(0);
                lemma.setFrequency(lemmaEntity.getFrequency() + 1);
                lemma.setId(lemmaEntity.getId());
            } else {
                lemma.setFrequency(1);
            }
            lemmaRepository.save(lemma);
            saveIndexToDB(lemma, pageEntity, pageLemma.getValue());
        }
    }

    void saveIndexToDB(Lemma lemma, PageEntity pageEntity, float rank) {
        Index index = new Index();
        index.setLemma(lemma);
        index.setPageEntity(pageEntity);
        index.setRank(rank);
        indexRepository.save(index);
    }

    @Override
    public List<Lemma> findLemmaSite(String lemma, int siteId) {
        return lemmaRepository.findByLemmaAndSiteId(lemma, siteId);
    }

    Site checkPageOnSites(String url, SitesList sites) throws StopIndexingException, IOException {
        Site urlSite = null;
        for (searchengine.config.Site site : sites.getSites()) {
            if (url.matches(site.getUrl() + ".*")) {
                urlSite = site;
            }
        }
        return urlSite;
    }

    SiteEntity makeSiteEntity(String url, String name) {
        SiteEntity madeSite = new SiteEntity(StatusType.INDEXING, new Date(), "", url, name);
        siteRepository.save(madeSite);
        return getSiteEntityByUrl(url, jdbcTemplate);
    }

    PageEntity makePageEntity(String url, SiteEntity siteEntity) throws StopIndexingException, IOException {
        PageEntity madePage = new PageEntity(siteEntity, url, 0, "");
        PageServiceImpl madePageService = new PageServiceImpl(madePage, siteRepository, pageRepository);
        madePageService.getPageInfo();
        madePageService.savePageToDB();
        return getPageEntityByUrl(url, jdbcTemplate);
    }

    SiteEntity getSiteEntityByUrl(String url, JdbcTemplate jdbcTemplate) {
        String query = "SELECT * FROM site WHERE url = '" + url + "'";
        SiteEntity siteEntity = null;
        try {
            siteEntity = jdbcTemplate.queryForObject(query, SiteEntityRowMapper.getInstance());
        } catch (EmptyResultDataAccessException ex) {
        }
        return (siteEntity == null) ? new SiteEntity() : siteEntity;
    }

    PageEntity getPageEntityByUrl(String path, JdbcTemplate jdbcTemplate) {
        String query = "SELECT * FROM page WHERE path = '" + path + "'";

        PageEntity pageEntity = null;
        try {
            pageEntity = jdbcTemplate.queryForObject(query, PageEntityRowMapper.getInstance());
        } catch (DataAccessException e) {
        }
        return (pageEntity == null) ? new PageEntity() : pageEntity;
    }

    PageServiceImpl deletePageLemmaIndexInfo(PageServiceImpl pageService) throws StopIndexingException {
        if (pageRepository.existsPageEntityByPath(pageService.getPageEntity().getPath())) {
            PageEntity newPageEntity = pageService.getPageEntity().clone();
            PageServiceImpl newPageService = new PageServiceImpl(newPageEntity,
                    siteRepository, pageRepository);
            synchronized (pageRepository) {
                pageService.deletePageFromDB();
                newPageService.savePageToDB();
            }
            return newPageService;
        }
        return pageService;
    }
}
