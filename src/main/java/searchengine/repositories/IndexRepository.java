package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByLemmaAndPageDB(Lemma lemma, PageDB pageDB);
    List<Index> findByPageDB(PageDB pageDB);
    List<Index> findByLemma(Lemma lemma);
    @Query(value = "SELECT * FROM search_engine.search_index AS s_i WHERE s_i.page_id IN " +
                   "(SELECT s_l.id FROM search_engine.lemma AS s_l WHERE s_l.site_id =  :site_id))", nativeQuery = true)
    List<Index> findBySite(int site_id);

}
