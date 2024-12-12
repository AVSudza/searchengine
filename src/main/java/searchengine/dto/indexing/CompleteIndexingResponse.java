package searchengine.dto.indexing;

public class CompleteIndexingResponse extends IndexingResponse{
    public CompleteIndexingResponse() {
        super();
        setResult(true);
        setError("");
    }
}
