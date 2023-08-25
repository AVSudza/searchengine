package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "site")
@Component
public class SiteDB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private Set<PageDB> pageDB;
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private Set<Lemma> lemma;
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

    public SiteDB() {
    }

    public SiteDB(StatusType status, Date statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public StatusType getStatus() {
        return status;
    }
}
