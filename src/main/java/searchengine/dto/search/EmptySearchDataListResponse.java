package searchengine.dto.search;

import java.util.ArrayList;
import java.util.List;

public class EmptySearchDataListResponse extends BasicResponse{
    private int count = 0;
    private List<SearchData> searchDataList = new ArrayList<>();
    public EmptySearchDataListResponse() {
        super(true);
    }
}
