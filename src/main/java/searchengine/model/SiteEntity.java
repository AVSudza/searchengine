package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "site")
@Component
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<PageEntity> setPageEntity;
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Lemma> setLemma;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL")
    private StatusType status;
    @Column(name = "status_time", nullable = false, columnDefinition = "DATETIME")
    private Date statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

    public SiteEntity() {
    }

    public SiteEntity(StatusType status, Date statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public StatusType getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteEntity site = (SiteEntity) o;
        return status == site.status && Objects.equals(statusTime.getTime(), site.statusTime.getTime())
                && Objects.equals(lastError, site.lastError) && Objects.equals(url, site.url)
                && Objects.equals(name, site.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, statusTime, lastError, url, name);
    }
}
