package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "search_index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity pageEntity;
    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;
    @Column(name = "rank_page", nullable = false)
    private float rank;

    public Index() {
    }

    public Index(PageEntity pageEntity, Lemma lemma, float rank) {
        this.pageEntity = pageEntity;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return id == index.id && Float.compare(index.rank, rank) == 0 &&
                Objects.equals(pageEntity.getPath(), index.pageEntity.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pageEntity, lemma, rank);
    }
}
