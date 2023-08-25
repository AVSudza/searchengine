package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "page")
@Component
//@Table(name = "page", indexes = @Index(columnList = "path"))//todo why doesn't it work
public class PageDB implements Cloneable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteDB site;
    @OneToMany(mappedBy = "pageDB", cascade = CascadeType.ALL)
    private Set<Index> insexSet;
    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE (path(256))")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    public PageDB() {
    }

    public PageDB(SiteDB site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageDB pageDB = (PageDB) o;
        return Objects.equals(path, pageDB.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public PageDB clone() {
        return new PageDB(site, path, code, content);
    }
}
