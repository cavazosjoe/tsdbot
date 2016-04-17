package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InjectableStreamFactory {

    private static Logger logger = LoggerFactory.getLogger(InjectableStreamFactory.class);

    @Inject
    protected Injector injector;

    public TSDTVStream newStream(String videoFilter, TSDTVQueueItem movie) {
        logger.debug("Creating TSDTVStream, {}", movie);
        TSDTVStream stream = injector.getInstance(TSDTVStream.class);
        stream.init(videoFilter, movie);
        return stream;
    }
}
