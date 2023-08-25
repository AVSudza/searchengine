package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteDB;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findByLemmaAndSiteId(String lemma, int sideId);
    List<Lemma> findBySiteId(int siteId);
    int countLemmaBySite(SiteDB siteDB);
    @Query(value = "SELECT count(distinct site_id) FROM lemma", nativeQuery = true)
    int countDistinctSite();
    List<Lemma> findByLemma(String lemmaString);
}
