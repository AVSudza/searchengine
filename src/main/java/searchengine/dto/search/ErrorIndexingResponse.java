package searchengine.dto.search;

public class ErrorIndexingResponse extends ErrorResponse{
    public ErrorIndexingResponse() {
        super("Выбранные сайты не индексированы");
    }
}
