package org.tsd.tsdbot.model.warzone.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import org.tsd.tsdbot.model.warzone.WarzoneGame;
import org.tsd.tsdbot.model.warzone.WarzoneGamePlayer;

import java.sql.SQLException;

/**
 * Created by Joe on 10/31/2015.
 */
public class WarzoneGameDao extends BaseDaoImpl<WarzoneGame, String> {

    public WarzoneGameDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, WarzoneGame.class);
    }

    @Override
    public int delete(WarzoneGame data) throws SQLException {
        WarzoneGamePlayerDao playerDao = new WarzoneGamePlayerDao(getConnectionSource());
        for(WarzoneGamePlayer player : data.getPlayers()) {
            playerDao.delete(player);
        }
        return super.delete(data);
    }
}
