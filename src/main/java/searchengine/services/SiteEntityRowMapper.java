package searchengine.services;

import org.springframework.jdbc.core.RowMapper;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SiteEntityRowMapper implements RowMapper<SiteEntity> {
    private static volatile SiteEntityRowMapper INSTANCE;
    @Override
    public SiteEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        SiteEntity existingSite = new SiteEntity();
        existingSite.setId(rs.getInt("Id"));
        existingSite.setLastError(rs.getString("last_error"));
        existingSite.setName(rs.getString("name"));
        existingSite.setStatus(StatusType.valueOf(rs.getString("status")));
        existingSite.setStatusTime(rs.getDate("status_time"));
        existingSite.setUrl(rs.getString("url"));
        return existingSite;
    }

    private SiteEntityRowMapper() {
    }

    public synchronized static SiteEntityRowMapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SiteEntityRowMapper();
        }
        return INSTANCE;
    }
}
