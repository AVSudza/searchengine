package searchengine.services;

import searchengine.model.PageDB;

public interface PageService {
    boolean existsPageInDB();
    boolean existsPageInDB(PageDB page);
    void savePageToDB();
    PageDB getPageDB();
}
