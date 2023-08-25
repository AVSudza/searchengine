package searchengine.dto.search;

public class ErrorResponse extends BasicResponse {
    String error;
    public ErrorResponse(String error) {
        super(false);
        this.error = error;
    }
}
