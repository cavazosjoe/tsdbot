package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 2/7/2015.
 */
@Singleton
public class DboFireteamFunction extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(DboFireteamFunction.class);

    @Inject
    private TSDBot bot;

    @Inject
    public DboFireteamFunction(JdbcConnectionSource connectionSource) {
        int i=0;
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {

    }

    @Override
    public String getRegex() {
        return ".dboft$";
    }

}
