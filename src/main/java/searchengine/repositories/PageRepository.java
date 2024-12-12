package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsPageEntityByPath(String path);
    int countPageEntityBySite(SiteEntity siteEntity);
    PageEntity findPageEntityByPath(String path);
    @Modifying
    @Query("DELETE FROM PageEntity pe WHERE pe.id in ?1")
    @Transactional
    void delete(Set<Integer> id);

}
