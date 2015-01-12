package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.stats.HustleStats;

import java.text.DecimalFormat;
import java.util.Properties;

/**
 * Created by Joe on 1/3/2015.
 */
@Singleton
public class Hustle extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Hustle.class);

    private static final DecimalFormat decimalFormat = new DecimalFormat("##0.00");

    private HustleStats hustleStats;
    private String serverUrl;

    @Inject
    public Hustle(TSDBot bot, HustleStats hustleStats, @Named("serverUrl") String serverUrl) {
        super(bot);
        this.hustleStats = hustleStats;
        this.serverUrl = serverUrl;
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {
        double hhr = hustleStats.getHhr();
        bot.sendMessage(channel, "Current HHR: " + decimalFormat.format(hhr) + " -- " + serverUrl + "/hustle");
    }

}
