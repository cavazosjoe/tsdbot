package org.tsd.tsdbot.scheduled;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * Created by Joe on 9/21/2014.
 */
public class InjectableJobFactory implements JobFactory {

    @Inject
    protected Injector injector;

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        return injector.getInstance(triggerFiredBundle.getJobDetail().getJobClass());
    }
}
