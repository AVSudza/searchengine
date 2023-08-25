package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.*;
import searchengine.model.*;
import searchengine.repositories.AllRepositories;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final double LIMIT_OCCURRENCE = .5;
    private static final int LENGTH_SNIPPET = 240;
    private final SitesList sites;
    private final AllRepositories allRepositories;

    @Override
    public BasicResponse search(String searchQuery, String site) {
        if ("".equals(searchQuery)) {
            return new EmptySearchQueryResponse();
        }

        List<Lemma> searchLemmas;
        try {
            searchLemmas = this.getSearchLemmas(searchQuery);
            if (searchLemmas.size() == 0) {
                return new EmptySearchDataListResponse();
            }
        } catch (IOException e) {
            return new ErrorLibraryResponse();
        }

        List<SiteDB> siteDBList;
        try {
            siteDBList = allSitesIndexing(site);
        } catch (NullPointerException e) {
            return new ErrorIndexingResponse();
        }

        List<Index> containingIndex;
        List<PageDB> containingPages;
        List<LemmaPageDB> containingLemmaPageAll = new ArrayList<>();

        for (SiteDB siteSearch : siteDBList) {
            if (searchLemmas.size() > 0) {
                Lemma nextLemma = searchLemmas.get(0);

                if (site == null){
                    containingIndex = this.allRepositories.getIndexRepository().findByLemma(searchLemmas.get(0));
                } else {
                    containingIndex = this.allRepositories.getIndexRepository().findBySite(siteSearch.getId());
                }

                containingPages = containingIndex.stream().map(Index::getPageDB).toList();
                containingLemmaPageAll = containingPages.stream()
                        .map((pageDB) -> new LemmaPageDB(pageDB, nextLemma))
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            for (Lemma lemma : searchLemmas) {

                if (site == null) {
                    containingIndex = this.allRepositories.getIndexRepository().findByLemma(lemma);
                } else {
                    containingIndex = this.allRepositories.getIndexRepository().findBySite(siteSearch.getId());
                }

                containingPages = containingIndex.stream().map(Index::getPageDB).toList();
                List<LemmaPageDB> searchDataList = containingPages.stream()
                        .map((pageDB) -> new LemmaPageDB(pageDB, lemma)).toList();

                containingLemmaPageAll.retainAll(searchDataList);
            }
        }

        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(containingLemmaPageAll.size());

        float maxAbsoluteRelevance = 0.0F;
        List<SearchData> searchDataList = new ArrayList<>();

        SearchData searchResponse = new SearchData();
        for (LemmaPageDB lemmaPageDB : containingLemmaPageAll) {
            searchResponse = new SearchData();
            PageDB page = lemmaPageDB.getPageDB();
            searchResponse.setSite(page.getSite().getUrl());
            searchResponse.setSiteName(page.getSite().getName());
            searchResponse.setUri(this.getUri(page.getPath()));
            searchResponse.setTitle(this.getTitle(page.getContent()));
            searchResponse.setSnippet(this.getSnippet(page, lemmaPageDB.getLemma()));
            float absoluteRelevancePage = this.getAbsoluteRelevance(page, searchLemmas);
            searchResponse.setRelevance(absoluteRelevancePage);
            maxAbsoluteRelevance = Math.max(maxAbsoluteRelevance, absoluteRelevancePage);
            searchDataList.add(searchResponse);
        }
        for (SearchData searchData : searchDataList) {
            searchData.setRelevance(searchData.getRelevance() / maxAbsoluteRelevance);
        }

        searchDataList = this.sortSearchDataList(searchDataList);
        response.setData(searchDataList);
        return response;
    }

    private String getSnippet(PageDB page, Lemma lemma) {
        String content = page.getContent();
        String lemmaString = lemma.getLemma();
        content = deleteTags(content);
        int indexBeginLemma = content.indexOf(lemmaString);
        String substringBeforeLemma = content.substring(0, indexBeginLemma);
        int indexBeginSnippet = substringBeforeLemma.lastIndexOf(".") + 1;
        int indexEndSnippet = content.indexOf(" ", indexBeginSnippet + LENGTH_SNIPPET);
        return content.substring(indexBeginSnippet, indexEndSnippet);
    }

    private List<SearchData> sortSearchDataList(List<SearchData> searchDataList) {
        return searchDataList.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRelevance(), d1.getRelevance()))
                .toList();
    }

    private float getAbsoluteRelevance(PageDB page, List<Lemma> searchLemmas) {
        float sumRelevance = 0.0F;
        for (Lemma lemma : searchLemmas) {
            List<Index> indexList = this.allRepositories.getIndexRepository().findByLemmaAndPageDB(lemma, page);
            sumRelevance += indexList.get(0).getRank();
        }
        return sumRelevance;
    }

    private String getTitle(String content) {
        Pattern pattern = Pattern.compile("<title>(.+)</title>");
        Matcher matcher = pattern.matcher(content);
        return (matcher.find()) ? matcher.group(1) : "";
    }

    private String getUri(String path) {
        return path.replaceAll("((http)|(https))://[^/]+", "");
    }

    List<Lemma> getSearchLemmas(String search) throws IOException {
        LemmaDetector lemmaDetector = new LemmaDetector(search);
        List<String> stringList = lemmaDetector.getMapLemmas().keySet().stream().toList();
        int countDistinctSite = this.allRepositories.getLemmaRepository().countDistinctSite();
        if (countDistinctSite == 0) {
            return new ArrayList<>();
        }
        HashMap<Lemma, Integer> lemmaHashMap = new HashMap<>();

        for (String lemmaString : stringList) {
            List<Lemma> listLemma = this.allRepositories.getLemmaRepository().findByLemma(lemmaString);
            if (listLemma.size() == 0) {
                continue;
            }
            if (listLemma.size() <= LIMIT_OCCURRENCE * (double) countDistinctSite) {
                lemmaHashMap.put(listLemma.get(0), listLemma.size());
            }
        }
        HashMap<Lemma, Integer> sortedLemmaMap = lemmaHashMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        return sortedLemmaMap.keySet().stream().toList();
    }

    private List<SiteDB> getLookingSites(String site) {
        List<SiteDB> listLookingSites = new ArrayList<>();
        SiteDB lookingSite = this.allRepositories.getSiteRepository().getSiteDBByUrl(site);
        if (lookingSite == null) {
            for (Site siteConfig : sites.getSites()) {
                listLookingSites.add(this.allRepositories.getSiteRepository().getSiteDBByUrl(siteConfig.getUrl()));
            }
        } else {
            listLookingSites.add(lookingSite);
        }
        return listLookingSites;
    }

    private List<SiteDB> allSitesIndexing(String site) {
        List<SiteDB> listLookingSites = this.getLookingSites(site);
        boolean isAllSitesIndexing = true;
        for (SiteDB lookingSite : listLookingSites) {
            if (!lookingSite.getStatus().equals(StatusType.INDEXED)) {
                isAllSitesIndexing = false;
                break;
            }
        }

        return isAllSitesIndexing ? listLookingSites : new ArrayList<>();
    }

    private static String deleteTags(String text) {
        String delScript = text.replaceAll("<script.*>[\\s\\S]*?</script>", "");
        String delStyle = delScript.replaceAll("<style.*>[\\s\\S]*?</style>", "");
        String delTags = delStyle.replaceAll("<[.[^>]]*>", "");
        String delMnemonicCode = delTags.replaceAll("&.*;", " ");
        String delNewline = delMnemonicCode.replaceAll("[\n]+", " ");
        return delNewline;
    }

    @Data
    private static class LemmaPageDB {
        PageDB pageDB;
        Lemma lemma;

        public LemmaPageDB(PageDB pageDB, Lemma lemma) {
            this.pageDB = pageDB;
            this.lemma = lemma;
        }
    }
}
