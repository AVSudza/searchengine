package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Lemma;

import java.util.List;

public interface IndexPageService {
    IndexingResponse addPage(String url);

    List<Lemma> findLemmaSite(String lemma, int siteId);
}
