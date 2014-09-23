package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Joe on 9/21/2014.
 */
@Singleton
public class InjectableStreamFactory {

    private static Logger logger = LoggerFactory.getLogger(InjectableStreamFactory.class);

    @Inject
    protected Injector injector;

    public TSDTVStream newStream(String pathToMovie) {
        logger.info("Creating TSDTVStream with pathToMovie={}", pathToMovie);
        TSDTVStream stream = injector.getInstance(TSDTVStream.class);
        stream.init(pathToMovie);
        return stream;
    }
}
