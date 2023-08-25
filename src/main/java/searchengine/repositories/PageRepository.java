package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageDB, Integer> {
    boolean existsPageDBByPath(String path);
    PageDB getPageDBByPath(String path);
    int countPageDBBySite(SiteDB siteDB);
}
