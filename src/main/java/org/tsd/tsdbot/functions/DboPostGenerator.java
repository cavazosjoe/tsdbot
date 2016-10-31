package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.markov.MarkovFileManager;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.MarkovUtil;

@Singleton
@Function(initialRegex = "^\\.dbogen.*")
public class DboPostGenerator extends MainFunctionImpl {

    private static final Logger log = LoggerFactory.getLogger(DboPostGenerator.class);

    private final MarkovFileManager markovFileManager;

    @Inject
    public DboPostGenerator(TSDBot bot,
                            MarkovFileManager markovFileManager) {
        super(bot);
        this.description = "DBO post generator. Generate a DBO post";
        this.usage = "USAGE: .dbogen <forum_handle>";
        this.markovFileManager = markovFileManager;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");
        if(cmdParts.length < 2) {
            bot.sendMessage(channel, getUsage());
            return;
        }

        String filename = MarkovUtil.sanitize(StringUtils.join(ArrayUtils.subarray(cmdParts, 1, cmdParts.length), " "));
        try {
            String chain = markovFileManager.generateChain(filename, 200);
            bot.sendMessage(channel, chain);
        } catch (Exception e) {
            log.error("Error generating dbo post", e);
            bot.sendMessage(channel, "Error generating DBO post");
        }
    }
}
