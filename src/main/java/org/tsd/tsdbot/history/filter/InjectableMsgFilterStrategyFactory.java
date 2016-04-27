package org.tsd.tsdbot.history.filter;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InjectableMsgFilterStrategyFactory {

    private static Logger log = LoggerFactory.getLogger(InjectableMsgFilterStrategyFactory.class);

    @Inject
    protected Injector injector;

    public void injectStrategy(MessageFilterStrategy strat) {
        log.info("Creating MessageFilterStrategy {}", strat.getClass().getName());
        injector.injectMembers(strat);
    }
}
