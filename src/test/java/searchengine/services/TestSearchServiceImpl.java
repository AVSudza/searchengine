package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exeptions.UnindexedSiteException;
import searchengine.model.*;
import searchengine.repositories.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Slf4j
public class TestSearchServiceImpl {
    private static final String NAME_TEST_FOLDER = "./src/test/testData/";
    @Mock
    private SitesList sites = Mockito.mock(SitesList.class);
    @Mock
    private SiteRepository siteRepository = Mockito.mock(SiteRepository.class);
    @Autowired
    SiteRepository siteRepositoryDB;
    @Mock
    private LemmaRepository lemmaRepository = Mockito.mock(LemmaRepository.class);
    @Autowired
    LemmaRepository lemmaRepositoryDB;
    @Mock
    private IndexRepository indexRepository = Mockito.mock(IndexRepository.class);
    @Autowired
    IndexRepository indexRepositoryDB;
    @Mock
    PageRepository pageRepository = Mockito.mock(PageRepository.class);
    @Autowired
    PageRepository pageRepositoryDB;
    @Mock
    IndexPageService indexPageService = Mockito.mock(IndexPageService.class);
    @Autowired
    IndexPageService indexPageServiceDB;
    @Mock
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    @Autowired
    JdbcTemplate jdbcTemplateDB;
    @Mock
    AllRepositories allRepositories = Mockito.mock(AllRepositories.class);
    @Autowired
    AllRepositories allRepositoriesDB = new AllRepositories(siteRepositoryDB, pageRepositoryDB, lemmaRepositoryDB,
            indexRepositoryDB, jdbcTemplateDB);
    SearchServiceImpl searchService = new SearchServiceImpl(sites, allRepositories);
    SearchServiceImpl searchServiceDB = new SearchServiceImpl(sites, allRepositoriesDB);
    private String content = "";

    @BeforeEach
    public void init() {
        content = getContentFile(NAME_TEST_FOLDER + "testPage.html");
    }

