package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;
@Getter
@Setter
@Entity
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Index> indexSet;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String lemma;
    @Column(nullable = false)
    private int frequency;

    public Lemma() {
    }

    public Lemma(Set<Index> indexSet, SiteEntity site, String lemma, int frequency) {
        this.indexSet = indexSet;
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return id == lemma1.id && frequency == lemma1.frequency
                && Objects.equals(indexSet.size(), lemma1.getIndexSet().size())
                && Objects.equals(site.getId(), lemma1.getSite().getId())
                && Objects.equals(lemma, lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, lemma, frequency);
    }
}
