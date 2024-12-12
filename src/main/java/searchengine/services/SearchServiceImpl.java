package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.*;
import searchengine.exeptions.UnindexedSiteException;
import searchengine.model.*;
import searchengine.repositories.AllRepositories;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final double LIMIT_OCCURRENCE = 1.0;
    private static final int LENGTH_SNIPPET = 240;
    private static final String HIGHLIGHTING_TAG = "b";
    private final SitesList sites;
    private final AllRepositories allRepositories;

    @Override
    public BasicResponse search(String searchQuery, String site) {
        if ("".equals(searchQuery)) {
            return new EmptySearchQueryResponse();
        }

        List<Lemma> searchLemmas;
        List<SiteEntity> siteEntityList;
        try {
            siteEntityList = getLookingSites(site, sites, allRepositories);
            searchLemmas = getSearchLemmas(searchQuery, allRepositories);
            if (searchLemmas.isEmpty()) {
                return new EmptySearchDataListResponse();
            }
        } catch (IOException e) {
            return new ErrorLibraryResponse();
        } catch (NullPointerException | UnindexedSiteException e) {
            return new ErrorIndexingResponse();
        }
        List<LemmaPageEntity> lemmaPagePairs = getLemmaPagePairs(siteEntityList, searchLemmas, allRepositories);
        return makeResponse(lemmaPagePairs, searchLemmas, allRepositories);
    }

    List<Lemma> getSearchLemmas(String search, AllRepositories allRepositories) throws IOException {
        LemmaDetector lemmaDetector = new LemmaDetector(search);
        List<String> lemmaList = lemmaDetector.getMapLemmas().keySet().stream().toList();
        int countDistinctSite = allRepositories.getLemmaRepository().countDistinctSite();
        if (countDistinctSite == 0) {
            return new ArrayList<>();
        }
        HashMap<Lemma, Integer> lemmaHashMap = new HashMap<>();

        for (String lemma : lemmaList) {
            List<Lemma> listLemma = allRepositories.getLemmaRepository().findByLemma(lemma);
            if (listLemma.isEmpty()) {
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

    List<SiteEntity> getLookingSites(String site, SitesList sites,
                                     AllRepositories allRepositories) throws UnindexedSiteException {
        List<SiteEntity> listSites = new ArrayList<>();
        List<SiteEntity> listLookingSites = new ArrayList<>();
        if (!"".equals(site)) {
            SiteEntity lookingSite = allRepositories.getSiteRepository().getSiteEntityByUrl(site);
            if (lookingSite.getStatus().equals(StatusType.INDEXED)) {
                listSites.add(lookingSite);
            } else {
                throw new UnindexedSiteException();
            }
        } else {
            for (Site siteConfig : sites.getSites()) {
                SiteEntity siteEntity = allRepositories.getSiteRepository().getSiteEntityByUrl(siteConfig.getUrl());
                listSites.add(siteEntity);
            }
        }
        for (SiteEntity siteEntity : listSites) {
            if (siteEntity.getStatus().equals(StatusType.INDEXED)) {
                listLookingSites.add(siteEntity);
            }
        }
        if (listLookingSites.isEmpty()) {
            throw new UnindexedSiteException();
        } else {
            return listLookingSites;
        }
    }

    List<LemmaPageEntity> getLemmaPagePairs(List<SiteEntity> siteEntityList, List<Lemma> searchLemmas, AllRepositories allRepositories) {
        List<Index> siteIndexes;
        List<LemmaPageEntity> listLemmaPagePairs = new ArrayList<>();

        for (SiteEntity siteSearch : siteEntityList) {

            for (Lemma searchLemma : searchLemmas) {
                siteIndexes = allRepositories.getIndexRepository().findBySite(searchLemma.getLemma(), siteSearch.getId());
                listLemmaPagePairs.addAll(siteIndexes.stream()
                        .map(index -> new LemmaPageEntity(index.getPageEntity(), index.getLemma()))
                        .collect(Collectors.toCollection(ArrayList::new)));
            }
        }
        return listLemmaPagePairs;
    }

    SearchResponse makeResponse(List<LemmaPageEntity> lemmaPagePairs,
                                        List<Lemma> searchLemmas, AllRepositories allRepositories) {
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(lemmaPagePairs.size());

        float maxAbsoluteRelevance = 0.0F;
        List<SearchData> searchDataList = new ArrayList<>();

        for (LemmaPageEntity lemmaPageEntity : lemmaPagePairs) {
            SearchData searchResponse = new SearchData();
            PageEntity page = lemmaPageEntity.getPageEntity();
            if (!page.getContent().contains(lemmaPageEntity.getLemma().getLemma())) {
                continue;
            }
            searchResponse.setSite(page.getSite().getUrl());
            searchResponse.setSiteName(page.getSite().getName());
            searchResponse.setUri(this.getUri(page.getPath()));
            searchResponse.setTitle(this.getTitle(page.getContent()));
            searchResponse.setSnippet(this.getSnippet(page, lemmaPageEntity.getLemma()));
            float absoluteRelevancePage = this.getAbsoluteRelevance(page, searchLemmas, allRepositories);
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

    String getSnippet(PageEntity page, Lemma lemma) {
        String content = page.getContent();
        String lemmaString = lemma.getLemma();
        content = deleteTags(content);
        int indexBeginLemma = content.indexOf(lemmaString);
        String substringBeforeLemma = content.substring(0, indexBeginLemma);
        int indexBeginSnippet = substringBeforeLemma.lastIndexOf(".") + 2;
        int indexEndSnippet = content.indexOf(" ", indexBeginSnippet + LENGTH_SNIPPET);
        indexEndSnippet = (indexEndSnippet == -1) ? content.length() : indexEndSnippet;
        String snippetNotFormatted = content.substring(indexBeginSnippet, indexEndSnippet) + "...";
        return addSelectionLemma(snippetNotFormatted, lemmaString);
    }

    private String addSelectionLemma(String snippetNotFormatted, String lemma, String selectionTag) {
        int searchPosition = 0;
        int insertPosition;
        String selectBeginTag = "<" + selectionTag + ">";
        String selectEndTag = "</" + selectionTag + ">";
        int selectedLength = selectBeginTag.length() + lemma.length() + selectEndTag.length();
        StringBuilder snippetFormatted = new StringBuilder(snippetNotFormatted);
        while (searchPosition < snippetFormatted.length()) {
            insertPosition = snippetFormatted.indexOf(lemma, searchPosition);
            if (insertPosition < 0) {
                break;
            }
            if (insertPosition != 0 && snippetFormatted.charAt(insertPosition - 1) != ' ') {
                searchPosition = ++insertPosition;
                continue;
            }
            snippetFormatted.insert(insertPosition, selectBeginTag)
                    .insert(insertPosition + selectBeginTag.length() + lemma.length(), selectEndTag);
            searchPosition = insertPosition + selectedLength;
        }
        return snippetFormatted.toString();
    }

    private String addSelectionLemma(String snippetNotFormatted, String lemma) {
        return addSelectionLemma(snippetNotFormatted, lemma, HIGHLIGHTING_TAG);
    }

    String deleteTags(String text) {
        String delScript = text.replaceAll("<script.*?>[\\s\\S]*?</script>", "");
        String delStyle = delScript.replaceAll("<style.*?>[\\s\\S]*?</style>", "");
        String delTags = delStyle.replaceAll("<[.[^>]]*>", "");
        String delMnemonic = delTags.replaceAll("&.*?;", "");
        String delSpaceAndN = delMnemonic.replaceAll("((/n)+\\s*)+", " ");
        return delSpaceAndN.replaceAll("\\s{2,}", " ");
    }

    List<SearchData> sortSearchDataList(List<SearchData> searchDataList) {
        return searchDataList.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRelevance(), d1.getRelevance()))
                .toList();
    }

    float getAbsoluteRelevance(PageEntity page, List<Lemma> searchLemmas, AllRepositories allRepositories) {
        float sumRelevance = 0.0F;
        for (Lemma lemma : searchLemmas) {
            List<Index> indexList = allRepositories.getIndexRepository().findByLemmaAndPageEntity(lemma, page);
            sumRelevance += (indexList.size() == 0) ? 0 : indexList.get(0).getRank();
        }
        return sumRelevance;
    }

    String getTitle(String content) {
        Pattern pattern = Pattern.compile("<title>(.+)</title>");
        Matcher matcher = pattern.matcher(content);
        return (matcher.find()) ? matcher.group(1) : "";
    }

    String getUri(String path) {
        return path.replaceAll("((http)|(https))://[^/]+", "");
    }

    @Data
    static class LemmaPageEntity {
        PageEntity pageEntity;
        Lemma lemma;

        public LemmaPageEntity(PageEntity pageEntity, Lemma lemma) {
            this.pageEntity = pageEntity;
            this.lemma = lemma;
        }
    }
}
