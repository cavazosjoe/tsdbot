package org.tsd.tsdbot.scheduled;

import com.google.inject.Inject;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.notifications.NotificationEntity;
import org.tsd.tsdbot.notifications.NotificationManager;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NotificationSweeperJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSweeperJob.class);

    @Inject
    protected TSDBot bot;

    @Inject
    protected PoolingHttpClientConnectionManager connectionManager;

    @Inject
    protected Set<NotificationManager> notificationManagers;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        try {
            for(NotificationManager<NotificationEntity> sweeper : notificationManagers) {
                sweeper.sweepAndNotify();
            }
            connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Notification Sweeper error", e);
            bot.incrementBlunderCnt();
        }
    }
}
