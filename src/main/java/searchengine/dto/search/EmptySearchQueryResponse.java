package searchengine.dto.search;

public class EmptySearchQueryResponse extends ErrorResponse{
    public EmptySearchQueryResponse() {
        super("Задан пустой поисковый запрос");
    }
}
