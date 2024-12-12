package searchengine.dto.search;

import lombok.Data;

@Data
public class BasicResponse {
    boolean result;
    public BasicResponse() {
    }
    public BasicResponse(boolean result) {
        this.result = result;
    }
}
