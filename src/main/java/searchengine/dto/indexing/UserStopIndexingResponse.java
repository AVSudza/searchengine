package searchengine.dto.indexing;

public class UserStopIndexingResponse extends IndexingResponse{
    public UserStopIndexingResponse() {
        setResult(false);
        setError("Индексация остановлена пользователем");
    }
}