    @Test
    @DisplayName("test get search lemmas")
    public void testGetSearchLemmas() {
        String search = "мама мыла";

        List<Lemma> listLemma1 = new ArrayList<>();
        Lemma lemma11 = new Lemma();
        lemma11.setLemma("мама");
        Lemma lemma12 = new Lemma();
        lemma12.setLemma("мама");
        listLemma1.add(lemma11);
        listLemma1.add(lemma12);
        List<Lemma> listLemma2 = new ArrayList<>();
        Lemma lemma2 = new Lemma();
        lemma2.setLemma("мыть");
        listLemma2.add(lemma2);
        List<Lemma> listLemma3 = new ArrayList<>();
        Lemma lemma31 = new Lemma();
        lemma31.setLemma("мыло");
        Lemma lemma32 = new Lemma();
        lemma32.setLemma("мыло");
        Lemma lemma33 = new Lemma();
        lemma33.setLemma("мыло");
        listLemma3.add(lemma31);
        listLemma3.add(lemma32);
        listLemma3.add(lemma33);

        when(allRepositories.getLemmaRepository()).thenReturn(lemmaRepository);
        when(allRepositories.getLemmaRepository().countDistinctSite()).thenReturn(6);

        when(allRepositories.getLemmaRepository().findByLemma(lemma11.getLemma())).thenReturn(listLemma1);
        when(allRepositories.getLemmaRepository().findByLemma(lemma2.getLemma())).thenReturn(listLemma2);
        when(allRepositories.getLemmaRepository().findByLemma(lemma31.getLemma())).thenReturn(listLemma3);
        List<Lemma> actual = List.of();
        try {
            actual = searchService.getSearchLemmas(search, allRepositories);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Lemma> expected = List.of(lemma2, lemma11, lemma31);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("test get lookingSites for selected site")
    public void testGetLookingSitesForSelectedSite() {
        String selectedSite = "https://www.skillbox.ru";
        SiteEntity selectedSiteEntity = new SiteEntity(
                StatusType.INDEXED, new Date(), "",
                selectedSite, "Skillbox");
        String returnedSite = "https://www.skillbox.ru";
        SiteEntity returnedSiteEntity = new SiteEntity(
                StatusType.INDEXED, new Date(), "",
                returnedSite, "Skillbox");
        SitesList sites = new SitesList();

        when(allRepositories.getSiteRepository()).thenReturn(siteRepository);
        when(allRepositories.getSiteRepository().getSiteEntityByUrl(selectedSite)).thenReturn(returnedSiteEntity);

        List<SiteEntity> expected = List.of(selectedSiteEntity);
        List<SiteEntity> actual;
        try {
            actual = searchService.getLookingSites(selectedSite, sites, allRepositories);
        } catch (UnindexedSiteException e) {
            actual = List.of();
        }
        assertEquals(expected, actual);
        verify(allRepositories.getSiteRepository(), times(1)).getSiteEntityByUrl(selectedSite);
    }

    @Test
    @DisplayName("test get lookingSites for all sites")
    public void testGetLookingSitesForAllSites() {
        List<Site> sitesList = new ArrayList<>();
        Site site1 = new Site();
        site1.setUrl("https://www.skillbox.ru");
        site1.setName("Skillbox");
        SiteEntity siteEntity1 = new SiteEntity();
        siteEntity1.setStatus(StatusType.INDEXED);
        Site site2 = new Site();
        site2.setUrl("https://www.lenta.ru");
        site2.setName("Лента.ру");
        SiteEntity siteEntity2 = new SiteEntity();
        siteEntity2.setStatus(StatusType.INDEXED);
        Site site3 = new Site();
        site3.setUrl("https://www.playback.ru");
        site3.setName("PlayBack.Ru");
        SiteEntity siteEntity3 = new SiteEntity();
        siteEntity3.setStatus(StatusType.FAILED);

        sitesList.add(site1);
        sitesList.add(site2);
        sitesList.add(site3);

        SitesList sites = new SitesList();
        sites.setSites(sitesList);

        when(allRepositories.getSiteRepository()).thenReturn(siteRepository);
        when(allRepositories.getSiteRepository().getSiteEntityByUrl("https://www.skillbox.ru")).thenReturn(siteEntity1);
        when(allRepositories.getSiteRepository().getSiteEntityByUrl("https://www.lenta.ru")).thenReturn(siteEntity2);
        when(allRepositories.getSiteRepository().getSiteEntityByUrl("https://www.playback.ru")).thenReturn(siteEntity3);
        List<SiteEntity> expected = List.of(siteEntity1, siteEntity2);
        List<SiteEntity> actual;
        try {
            actual = searchService.getLookingSites("", sites, allRepositories);
        } catch (UnindexedSiteException e) {
            actual = List.of();
        }
        assertEquals(expected, actual);
        verify(allRepositories.getSiteRepository(), times(1))
                .getSiteEntityByUrl("https://www.skillbox.ru");
        verify(allRepositories.getSiteRepository(), times(1))
                .getSiteEntityByUrl("https://www.lenta.ru");
        verify(allRepositories.getSiteRepository(), times(1))
                .getSiteEntityByUrl("https://www.playback.ru");
    }

    @Test
    @DisplayName("test getLemmaPagePairs")
    public void testGetLemmaPagePairs() {
        Date statusTime = new Date();
        List<SiteEntity> searchSiteEntityList = new ArrayList<>();

        SiteEntity siteEntity1 = new SiteEntity(StatusType.INDEXED, statusTime,
                "noError1", "url1", "name1");
        SiteEntity siteEntity2 = new SiteEntity(StatusType.INDEXED, statusTime,
                "noError2", "url2", "name2");
        SiteEntity siteEntity3 = new SiteEntity(StatusType.INDEXED, statusTime,
                "noError3", "url3", "name3");
        List<SiteEntity> siteEntityList = new ArrayList<>(Stream.of(siteEntity1, siteEntity2, siteEntity3).toList());
        siteRepositoryDB.saveAll(siteEntityList);

        PageEntity pageEntity1 = new PageEntity(siteEntity1, "path1", 200, "content1 lemma2 lemma2 lemma2");
        PageEntity pageEntity2 = new PageEntity(siteEntity2, "path2", 200, "content2 lemma1 lemma2 lemma2");
        PageEntity pageEntity3 = new PageEntity(siteEntity2, "path3", 200, "content3 lemma1 lemma1 lemma2");
        List<PageEntity> pageEntityList = new ArrayList<>(Stream.of(pageEntity1, pageEntity2, pageEntity3).toList());
        pageRepositoryDB.saveAll(pageEntityList);

        Lemma lemma1 = new Lemma(new HashSet<>(), siteEntity1, "lemma1", 1);
        Lemma lemma2 = new Lemma(new HashSet<>(), siteEntity1, "lemma2", 1);
        Lemma lemma3 = new Lemma(new HashSet<>(), siteEntity2, "lemma1", 2);
        Lemma lemma4 = new Lemma(new HashSet<>(), siteEntity2, "lemma2", 2);
        List<Lemma> lemmaList = new ArrayList<>(Stream.of(lemma1, lemma2, lemma3, lemma4).toList());
        lemmaRepositoryDB.saveAll(lemmaList);

        Index index1 = new Index(pageEntity1, lemma1, 1f);
        Index index2 = new Index(pageEntity1, lemma2, 3f);
        Index index3 = new Index(pageEntity2, lemma1, 1f);
        Index index4 = new Index(pageEntity2, lemma2, 3f);
        Index index5 = new Index(pageEntity3, lemma1, 2f);
        Index index6 = new Index(pageEntity3, lemma2, 1f);
        List<Index> indexList = new ArrayList<>(Stream.of(index1, index2, index3, index4, index5, index6).toList());
        indexRepositoryDB.saveAll(indexList);

        SearchServiceImpl.LemmaPageEntity lemmaPageEntity1 = new SearchServiceImpl.LemmaPageEntity(pageEntity2, lemma1);
        SearchServiceImpl.LemmaPageEntity lemmaPageEntity2 = new SearchServiceImpl.LemmaPageEntity(pageEntity2, lemma2);
        SearchServiceImpl.LemmaPageEntity lemmaPageEntity3 = new SearchServiceImpl.LemmaPageEntity(pageEntity3, lemma1);
        SearchServiceImpl.LemmaPageEntity lemmaPageEntity4 = new SearchServiceImpl.LemmaPageEntity(pageEntity3, lemma2);
        List<SearchServiceImpl.LemmaPageEntity> expectedLemmaPagePairs = new ArrayList<>(Stream.of(
                lemmaPageEntity1, lemmaPageEntity2, lemmaPageEntity3, lemmaPageEntity4).toList());

        searchSiteEntityList.add(siteEntity2);
        List<Lemma> searchLemmas = new ArrayList<>(Stream.of(lemma1, lemma2).toList());
        List<SearchServiceImpl.LemmaPageEntity> actualLemmaPagePairs =
                searchServiceDB.getLemmaPagePairs(searchSiteEntityList, searchLemmas, allRepositoriesDB);
        assertEquals(expectedLemmaPagePairs.size(), actualLemmaPagePairs.size());
        assertEquals(expectedLemmaPagePairs.stream().mapToInt(lpr -> lpr.pageEntity.getId()).sum(),
                actualLemmaPagePairs.stream().mapToInt(lpr -> lpr.pageEntity.getId()).sum());
        assertEquals(expectedLemmaPagePairs.stream().mapToInt(lpr -> lpr.lemma.getId()).sum(),
                actualLemmaPagePairs.stream().mapToInt(lpr -> lpr.lemma.getId()).sum());

        deleteTestRows(siteEntityList, pageEntityList, lemmaList, indexList);
    }

    @Test
    @DisplayName("test makeResponse")
    public void testMakeResponse() {
        Date statusTime = new Date();
        List<SiteEntity> searchSiteEntityList = new ArrayList<>();

        SiteEntity siteEntity1 = new SiteEntity(StatusType.INDEXED, statusTime,
                "noError1", "url1", "name1");
        SiteEntity siteEntity2 = new SiteEntity(StatusType.INDEXED, statusTime,
                "noError2", "url2", "name2");
        SiteEntity siteEntity3 = new SiteEntity(StatusType.INDEXED, statusTime,
                "noError3", "url3", "name3");
        List<SiteEntity> siteEntityList = new ArrayList<>(Stream.of(siteEntity1, siteEntity2, siteEntity3).toList());
        siteRepositoryDB.saveAll(siteEntityList);

        PageEntity pageEntity1 = new PageEntity(siteEntity1, "path1", 200,
                "content1 lemma2 lemma2 lemma2 content1");
        PageEntity pageEntity2 = new PageEntity(siteEntity2, "path2", 200,
                "content2. content2 lemma1 lemma2 lemma2");
        PageEntity pageEntity3 = new PageEntity(siteEntity2, "path3", 200,
                "content3. content3 content3 lemma1 lemma1 lemma1 lemma2 content3 content3 content3 content3 content3 content3");
        PageEntity pageEntity4 = new PageEntity(siteEntity2, "path4", 200,
                "content4. content4 lemma1 lemma1 lemma1 content4 content4");
        List<PageEntity> pageEntityList = new ArrayList<>(Stream.of(pageEntity1, pageEntity2, pageEntity3, pageEntity4).toList());
        pageRepositoryDB.saveAll(pageEntityList);

        Lemma lemma1 = new Lemma(new HashSet<>(), siteEntity1, "lemma1", 0);
        Lemma lemma2 = new Lemma(new HashSet<>(), siteEntity1, "lemma2", 1);
        Lemma lemma3 = new Lemma(new HashSet<>(), siteEntity2, "lemma1", 2);
        Lemma lemma4 = new Lemma(new HashSet<>(), siteEntity2, "lemma2", 2);
        List<Lemma> lemmaList = new ArrayList<>(Stream.of(lemma1, lemma2, lemma3, lemma4).toList());
        lemmaRepositoryDB.saveAll(lemmaList);

        Index index1 = new Index(pageEntity1, lemma1, 0f);
        Index index2 = new Index(pageEntity1, lemma2, 3f);
        Index index3 = new Index(pageEntity2, lemma1, 1f);
        Index index4 = new Index(pageEntity2, lemma2, 2f);
        Index index5 = new Index(pageEntity3, lemma1, 3f);
        Index index6 = new Index(pageEntity3, lemma2, 1f);
        Index index7 = new Index(pageEntity4, lemma1, 3f);
        List<Index> indexList = new ArrayList<>(Stream.of(index1, index2, index3, index4, index5, index6, index7).toList());
        indexRepositoryDB.saveAll(indexList);

        searchSiteEntityList.add(siteEntity2);
        List<Lemma> searchLemmas = new ArrayList<>(Stream.of(lemma1, lemma2).toList());
        List<SearchServiceImpl.LemmaPageEntity> lemmaPagePairs =
                searchServiceDB.getLemmaPagePairs(searchSiteEntityList, searchLemmas, allRepositoriesDB);

        List<SearchData> data = new ArrayList<>();
        SearchData data1 = new SearchData();
        data1.setSite(siteEntity2.getUrl());
        data1.setSiteName(siteEntity2.getName());
        data1.setUri(pageEntity3.getPath());
        data1.setTitle("");
        data1.setSnippet("content3 content3 <b>lemma1</b> <b>lemma1</b> <b>lemma1</b> lemma2 content3 content3 content3 content3 content3 content3...");
        data1.setRelevance(1.0);
        SearchData data2 = new SearchData();
        data2.setSite(siteEntity2.getUrl());
        data2.setSiteName(siteEntity2.getName());
        data2.setUri(pageEntity3.getPath());
        data2.setTitle("");
        data2.setSnippet("content3 content3 lemma1 lemma1 lemma1 <b>lemma2</b> content3 content3 content3 content3 content3 content3...");
        data2.setRelevance(1.0);
        SearchData data3 = new SearchData();
        data3.setSite(siteEntity2.getUrl());
        data3.setSiteName(siteEntity2.getName());
        data3.setUri(pageEntity2.getPath());
        data3.setTitle("");
        data3.setSnippet("content2 <b>lemma1</b> lemma2 lemma2...");
        data3.setRelevance(0.75);
        SearchData data4 = new SearchData();
        data4.setSite(siteEntity2.getUrl());
        data4.setSiteName(siteEntity2.getName());
        data4.setUri(pageEntity4.getPath());
        data4.setTitle("");
        data4.setSnippet("content4 <b>lemma1</b> <b>lemma1</b> <b>lemma1</b> content4 content4...");
        data4.setRelevance(0.75);
        SearchData data5 = new SearchData();
        data5.setSite(siteEntity2.getUrl());
        data5.setSiteName(siteEntity2.getName());
        data5.setUri(pageEntity2.getPath());
        data5.setTitle("");
        data5.setSnippet("content2 lemma1 <b>lemma2</b> <b>lemma2</b>...");
        data5.setRelevance(0.75);

        data.add(data1);
        data.add(data2);
        data.add(data3);
        data.add(data4);
        data.add(data5);
        SearchResponse expectedResponse = new SearchResponse(true, data.size(), data);
        SearchResponse actualResponse = searchServiceDB.makeResponse(lemmaPagePairs, searchLemmas, allRepositoriesDB);

        deleteTestRows(siteEntityList, pageEntityList, lemmaList, indexList);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("test get snippet from page by lemma")
    public void testGetSnippet() {
        String searchLemma = "Writer";
        Lemma lemma = new Lemma();
        lemma.setLemma(searchLemma);
        PageEntity page = new PageEntity();
        page.setContent(getContentFile(NAME_TEST_FOLDER + "testPage.html"));
        String expected = "И для этой цели служат совсем другие классы, которые являются наследниками " +
                "абстрактных классов Reader и <b>Writer</b>. Запись файлов. Класс FileWriter Класс " +
                "FileWriter является производным от класса <b>Writer</b>. Он используется для записи " +
                "текстовых файлов....";
        assertEquals(expected, searchService.getSnippet(page, lemma));
    }

    @Test
    @DisplayName("test removing tags from page content")
    public void testDeleteTags() {
        String expected = getContentFile(NAME_TEST_FOLDER + "testDeleteTags/resultPage.html");
        assertEquals(expected, searchService.deleteTags(getContentFile(
                NAME_TEST_FOLDER + "testDeleteTags/testPage.html")));
    }

    @Test
    @DisplayName("test sort searchDataList")
    public void testSortSearchDataList() {
        List<SearchData> searchDataList = new ArrayList<>();
        SearchData searchData1 = new SearchData();
        searchData1.setRelevance(1.0);
        searchDataList.add(searchData1);
        SearchData searchData2 = new SearchData();
        searchData2.setRelevance(1.5);
        searchDataList.add(searchData2);
        SearchData searchData3 = new SearchData();
        searchData3.setRelevance(1.9);
        searchDataList.add(searchData3);
        List<SearchData> expectedSearchDataList = new ArrayList<>();
        expectedSearchDataList.add(searchData3);
        expectedSearchDataList.add(searchData2);
        expectedSearchDataList.add(searchData1);
        assertEquals(expectedSearchDataList, searchService.sortSearchDataList(searchDataList));
    }

    @Test
    @DisplayName("test get absolute relevance")
    public void testGetAbsoluteRelevance() {
        PageEntity page = new PageEntity();
        Lemma lemma1 = new Lemma(new HashSet<>(), new SiteEntity(), "lemma1", 0);
        Lemma lemma2 = new Lemma(new HashSet<>(), new SiteEntity(), "lemma2", 0);
        Lemma lemma3 = new Lemma(new HashSet<>(), new SiteEntity(), "lemma3", 0);
        List<Lemma> searchLemmas = List.of(lemma1, lemma2, lemma3);

        Index index1 = new Index();
        index1.setRank(1.2f);
        List<Index> indexList1 = List.of(index1);
        Index index2 = new Index();
        index2.setRank(0.7f);
        List<Index> indexList2 = List.of(index2);
        Index index3 = new Index();
        index3.setRank(1.5f);
        List<Index> indexList3 = List.of(index3);

        when(allRepositories.getIndexRepository()).thenReturn(indexRepository);
        when(allRepositories.getIndexRepository().findByLemmaAndPageEntity(lemma1, page)).thenReturn(indexList1);
        when(allRepositories.getIndexRepository().findByLemmaAndPageEntity(lemma2, page)).thenReturn(indexList2);
        when(allRepositories.getIndexRepository().findByLemmaAndPageEntity(lemma3, page)).thenReturn(indexList3);
        Float expected = 3.4f;
        Float actual = searchService.getAbsoluteRelevance(page, searchLemmas, allRepositories);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("test get title of pages content")
    public void testGetTitle() {
        String expected = "Java | Чтение и запись текстовых файлов";
        assertEquals(expected, searchService.getTitle(getContentFile(NAME_TEST_FOLDER + "testPage.html")));
    }

    @Test
    @DisplayName("test get uri")
    public void testGetUri() {
        String expected = "/style44.css?v=2";
        String path = "https://metanit.com/style44.css?v=2";
        assertEquals(expected, searchService.getUri(path));
    }

    private String getContentFile(String fileName) {
        String content = "";
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
            content = String.join("/n", lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private void deleteTestRows(List<SiteEntity> testSites, List<PageEntity> testPages,
                                List<Lemma> testLemmas, List<Index> testIndexes) {
        for (Index testIndex : testIndexes) {
            deleteIndexFromDB(testIndex);
        }
        for (Lemma testLemma : testLemmas) {
            deleteLemmaFromDB(testLemma);
        }
        for (PageEntity testPage : testPages) {
            deletePageFromDB(testPage);
        }
        for (SiteEntity testSite : testSites) {
            deleteSiteFromDB(testSite);
        }
    }

    private void deleteSiteFromDB(SiteEntity siteEntity) {
        Set<Integer> setIdSite = new HashSet<>();
        setIdSite.add(siteEntity.getId());
        siteRepositoryDB.delete(setIdSite);
    }

    private void deletePageFromDB(PageEntity pageEntity) {
        Set<Integer> setIdPage = new HashSet<>();
        setIdPage.add(pageEntity.getId());
        pageRepositoryDB.delete(setIdPage);
    }

    private void deleteLemmaFromDB(Lemma lemma) {
        Set<Integer> setIdLemma = new HashSet<>();
        setIdLemma.add(lemma.getId());
        lemmaRepositoryDB.delete(setIdLemma);
    }

    private void deleteIndexFromDB(Index index) {
        Set<Integer> setIdIndex = new HashSet<>();
        setIdIndex.add(index.getId());
        indexRepositoryDB.delete(setIdIndex);
    }

}
