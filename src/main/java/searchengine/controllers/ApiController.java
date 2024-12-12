package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.BasicResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexPageService;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping(value = "/api", method = {RequestMethod.GET, RequestMethod.POST})
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final IndexPageService indexPageService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService,
                         IndexPageService indexPageService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.indexPageService = indexPageService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse startResponse = indexingService.startIndexing();
        if (startResponse.isResult()) {
            return new ResponseEntity<>(startResponse, HttpStatus.OK);
        }
        return new ResponseEntity<>(startResponse, HttpStatus.FORBIDDEN);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse stopResponse = indexingService.stopIndexing();
        if (stopResponse.isResult()) {
            return new ResponseEntity<>(stopResponse, HttpStatus.OK);
        }
        return new ResponseEntity<>(stopResponse, HttpStatus.FORBIDDEN);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> addPage(@RequestParam String url) {
        IndexingResponse addResponse = indexPageService.addPage(url);
        if (addResponse.isResult()) {
            return new ResponseEntity<>(addResponse, HttpStatus.OK);
        }
        return new ResponseEntity<>(addResponse, HttpStatus.NOT_FOUND);

    }

    @GetMapping("/search")
    public ResponseEntity<BasicResponse> search(@RequestParam String query,
                                                @RequestParam(defaultValue = "") String site) {
        BasicResponse searchResponse = searchService.search(query, site);
        if (searchResponse.isResult()) {
            return new ResponseEntity<>(searchResponse, HttpStatus.OK);
        }
        return new ResponseEntity<>(searchResponse, HttpStatus.NOT_FOUND);
    }
}
