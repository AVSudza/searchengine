package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchQuery {
    private String query;
    private String site;
    private int offset;
    private int limit;

    public SearchQuery(String query, String site, int offset, int limit) {
        this.query = query;
        this.site = site;
        this.offset = offset;
        this.limit = limit;
    }

    public SearchQuery(String query, String site, int offset) {
        this(query, site, offset, 20);
    }

    public SearchQuery(String query, String site) {
        this(query, site, 0, 20);
    }

    public SearchQuery(String query) {
        this(query, "", 0, 20);
    }
}
