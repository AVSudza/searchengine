package searchengine.services;

import org.springframework.jdbc.core.RowMapper;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PageEntityRowMapper implements RowMapper<PageEntity> {
    private static volatile PageEntityRowMapper INSTANCE;

    @Override
    public PageEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        PageEntity existingPage = new PageEntity();
        existingPage.setId(rs.getInt("Id"));
        existingPage.setCode(rs.getInt("code"));
        existingPage.setContent(rs.getString("content"));
        existingPage.setPath(rs.getString("path"));
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setId(rs.getInt("site_id"));
        existingPage.setSite(siteEntity);
        return existingPage;
    }

    private PageEntityRowMapper() {
    }

    public static synchronized PageEntityRowMapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PageEntityRowMapper();
        }
        return INSTANCE;
    }
}
