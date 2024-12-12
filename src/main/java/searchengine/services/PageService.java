package searchengine.services;

import searchengine.model.PageEntity;

public interface PageService {
    boolean existsPageInDB();
    void savePageToDB();
    void deletePageFromDB();
    PageEntity getPageEntity();
}
