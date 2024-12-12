package searchengine.dto.indexing;

public class ErrorReadPageResponse extends IndexingResponse {
    String page;
    public ErrorReadPageResponse(String page) {
        super();
        setResult(false);
        setError("Ошибка чтения веб-страницы: " + page);
        this.page = page;
    }

    public String getPage() {
        return page;
    }
}
