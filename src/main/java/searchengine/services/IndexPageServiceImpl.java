package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exeptions.StopIndexingException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexPageServiceImpl implements IndexPageService {
    private final SitesList sites;
    @Getter
    private IndexPageService indexPageService;
    @Getter
    private PageService pageService;
    @Getter
    @Setter
    private SiteDB siteDB;
    @Getter
    @Setter
    private PageDB pageDB;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public IndexingResponse addPage(String url) {
        IndexingResponse response = new IndexingResponse();

        try {
            if (!checkPageOnSites(url)) {
                response.setResult(false);
                response.setError("Страница: " + url + " находится за пределами сайтов, \n" +
                                  "указанных в конфигурационном файле\n");
                return response;
            }
            deletePageLemmaIndexInfo();
            pageService = new PageServiceImpl(pageDB, siteRepository, pageRepository, indexPageService);
        } catch (StopIndexingException e) {
            response.setResult(false);
            response.setError("Индексация остановлена пользователем");
            return response;
        } catch (IOException e) {
            response.setResult(false);
            response.setError(e.getMessage());
            return response;
        }
        if (pageService.getPageDB().getCode() < 400) {
            pageRepository.save(pageDB);
            try {
                saveLemmaToDB();
            } catch (IOException e) {
                response.setResult(false);
                response.setError("Ошибка при чтении страницы: " + pageDB.getPath() + "\n");
                return response;
            }
        }

        response.setResult(true);
        response.setError("");
        return response;
    }

    public void saveLemmaToDB() throws IOException {
        LemmaDetector lemmaDetector = new LemmaDetector(pageDB.getContent());
        HashMap<String, Integer> pageLemmas = lemmaDetector.getMapLemmas();
        for (Map.Entry<String, Integer> pageLemma : pageLemmas.entrySet()) {
            Lemma lemma = new Lemma();
            lemma.setLemma(pageLemma.getKey());
            lemma.setSite(siteDB);

            List<Lemma> lemmaList = findLemmaSite(pageLemma.getKey(), siteDB.getId());

            if (lemmaList.size() > 0) {
                Lemma lemmaDB = lemmaList.get(0);
                lemma.setFrequency(lemmaDB.getFrequency() + 1);
                lemma.setId(lemmaDB.getId());
            } else {
                lemma.setFrequency(1);
            }
            lemmaRepository.save(lemma);
            saveIndexToDB(lemma, pageDB, pageLemma.getValue());
        }
    }

    private void saveIndexToDB(Lemma lemma, PageDB pageDB, float rank) {
        Index index = new Index();
        index.setLemma(lemma);
        index.setPageDB(pageDB);
        index.setRank(rank);
        indexRepository.save(index);
    }

    @Override
    public List<Lemma> findLemmaSite(String lemma, int siteId) {
        return lemmaRepository.findByLemmaAndSiteId(lemma, siteId);
    }

    private boolean checkPageOnSites(String url) throws StopIndexingException, IOException {
        Pattern pattern = Pattern.compile("((http)|(https))://(www.)?.*\\.[^/\\s]+");
        Matcher matcher = pattern.matcher(url);
        String urlSite = "";
        while (matcher.find()) {
            urlSite = matcher.group(0);
        }

        boolean onSite = false;
        for (searchengine.config.Site site : sites.getSites()) {
            if (site.getUrl().matches(urlSite + "/?")) {
                onSite = true;
                siteDB = getSiteByUrl(urlSite);
                if (siteDB.getId() == 0) {
                    siteDB = makeSiteDB(site.getUrl(), site.getName());
                }
                pageDB = getPageDBByUrl(url);
                if (pageDB.getId() == 0) {
                    pageDB = makePageDB(url, siteDB);
                }
            }
        }
        return onSite;
    }

    private SiteDB makeSiteDB(String url, String name) {
        SiteDB madeSite = new SiteDB(StatusType.INDEXING, new Date(), "", url, name);
        siteRepository.save(madeSite);
        return getSiteByUrl(url);
    }

    private PageDB makePageDB(String url, SiteDB siteDB) throws StopIndexingException, IOException {
        PageDB madePage = new PageDB(siteDB, url, 0, "");
        pageRepository.save(madePage);
        PageServiceImpl madePageService = new PageServiceImpl(getPageDBByUrl(url),
                siteRepository, pageRepository, indexPageService);
        madePageService.getPageInfo();
        return madePageService.getPageDB();
    }

    private SiteDB getSiteByUrl(String url) {
        String query = "SELECT * FROM site WHERE url = '" + url + "'";
        List<SiteDB> siteDBList = jdbcTemplate.query(query,
                (ResultSet rs, int id) -> {
                    SiteDB existingSite = new SiteDB();
                    existingSite.setId(rs.getInt("Id"));
                    existingSite.setLastError(rs.getString("last_error"));
                    existingSite.setName(rs.getString("name"));
                    existingSite.setStatus(StatusType.valueOf(rs.getString("status")));
                    existingSite.setStatusTime(rs.getDate("status_time"));
                    existingSite.setUrl(rs.getString("url"));
                    return existingSite;
                });
        return (siteDBList.size() > 0) ? siteDBList.get(0) : new SiteDB();
    }

    private PageDB getPageDBByUrl(String path) {
        String query = "SELECT * FROM page WHERE path = '" + path + "'";
        List<PageDB> pageDBList = jdbcTemplate.query(query,
                (ResultSet rs, int id) -> {
                    PageDB existingPage = new PageDB();
                    existingPage.setId(rs.getInt("Id"));
                    existingPage.setCode(rs.getInt("code"));
                    existingPage.setContent(rs.getString("content"));
                    existingPage.setPath(rs.getString("path"));
                    existingPage.setSite(siteDB);
                    return existingPage;
                });
        return (pageDBList.size() > 0) ? pageDBList.get(0) : new PageDB();
    }

    private void deletePageLemmaIndexInfo() {
        if (pageRepository.existsPageDBByPath(pageDB.getPath())) {
            List<Lemma> lemmaList = lemmaRepository.findBySiteId(siteDB.getId());
            List<Index> pageDBList = indexRepository.findByPageDB(pageDB);
            if (pageDBList.size() > 0) {
                indexRepository.deleteAll(pageDBList);
            }
            if (lemmaList.size() > 0) {
                lemmaRepository.deleteAll(lemmaList);
            }

            PageDB newPageDB = pageDB.clone();

            pageRepository.delete(pageDB);
            pageRepository.save(newPageDB);
            pageDB = newPageDB;
        }
    }
}
