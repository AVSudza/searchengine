package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class TestIndexPageServiceImpl {
    private static final String NAME_TEST_FOLDER = "src/test/testData/";
    private static final String NAME_TEST_FILE = "testPageMini";
    @Value("${compose.phpMyAdmin.port}")
    private String testPort;

    @Mock
    SitesList sitesList = Mockito.mock(SitesList.class);
    @Mock
    SiteRepository siteRepository = Mockito.mock(SiteRepository.class);
    @Autowired
    SiteRepository siteRepositoryDB;
    @Mock
    PageRepository pageRepository = Mockito.mock(PageRepository.class);
    @Autowired
    PageRepository pageRepositoryDB;
    @Mock
    LemmaRepository lemmaRepository = Mockito.mock(LemmaRepository.class);
    @Autowired
    LemmaRepository lemmaRepositoryDB;
    @Mock
    IndexRepository indexRepository = Mockito.mock(IndexRepository.class);
    @Autowired
    IndexRepository indexRepositoryDB;
    @Mock
    IndexPageService indexPageService = Mockito.mock(IndexPageService.class);
    @Autowired
    IndexPageServiceImpl indexPageServiceDB;
    @Mock
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    @Autowired
    JdbcTemplate jdbcTemplateDB;
    IndexPageServiceImpl service = new IndexPageServiceImpl(sitesList,
            siteRepository, pageRepository, lemmaRepository, indexRepository, jdbcTemplate);
    IndexPageServiceImpl serviceDB;

    @BeforeEach
    public void Init() {
        serviceDB = new IndexPageServiceImpl(sitesList,
                siteRepositoryDB, pageRepositoryDB, lemmaRepositoryDB, indexRepositoryDB, jdbcTemplateDB);
    }

    @Test // test getResponseByCode with Docker image from compose.yaml
    @DisplayName("get response by code 200")
    public void testGetResponseByCode200() throws StopIndexingException, IOException {
        SiteEntity siteEntity = getTestSite();
        PageServiceImpl testPageService = getTestPageService(siteEntity, 200);
        IndexingResponse expectedResponse = new IndexingResponse();
        expectedResponse.setError("");
        expectedResponse.setResult(true);
        serviceDB.setSiteEntity(siteEntity);
        siteRepositoryDB.save(siteEntity);
        pageRepositoryDB.save(testPageService.getPageEntity());
        serviceDB.setPageEntity(testPageService.getPageEntity());

        IndexingResponse actualResponse = serviceDB.indexingPage(testPageService);
        assertEquals(expectedResponse, actualResponse);

        Path pathTestFile = Paths.get(NAME_TEST_FOLDER + NAME_TEST_FILE + ".txt");
        int countLemma = Integer.parseInt(Files.readAllLines(pathTestFile, StandardCharsets.UTF_8).get(0));
        int countIndex = Integer.parseInt(Files.readAllLines(pathTestFile, StandardCharsets.UTF_8).get(1));

        assertEquals(lemmaRepositoryDB.count(), countLemma);
        assertEquals(indexRepositoryDB.count(), countIndex);
        indexRepositoryDB.deleteAll();
        lemmaRepositoryDB.deleteAll();
        pageRepositoryDB.deleteAll();
        siteRepositoryDB.deleteAll();
    }

    @Test // test getResponseByCode with Docker image from compose.yaml
    @DisplayName("get response by code 400")
    public void testGetResponseByCode400() throws StopIndexingException {
        SiteEntity siteEntity = getTestSite();
        PageServiceImpl testPageService = getTestPageService(siteEntity, 400);
        IndexingResponse expectedResponse = new IndexingResponse();
        expectedResponse.setError("Ошибка " + testPageService.getPageEntity().getCode() +
                " на странице: " + testPageService.getPageEntity().getPath());
        expectedResponse.setResult(false);
        IndexingResponse actualResponse = serviceDB.indexingPage(testPageService);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test // test getResponseByCode with Docker image from compose.yaml
    @DisplayName("get response by IOException")
    public void testGetResponseByIOException() throws StopIndexingException {
        SiteEntity siteEntity = getTestSite();
        PageServiceImpl testPageService = getTestPageService(siteEntity, 0);
        IndexingResponse expectedResponse = new IndexingResponse();
        expectedResponse.setError("Ошибка при чтении страницы: " + testPageService.getPageEntity().getPath() + "\n");
        expectedResponse.setResult(false);
        IndexingResponse actualResponse = serviceDB.indexingPage(testPageService);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test // test makePageServiceForUrl with Docker image from compose.yaml
    @DisplayName("make pageService for url")
    public void testMakePageServiceForUrl() throws StopIndexingException, IOException {
        SiteEntity testSite = getTestSite();
        SiteEntity siteEntity = new SiteEntity(StatusType.INDEXING,
                new Date(), "", testSite.getUrl(), testSite.getName());
        String testPagePath = "http://localhost:" + testPort + "/index.php";
        PageEntity pageEntity = new PageEntity(siteEntity, testPagePath, 200, "");
        PageServiceImpl expectedPageService = new PageServiceImpl(pageEntity, siteRepositoryDB, pageRepositoryDB);
        PageServiceImpl actualPageService = serviceDB.makePageServiceForUrl(testSite, testPagePath);
        deletePageFromDB(actualPageService.getPageEntity());
        deleteSiteFromDB(actualPageService.getPageEntity().getSite());
        assertEquals(expectedPageService, actualPageService);
    }

    @Test // test saveLemmaToDB with Docker image from compose.yaml
    @DisplayName("test save Lemma to DB")
    public void testSaveLemmaToDB() {
        SiteEntity siteEntity = getTestSite();
        Lemma testLemma = getTestLemma(siteEntity);
        assertEquals(List.of(), lemmaRepositoryDB.findByLemma(testLemma.getLemma()));
        siteRepositoryDB.save(siteEntity);
        lemmaRepositoryDB.save(testLemma);
        List<Lemma> expectedLemma = new ArrayList<>();
        expectedLemma.add(testLemma);
        List<Lemma> actualLemma = lemmaRepositoryDB.findByLemma(testLemma.getLemma());
        assertEquals(expectedLemma, actualLemma);
        deleteLemmaFromDB(testLemma);
        deleteSiteFromDB(siteEntity);
    }

    @Test // test saveIndexToDB with Docker image from compose.yaml
    @DisplayName("test save Index to DB")
    public void testSaveIndexToDB() throws StopIndexingException {
        SiteEntity testSiteEntity = getTestSite();
        Lemma testLemma = getTestLemma(testSiteEntity);
        PageEntity testPageEntity = getTestPageService(testSiteEntity, 200).getPageEntity();
        Index testIndex = getTestIndex(testLemma, testPageEntity);
        assertEquals(List.of(), indexRepositoryDB.findAll());
        siteRepositoryDB.save(testSiteEntity);
        pageRepositoryDB.save(testPageEntity);
        lemmaRepositoryDB.save(testLemma);
        indexRepositoryDB.save(testIndex);
        List<Index> expectedIndexList = new ArrayList<>();
        expectedIndexList.add(testIndex);
        List<Index> actualIndexList = indexRepositoryDB.findByLemmaAndPageEntity(testLemma, testPageEntity);
        assertEquals(expectedIndexList, actualIndexList);
        deleteTestRows(testSiteEntity, testPageEntity, testLemma, testIndex);
    }

    @Test // test findLemmaSite with Docker image from compose.yaml
    @DisplayName("test find LemmaSite")
    public void testFindLemmaSite() {
        SiteEntity siteEntity = getTestSite();
        Lemma testLemma = getTestLemma(siteEntity);
        assertEquals(List.of(), lemmaRepositoryDB.findByLemma(testLemma.getLemma()));
        siteRepositoryDB.save(siteEntity);
        lemmaRepositoryDB.save(testLemma);
        assertEquals(List.of(testLemma), lemmaRepositoryDB.findByLemma(testLemma.getLemma()));
        List<Lemma> expectedListLemma = List.of(testLemma);
        List<Lemma> actualListLemma = serviceDB.findLemmaSite(testLemma.getLemma(), siteEntity.getId());
        assertEquals(expectedListLemma, actualListLemma);
        deleteLemmaFromDB(testLemma);
        deleteSiteFromDB(siteEntity);
    }

    @Test
    @DisplayName("check page on sites when it exists ")
    public void testCheckPageOnSitesWhenItExists() throws StopIndexingException, IOException {
        String pageUrl = "https://www.skillbox.ru/qwerty";
        List<Site> listSites = new ArrayList<>();
        Site site1 = new Site();
        site1.setUrl("https://www.skillbox.ru");
        site1.setName("Skillbox");
        listSites.add(site1);
        when(sitesList.getSites()).thenReturn(listSites);
        assertEquals(service.checkPageOnSites(pageUrl, sitesList), site1);
    }

    @Test
    @DisplayName("check page on sites when it not exists ")
    public void testCheckPageOnSitesWhenItNotExists() throws StopIndexingException, IOException {
        String url = "https://volochek.life/qwerty";
        List<Site> listSites = new ArrayList<>();
        Site site1 = new Site();
        site1.setUrl("https://www.skillbox.ru");
        site1.setName("Skillbox");
        Site site2 = new Site();
        site2.setUrl("https://www.playback.ru");
        site2.setName("PlayBack.Ru");
        listSites.add(site2);
        when(sitesList.getSites()).thenReturn(listSites);
        searchengine.config.Site actualSite = service.checkPageOnSites(url, sitesList);
        assertNull(actualSite);
    }


    @Test //test makeSiteEntity with Docker image from compose.yaml
    @DisplayName("make siteEntity")
    public void testMakeSiteEntity() {
        String url = "https://www.skillbox.ru";
        String name = "Skillbox";
        Date statusDate = new Date();
        SiteEntity expectedSite = new SiteEntity(StatusType.INDEXING,
                statusDate, "", url, name);
        SiteEntity actualSite = serviceDB.makeSiteEntity(url, name);
        actualSite.setStatusTime(statusDate);
        siteRepositoryDB.delete(actualSite);
        assertEquals(expectedSite, actualSite);
    }

    @Test //test makePageEntity with Docker image from compose.yaml
    @DisplayName("make pageEntity")
    public void testMakePageEntity() throws StopIndexingException, IOException {
        String siteUrl = "http://localhost:" + testPort;
        String pageUrl = "http://localhost:" + testPort + "/index.php";
        String siteName = "PHPMyAdmin";
        Date statusDate = new Date();
        SiteEntity sitePage = new SiteEntity(StatusType.INDEXING,
                statusDate, "", siteUrl, siteName);
        siteRepositoryDB.save(sitePage);
        PageEntity expectedPage = new PageEntity(sitePage, pageUrl, 200, "");

        PageEntity actualPage = serviceDB.makePageEntity(pageUrl, sitePage);
        actualPage.getSite().setStatusTime(statusDate);
        assertEquals(expectedPage, actualPage);
        deletePageFromDB(actualPage);
        deleteSiteFromDB(actualPage.getSite());
    }

    @Test
    @DisplayName("get siteEntity by url")
    public void testGetSiteEntityByUrl() {
        String url = "https://www.skillbox.ru";
        SiteEntity expectedSiteEntity = new SiteEntity(StatusType.INDEXING,
                new Date(), "", "https://www.skillbox.ru", "Skillbox");
        String query = "SELECT * FROM site WHERE url = 'https://www.skillbox.ru'";
        when(jdbcTemplate.queryForObject(query, SiteEntityRowMapper.getInstance())).thenReturn(expectedSiteEntity);
        SiteEntity actualSiteEntity = service.getSiteEntityByUrl(url, jdbcTemplate);
        assertEquals(expectedSiteEntity, actualSiteEntity);
    }

    @Test
    @DisplayName("get pageEntity by url")
    public void testGetPageEntityByUrl() {
        String url = "https://www.skillbox.ru/qwerty";
        SiteEntity siteEntity = new SiteEntity(StatusType.INDEXING,
                new Date(), "", "https://www.skillbox.ru", "Skillbox");
        PageEntity expectedPageEntity = new PageEntity(siteEntity, "", 200, "");
        String query = "SELECT * FROM page WHERE path = 'https://www.skillbox.ru/qwerty'";
        when(jdbcTemplate.queryForObject(query, PageEntityRowMapper.getInstance())).thenReturn(expectedPageEntity);
        PageEntity actualPageEntity = service.getPageEntityByUrl(url, jdbcTemplate);
        assertEquals(expectedPageEntity, actualPageEntity);
    }

    @Test
    @DisplayName("get pageEntity by missing url")
    public void testGetPageEntityByMissingUrl() {
        String url = "https://www.skillbox.ru/qwerty";
        PageEntity expectedPageEntity = new PageEntity();
        List<PageEntity> pageEntityList = List.of(expectedPageEntity);
        String query = "SELECT * FROM page WHERE path = 'https://www.skillbox.ru/qwerty'";
        when(jdbcTemplate.queryForObject(query, PageEntityRowMapper.getInstance())).thenReturn(expectedPageEntity);
        PageEntity actualPageEntity = service.getPageEntityByUrl(url, jdbcTemplate);
        assertEquals(expectedPageEntity, actualPageEntity);
    }

    @Test //test deletePageLemmaIndexInfo with Docker image from compose.yaml
    @DisplayName("test DeletePageLemmaIndexInfo")
    @Rollback(value = false)
    public void deletePageLemmaIndexInfo() throws StopIndexingException {
        SiteEntity testSite = getTestSite();
        SiteEntity testSiteEntity = new SiteEntity(StatusType.INDEXING,
                new Date(), "", testSite.getUrl(), testSite.getName());
        PageServiceImpl testPageService = getTestPageService(testSiteEntity, 200);

        assertFalse(testPageService.existsPageInDB());
        siteRepositoryDB.save(testSiteEntity);
        testPageService.savePageToDB();
        assertTrue(testPageService.existsPageInDB());

        assertEquals(List.of(), lemmaRepositoryDB.findBySiteId(testPageService.getPageEntity().getSite().getId()));
        Lemma testLemma = getTestLemma(testSiteEntity);

        Index testIndex = new Index();
        testIndex.setLemma(testLemma);
        testIndex.setPageEntity(testPageService.getPageEntity());
        testIndex.setRank(1.5f);

        Set<Index> testIndexSet = new HashSet<>();
        testIndexSet.add(testIndex);

        testLemma.setIndexSet(testIndexSet);
        lemmaRepositoryDB.save(testLemma);
        indexRepositoryDB.save(testIndex);
        List<Lemma> expectedListLemma = List.of(testLemma);
        List<Lemma> actualListLemma = lemmaRepositoryDB.findByLemma(testLemma.getLemma());

        assertEquals(expectedListLemma, actualListLemma);

        PageServiceImpl newTestPageService = serviceDB.deletePageLemmaIndexInfo(testPageService);
        assertEquals(newTestPageService, testPageService);

        deleteTestRows(testSiteEntity, testPageService.getPageEntity(), testLemma, testIndex);

        assertFalse(siteRepositoryDB.existsSiteEntityByUrl(testSiteEntity.getUrl()));
        assertFalse(testPageService.existsPageInDB());
        assertEquals(List.of(), lemmaRepositoryDB.findBySiteId(testPageService.getPageEntity().getSite().getId()));
        assertEquals(List.of(), indexRepositoryDB.findByLemma(testLemma));
    }

    private void deleteTestRows(SiteEntity testSite, PageEntity testPage, Lemma testLemma, Index testIndex) {
        deleteIndexFromDB(testIndex);
        deleteLemmaFromDB(testLemma);
        deletePageFromDB(testPage);
        deleteSiteFromDB(testSite);
    }

    private SiteEntity getTestSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("http://localhost:" + testPort);
        site.setName("PHPMyAdmin");
        site.setStatusTime(new Date());
        site.setStatus(StatusType.INDEXING);

        return site;
    }

    private SiteEntity getTestSiteEntity(Site site) {
        return new SiteEntity(StatusType.INDEXING,
                new Date(), "", site.getUrl(), site.getName());
    }

    private @NotNull PageServiceImpl getTestPageService(SiteEntity testSiteEntity, int code) throws StopIndexingException {
        String testPagePath = "http://localhost:" + testPort + "/index.php";
        PageEntity testPage = new PageEntity(testSiteEntity, testPagePath,
                code, getTestContent(NAME_TEST_FOLDER + NAME_TEST_FILE + ".html"));
        return new PageServiceImpl(testPage, siteRepositoryDB, pageRepositoryDB);
    }

    private Lemma getTestLemma(SiteEntity testSite) {
        Lemma testLemma = new Lemma();
        testLemma.setLemma("testLemma");
        testLemma.setSite(testSite);
        testLemma.setFrequency(55);
        testLemma.setIndexSet(new HashSet<>());
        return testLemma;
    }

    private Index getTestIndex(Lemma lemma, PageEntity pageEntity) {
        Index index = new Index();
        index.setLemma(lemma);
        index.setPageEntity(pageEntity);
        index.setRank(1.5f);
        return index;
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

    private String getTestContent(String pathTestPage) {
        String content = "";
        try {
            Path path = Paths.get(pathTestPage);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            content = String.join("/n", lines);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
