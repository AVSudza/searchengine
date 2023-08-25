package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.indexing.IndexingResponse;

import java.util.concurrent.ExecutionException;

public interface IndexingService {
   IndexingResponse startIndexing();
   IndexingResponse stopIndexing();
}
