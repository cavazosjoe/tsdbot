package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InjectableStreamFactory {

    private static Logger log = LoggerFactory.getLogger(InjectableStreamFactory.class);

    @Inject
    protected Injector injector;

    public TSDFMStream newStream(TSDFMQueueItem music) {
        log.debug("Creating TSDFMStream, {}", music);
        TSDFMStream stream = injector.getInstance(TSDFMStream.class);
        stream.init(music);
        return stream;
    }
}
