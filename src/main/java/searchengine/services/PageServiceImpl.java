package searchengine.services;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.PageDB;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.exeptions.StopIndexingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PageServiceImpl implements PageService {
    private final int REQUEST_DELAY_MIN = 500;
    private final int REQUEST_DELAY_MAX = 5000;
    @Getter
    private final PageDB pageDB;
    @Getter
    private Document content = new Document("");
    @Getter
    private List<PageDB> children = new ArrayList<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexPageService indexPageService;

    public PageServiceImpl(PageDB pageDB, SiteRepository siteRepository,
                           PageRepository pageRepository,
                           IndexPageService indexPageService) throws StopIndexingException {
        this.pageDB = pageDB;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexPageService = indexPageService;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageServiceImpl pageServiceImpl = (PageServiceImpl) o;
        return Objects.equals(pageDB.getPath(), pageServiceImpl.getPageDB().getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageDB.getPath());
    }

    public void getPageInfo() throws IOException {
        randomSleep();
        Connection.Response connection = null;
        Document content;
        pageDB.setContent(" ");
        try {
            connection = Jsoup.connect(pageDB.getPath())
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .execute();

            content = connection.parse();

            pageDB.setCode(connection.statusCode());
            pageDB.setContent(content.html());
        } catch (HttpStatusException e) {
            pageDB.setCode(e.getStatusCode());
            throw new HttpStatusException(e.getMessage(), e.getStatusCode(), e.getUrl());
        } catch (IOException e) {
            pageDB.setCode(500);
            throw new IOException(pageDB.getPath());
        }
        this.content = content;
    }

    public List<PageDB> findChildren() throws StopIndexingException, IOException {

        synchronized (pageRepository) {
            if (pageRepository.existsPageDBByPath(pageDB.getPath())) {
                return new ArrayList<>();
            }
        }

        getPageInfo();

        indexPageService.addPage(pageDB.getPath());

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

            if (!isValidLink(href)) {
                continue;
            }
            if (href.matches("^\\/.+")) {
                href = pageDB.getSite().getUrl() + href;
            }
            PageDB newPageDB = new PageDB();
            newPageDB.setSite(pageDB.getSite());
            newPageDB.setPath(href);

            if (children.contains(newPageDB) || existsPageInDB(newPageDB)) {
                continue;
            }
            children.add(newPageDB);
            pageDB.getSite().setStatusTime(new Date());
            siteRepository.save(pageDB.getSite());
        }
        return children;
    }

    @Override
    public boolean existsPageInDB(PageDB page) {
        synchronized (pageRepository) {
            return pageRepository.existsPageDBByPath(page.getPath());
        }
    }
    @Override
    public boolean existsPageInDB() {
        synchronized (pageRepository) {
            return pageRepository.existsPageDBByPath(pageDB.getPath());
        }
    }
    @Override
    public void savePageToDB() {
        synchronized (pageRepository) {
            if (!existsPageInDB()) {
                pageRepository.save(pageDB);
                Logger.getLogger(this.getClass().getSimpleName()).info("save " + pageDB.getPath()); //todo
            } else {
                Logger.getLogger(this.getClass().getSimpleName()).info(pageDB.getPath() + " is exists ");//todo
            }
        }
    }

    private boolean isValidLink(String href) {
        boolean valid = false;
        String domainName = getDomain(pageDB.getSite().getUrl());

        if (href.matches(".+(" + domainName + ").+")) {
            valid = true;
        }
        if (href.matches("^\\/.+")) {
            valid = true;
        }
        return valid;
    }

    private void randomSleep() {
        long randomSleep = Math.round(Math.random() * REQUEST_DELAY_MIN) + REQUEST_DELAY_MAX - REQUEST_DELAY_MIN;
        try {
            Thread.sleep(randomSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getDomain(String url) {
        String regex = "https://(www\\.)?(.+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        matcher.find();
        return matcher.group(2);
    }


}
