package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByLemmaAndPageEntity(Lemma lemma, PageEntity pageEntity);
    List<Index> findByPageEntity(PageEntity pageEntity);
    List<Index> findByLemma(Lemma lemma);
    @Query(value = "SELECT * FROM search_engine.search_index AS s_i " +
            "WHERE s_i.lemma_id in (SELECT s_l.id FROM search_engine.lemma AS s_l WHERE s_l.lemma = :lemma) " +
            "AND s_i.page_id IN " +
            "(SELECT s_p.id FROM search_engine.page AS s_p WHERE s_p.site_id = :site_id)", nativeQuery = true)
    List<Index> findBySite(String lemma, int site_id);
//    @Query(value = "SELECT * FROM search_engine.search_index AS s_i WHERE s_i.lemma_id = :lemma_id AND s_i.page_id IN " +
//                   "(SELECT s_p.id FROM search_engine.page AS s_p WHERE s_p.site_id = :site_id)", nativeQuery = true)
//    List<Index> findBySite(int lemma_id, int site_id);
    @Modifying
    @Query("DELETE FROM Index i WHERE i.id in ?1")
    @Transactional
    void delete(Set<Integer> id);
}
