package searchengine.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.exeptions.StopIndexingException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PageServiceImpl implements PageService {
    @Getter
    private PageEntity pageEntity;
    @Getter
    private Document content = new Document("");
    @Getter
    private final List<PageEntity> children = new ArrayList<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private int i;

    public PageServiceImpl(PageEntity pageEntity, SiteRepository siteRepository,
                           PageRepository pageRepository) throws StopIndexingException {
        this.pageEntity = pageEntity;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageServiceImpl pageServiceImpl = (PageServiceImpl) o;
        return Objects.equals(pageEntity.getPath(), pageServiceImpl.getPageEntity().getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageEntity.getPath());
    }

    void getPageInfo() throws IOException  {
        randomSleep();
        Connection.Response connection;
        Document contentPage;
        pageEntity.setContent(" ");
        try {
            connection = Jsoup.connect(pageEntity.getPath())
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .execute();

            contentPage = connection.parse();

            pageEntity.setCode(connection.statusCode());
            pageEntity.setContent(contentPage.html());
        } catch (HttpStatusException e) {
            pageEntity.setCode(e.getStatusCode());
            throw new HttpStatusException(e.getMessage(), e.getStatusCode(), e.getUrl());
        } catch (IOException e) {
            pageEntity.setCode(500);
            throw new IOException(pageEntity.getPath());
        }
        this.content = contentPage;
    }

    public List<PageEntity> findChildren() throws StopIndexingException, IOException {

        Elements anchors;
        try {
            anchors = content.select("a");
        } catch (NullPointerException e) {
            return new ArrayList<>();
        }

        for (Element anchor : anchors) {

            if (!IndexingServiceImpl.isIndexing) {
                throw new StopIndexingException();
            }
            String href = anchor.attr("href");
            if (!isValidLink(href, pageEntity)) {
                continue;
            }
            PageEntity newPageEntity = new PageEntity();
            newPageEntity.setSite(pageEntity.getSite());
            newPageEntity.setPath(joinSiteUrlAndHref(pageEntity.getSite().getUrl(), href));
            PageServiceImpl pageService = new PageServiceImpl(newPageEntity, siteRepository, pageRepository);

            if (children.contains(newPageEntity) || pageService.existsPageInDB()) {
                continue;
            }
            children.add(newPageEntity);
            pageEntity.getSite().setStatusTime(new Date());
            siteRepository.save(pageEntity.getSite());
        }
        return children;
    }

    String joinSiteUrlAndHref(String url, String href) {
        if (url.matches(".+/$")) {
            url = url.substring(0, url.length() - 1);
        }
        if (href.matches("^/.+")) {
            href = href.substring(1);
        }
        return url + "/" + href;
    }

    @Override
    public boolean existsPageInDB() {
        synchronized (pageRepository) {
            return pageRepository.existsPageEntityByPath(pageEntity.getPath());
        }
    }

    @Override
    public void savePageToDB() {
        synchronized (pageRepository) {
            if (!existsPageInDB()) {
                pageEntity = pageRepository.save(pageEntity);
            } else {
                pageEntity = pageRepository.findPageEntityByPath(pageEntity.getPath());
            }
        }
    }

    @Override
    public void deletePageFromDB() {
        synchronized (pageRepository) {
            if (existsPageInDB()) {
                pageRepository.delete(new HashSet<>(pageEntity.getId()));
            } else {
                log.info(pageEntity.getPath() + " is missing ");
            }
        }
    }

    boolean isValidLink(String href, PageEntity page) {
        boolean valid = false;
        String domainName = getDomain(page.getSite().getUrl());

        if (href.matches("https?://(www.)?(" + domainName + ").+")) {
            valid = true;
        }
        if (href.matches("^/.+")) {
            valid = true;
        }
        return valid;
    }

    private void randomSleep() {
        int REQUEST_DELAY_MIN = 500;
        int REQUEST_DELAY_MAX = 5000;
        long randomSleep = Math.round(Math.random() * REQUEST_DELAY_MIN) + REQUEST_DELAY_MAX - REQUEST_DELAY_MIN;
        try {
            Thread.sleep(randomSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getDomain(String url) {
        String regex = "http(s)?://(www\\.)?(.+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return (matcher.find()) ? matcher.group(3) : "";
    }


}
