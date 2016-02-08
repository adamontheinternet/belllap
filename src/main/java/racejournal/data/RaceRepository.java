package racejournal.data;

import racejournal.domain.Race;
import racejournal.domain.RaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by alaplante on 2/4/16.
 */
@Repository
public class RaceRepository {
    private final static Logger logger = LoggerFactory.getLogger(RaceRepository.class);

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        logger.info("Datasource set");
    }

//    public String getEmailByName(String name) {
//        logger.info("Get email for name {}", name);
//        String sql = "select email from users where name = :name";
//        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("name", name), String.class);
//    }

    public List<Race> fetchRaces() {
        return jdbcTemplate.query("select id, name, date, city, state, race_type from races", new RaceMapper());
    }

    private final static class RaceMapper implements RowMapper<Race> {
        public Race mapRow(ResultSet rs, int rowNum) throws SQLException {
            Race race = new Race();
            race.setId(rs.getLong("id"));
            race.setName(rs.getString("name"));
            race.setDate(rs.getDate("date").toLocalDate());
            race.setCity(rs.getString("city"));
            race.setState(rs.getString("state"));
            race.setRaceType(RaceType.valueOf(rs.getString("race_type")));
            return race;
        }
    }

    // todo batch for efficiency
    public void saveRaces(List<Race> races) {
        int updateCount = 0;
        String sql = "insert into races (id, name, date, city, state, race_type) values (:id, :name, :date, :city, :state, :raceType)";
        for(Race race : races) {
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("id", race.getId());
            namedParameters.addValue("name", race.getName());
            namedParameters.addValue("date", race.getDate().toString()); //java.sql.Date.valueOf( localDate );
            namedParameters.addValue("city", race.getCity());
            namedParameters.addValue("state", race.getState());
            namedParameters.addValue("raceType", race.getRaceType().toString());
            updateCount += jdbcTemplate.update(sql, namedParameters);
        }
        logger.info("Inserted {} races", updateCount);
    }

    public List<Race> fetchRacesByType(String type) {
        logger.info("Fetch {} races", type);
        return jdbcTemplate.query("select id, name, date, city, state, race_type from races where race_type = :raceType", new MapSqlParameterSource("raceType", type), new RaceMapper());
    }
}