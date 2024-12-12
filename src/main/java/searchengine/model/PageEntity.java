package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cascade;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "page")
@Component

public class PageEntity implements Cloneable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @OneToMany(mappedBy = "pageEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Index> indexSet;
    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE (path(256))")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    public PageEntity() {
    }

    public PageEntity(SiteEntity site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity pageEntity = (PageEntity) o;
        return Objects.equals(path, pageEntity.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public PageEntity clone() {
        return new PageEntity(site, path, code, content);
    }
}
