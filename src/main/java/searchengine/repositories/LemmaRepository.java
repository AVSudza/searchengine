package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findByLemmaAndSiteId(String lemma, int sideId);
    List<Lemma> findBySiteId(int siteId);
    int countLemmaBySite(SiteEntity siteEntity);
    @Query(value = "SELECT count(distinct site_id) FROM lemma", nativeQuery = true)
    int countDistinctSite();
    List<Lemma> findByLemma(String lemmaString);

    @Modifying
    @Query("DELETE FROM Lemma l WHERE l.id in ?1")
    @Transactional
    void delete(Set<Integer> id);
}
