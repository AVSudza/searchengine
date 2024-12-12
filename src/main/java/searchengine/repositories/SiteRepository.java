package searchengine.repositories;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.Set;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity getSiteEntityByUrl(String url);
    boolean existsSiteEntityByUrl(String url);
    @Modifying
    @Query("DELETE FROM SiteEntity se WHERE se.id in ?1")
    @Transactional
    void delete(Set<Integer> id);

}
