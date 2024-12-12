package searchengine.dto.search;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
@EqualsAndHashCode(callSuper = true)
@Data
public class SearchResponse extends BasicResponse {
    private int count;
    private List<SearchData> data;
    public SearchResponse() {
        super(true);
    }

    public SearchResponse(boolean result, int count, List<SearchData> data) {
        super(result);
        this.count = count;
        this.data = data;
    }
}
