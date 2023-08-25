package searchengine.dto.indexing;

public class ErrorReadPageResponse extends IndexingResponse {
    public ErrorReadPageResponse() {
        super();
        setResult(false);
        setError("Ошибка чтения веб-страницы");
    }
}
