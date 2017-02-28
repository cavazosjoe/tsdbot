package org.tsd.tsdbot.model.warzone.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import org.tsd.tsdbot.model.warzone.WarzoneGame;
import org.tsd.tsdbot.model.warzone.WarzoneNight;

import java.sql.SQLException;

public class WarzoneNightDao extends BaseDaoImpl<WarzoneNight, Integer> {

    public WarzoneNightDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, WarzoneNight.class);
    }

    @Override
    public int delete(WarzoneNight data) throws SQLException {
        WarzoneGameDao gameDao = new WarzoneGameDao(getConnectionSource());
        for(WarzoneGame game : data.getGames()) {
            gameDao.delete(game);
        }
        return super.delete(data);
    }
}
