package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import searchengine.exeptions.StopIndexingException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestPageServiceImpl {
    @Mock
    private final PageEntity pageEntity = Mockito.mock(PageEntity.class);
    private final PageServiceImpl pageService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexPageServiceImpl indexPageService;
    HashMap<String, Boolean> testHashMap = new HashMap<>();
    @Mock
    private final Document contentPage;

    public TestPageServiceImpl() throws StopIndexingException {
        siteRepository = Mockito.mock(SiteRepository.class);
        pageRepository = Mockito.mock(PageRepository.class);
        indexPageService = Mockito.mock(IndexPageServiceImpl.class);
        pageService = new PageServiceImpl(pageEntity, siteRepository, pageRepository);
        contentPage = Mockito.mock(Document.class);
    }

    @BeforeEach
    void init() {
    }

    @Test
    @DisplayName("get domain from URL with http")
    public void testGetDomainWithHttp() {
        String testUrl = "http://www.skillbox.ru";
        String expected = "skillbox.ru";
        String actual = pageService.getDomain(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("get domain from URL with https")
    public void testGetDomainWithHttps() {
        String testUrl = "https://www.skillbox.ru";
        String expected = "skillbox.ru";
        String actual = pageService.getDomain(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("check valid link")
    public void testIsValidLink() {
        testHashMap.put("#", false);
        testHashMap.put("/", false);
        testHashMap.put("/payment.html", true);
        testHashMap.put("tel:+74951437771", false);
        testHashMap.put("tel:8 (800) 500-05-22", false);
        testHashMap.put("mailto:hello@skillbox.ru", false);
        testHashMap.put("http://vk.com/playback_ru", false);
        testHashMap.put("https://www.behance.net/AllSkill", false);
        testHashMap.put("https://www.playback.ru", false);
        testHashMap.put("https://partners.skillbox.ru/referral", false);
        testHashMap.put("https://www.partners.skillbox.ru/referral", false);
        testHashMap.put("http://www.partners.skillbox.ru/referral", false);
        testHashMap.put("https://b2b.skillbox.ru/", false);
        testHashMap.put("https://skillbox.ru/course/profession-python/", true);
        testHashMap.put("https://www.skillbox.ru/course/profession-python/", true);
        testHashMap.put("http://www.skillbox.ru/course/profession-python/", true);

        SiteEntity site = new SiteEntity(StatusType.INDEXING, new Date(),
                "", "https://www.skillbox.ru","Skillbox");
        when(pageEntity.getSite()).thenReturn(site);
        ArrayList<Boolean> expectedResult = new ArrayList<>();
        ArrayList<Boolean> actualResult = new ArrayList<>();
        for (Map.Entry testEntry : testHashMap.entrySet()) {
            expectedResult.add((Boolean) testEntry.getValue());
            actualResult.add(pageService.isValidLink(testEntry.getKey().toString(), pageEntity));
        }
        assertArrayEquals(expectedResult.toArray(), actualResult.toArray());
    }

    @Test
    @DisplayName("join url and href")
    public void testJoinSiteUrlAndHref() {
        ArrayList<List<String>> listTestSet = new ArrayList<>();
        listTestSet.add(List.of("https://www.playback.ru", "/payment.html", "https://www.playback.ru/payment.html"));
        listTestSet.add(List.of("https://www.playback.ru", "payment.html", "https://www.playback.ru/payment.html"));
        listTestSet.add(List.of("https://www.playback.ru/", "/payment.html", "https://www.playback.ru/payment.html"));
        listTestSet.add(List.of("https://www.playback.ru/", "payment.html", "https://www.playback.ru/payment.html"));

        ArrayList<String> expectedResult = new ArrayList<>();
        ArrayList<String> actualResult = new ArrayList<>();
        for (List<String> testSet : listTestSet) {
            actualResult.add(pageService.joinSiteUrlAndHref(testSet.get(0), testSet.get(1)));
            expectedResult.add(testSet.get(2));
        }
        assertArrayEquals(expectedResult.toArray(), actualResult.toArray());
    }

}
