package searchengine.services;

import searchengine.dto.search.BasicResponse;
import searchengine.dto.search.SearchQuery;
import searchengine.dto.search.SearchResponse;

public interface SearchService {
    BasicResponse search(String query, String site);
}
