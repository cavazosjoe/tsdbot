package org.tsd.tsdbot.model.warzone.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import org.tsd.tsdbot.model.warzone.WarzoneGamePlayer;

import java.sql.SQLException;

public class WarzoneGamePlayerDao extends BaseDaoImpl<WarzoneGamePlayer, Void> {

    public WarzoneGamePlayerDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, WarzoneGamePlayer.class);
    }

    @Override
    public int delete(WarzoneGamePlayer data) throws SQLException {
        DeleteBuilder<WarzoneGamePlayer, Void> deleteBuilder = deleteBuilder();
        deleteBuilder
                .where()
                .eq("warzoneGameId", data.getGame().getGameId())
                .and()
                .eq("gamertag", data.getGamertag());
        return deleteBuilder.delete();
    }
}
