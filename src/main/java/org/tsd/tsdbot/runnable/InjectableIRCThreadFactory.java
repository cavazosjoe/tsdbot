package org.tsd.tsdbot.runnable;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.notifications.TwitterManager;

/**
 * Created by Joe on 1/14/2015.
 */
@Singleton
public class InjectableIRCThreadFactory {

    private static final Logger logger = LoggerFactory.getLogger(InjectableIRCThreadFactory.class);

    @Inject
    private Injector injector;

    public StrawPoll newStrawPoll(String channel, String proposer, String question,
                                  int duration, String[] options) throws Exception {
        logger.info("Creating and injecting new StrawPoll...");
        StrawPoll poll = injector.getInstance(StrawPoll.class);
        poll.init(channel, proposer, question, duration, options);
        return poll;
    }

    public TweetPoll newTweetPoll(String channel, String proposer, String proposedTweet,
                                  TwitterManager.Tweet replyTo) throws Exception {
        logger.info("Creating and injecting new TweetPoll...");
        TweetPoll poll = injector.getInstance(TweetPoll.class);
        poll.init(channel, proposer, proposedTweet, replyTo);
        return poll;
    }

    public DorjThread newDorjThread(String channel, String ident) throws Exception {
        logger.info("Creating and injecting new DorjThread...");
        DorjThread dorjThread = injector.getInstance(DorjThread.class);
        dorjThread.init(channel, ident);
        return dorjThread;
    }
}
