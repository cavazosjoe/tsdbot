package org.tsd.tsdbot;

import org.tsd.tsdbot.functions.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 11/18/2014.
 */
public enum Command {

    XBOX_LIVE(
            "^\\.xbl.*",
            "Xbox Live utility",
            "USAGE: WIP",
            null,
            XboxLive.class
    ),

    COMMAND_LIST(
            "^\\.cmd$",
            "Have the bot send you a list of commands",
            "USAGE: .cmd",
            null,
            CommandList.class
    ),

    PRINTOUT(
            "^(TSDBot.*?printout.*|.*?\\.+.*?)",
            "Get a printout",
            "USAGE: TSDBot can you get me a printout of [query]",
            null, Printout.class
    ),

    DEEJ(
            "^\\.deej$",
            "DeeJ utility. Picks a random line from the channel history and makes it all fancy and shit",
            "USAGE: .deej",
            null,
            Deej.class
    ),

    GV(
            "^\\.gv.*",
            "The Generally Vague Utility, I guess, but I don't know why you would want to use it, unless you had" +
                    "a good reason, but I guess that goes without saying, even though I never really had to," +
                    "because if I did have to, I would have just done it",
            "USAGE: .gv [pls]",
            null,
            GeeVee.class
    ),

    TSDTV(
            "^\\.tsdtv.*",
            "The TSDTV Streaming Entertainment Value Service",
            "USAGE: .tsdtv [ catalog [<directory>] | play [<movie-name> | <directory> <movie-name>] ]",
            null,
            org.tsd.tsdbot.functions.TSDTV.class
    ),

    FILENAME(
            "^\\.(filename|fname)$",
            "Pull a random entry from the TSD Filenames Database",
            "USAGE: .filename",
            null,
            Filename.class
    ),

    REPLACE(
            "^s/.+?/[^/]*",
            "Replace stuff",
            "USAGE: s/text1/text2",
            null,
            Replace.class
    ),

    CHOOSE(
            "^\\.choose.*",
            "Have the bot choose a random selection for you",
            "USAGE: .choose option1 | option2 [ | option3...]",
            null,
            Chooser.class
    ),

    SHUT_IT_DOWN(
            "^\\.SHUT_IT_DOWN$",
            "SHUT IT DOWN (owner only)",
            "USAGE: SHUT IT DOWN",
            null,
            ShutItDown.class
    ),

    BLUNDER_COUNT(
            "^\\.blunder.*",
            "View, manage, and update the blunder count",
            "USAGE: .blunder [ count | + ]",
            null,
            BlunderCount.class
    ),

    TOM_CRUISE(
            "^\\.tc.*",
            "Generate a random Tom Cruise clip or quote",
            "USAGE: .tc [ clip | quote ]",
            null,
            TomCruise.class
    ),

    HBO_FORUM(
            "^\\.hbof.*",
            "HBO Forum utility: browse recent HBO Forum posts",
            "USAGE: .hbof [ list | pv [postId (optional)] ]",
            null,
            OmniPost.class
    ),

    HBO_NEWS(
            "^\\.hbon.*",
            "HBO News utility: browse recent HBO News posts",
            "USAGE: .hbon [ list | pv [postId (optional)] ]",
            null,
            OmniPost.class
    ),

    DBO_FORUM(
            "^\\.dbof.*",
            "DBO Forum utility: browse recent DBO Forum posts",
            "USAGE: .dbof [ list | pv [postId (optional)] ]",
            null,
            OmniPost.class
    ),

    DBO_NEWS(
            "^\\.dbon.*",
            "DBO News utility: browse recent DBO News posts",
            "USAGE: .dbon [ list | pv [postId (optional)] ]",
            null,
            OmniPost.class
    ),

    STRAWPOLL(
            "^\\.poll.*",
            "Strawpoll: propose a question and choices for the chat to vote on",
            "USAGE: .poll <question> ; <duration (integer)> ; choice 1 ; choice 2 [; choice 3 ...]",
            new String[] {"abort"},
            StrawPoll.class
    ),

    VOTE(
            "^\\.vote.*",
            null, // don't show up in the dictionary
            ".vote <number of your choice>",
            null,
            null
    ),

    TWITTER(
            "^\\.tw.*",
            "Twitter utility: send and receive tweets from our exclusive @TSD_IRC Twitter account! Propose tweets" +
                    " for the chat to vote on.",
            "USAGE: .tw [ following | timeline | tweet <message> | reply <reply-to-id> <message> | " +
                    "follow <handle> | unfollow <handle> | propose [ reply <reply-to-id> ] <message> ]",
            new String[] {"abort","aye"},
            org.tsd.tsdbot.functions.Twitter.class
    ),

    FOURCHAN(
            "^\\.(4chan|4ch).*",
            "4chan \"utility\". Currently just retrieves random images from a board you specify",
            "USAGE: .4chan <board>",
            null,
            FourChan.class
    ),

    SANIC(
            "^\\.sanic$",
            "Sanic function. Retrieves a random page from the Sonic fanfiction wiki",
            "USAGE: .sanic",
            null,
            Sanic.class
    ),

    RECAP(
            "^\\.recap",
            "Recap function. Get a dramatic recap of recent chat history",
            "USAGE: .recap [ minutes (integer) ]",
            null,
            Recap.class
    ),

    WORKBOT(
            "^\\.(wod|workbot|werkbot).*",
            "TSD WorkBot. Get a randomized workout for today, you lazy sack of shit",
            "USAGE: .workbot [ options ]",
            null,
            Wod.class
    ),

    CATCHUP(
            "^\\.catchup.*",
            "Catchup function. Get a personalized review of what you missed",
            "USAGE: .catchup",
            null,
            Archivist.class
    ),

    SCAREQUOTE(
            "^\\.quote",
            "Scare quote \"function\"",
            "USAGE: .quote",
            null,
            ScareQuote.class
    ),

    ZACKDARK(
            "^\\s*(o/|\\\\o)\\s*$",
            ";)",
            ";)",
            null,
            SillyZackDark.class
    );

    private String regex;
    private String desc;
    private String usage;
    private String[] threadCommands; // used by running threads, not entry point
    private Class<? extends MainFunction> functionMap;

    Command(String regex, String desc, String usage, String[] threadCommands, Class<? extends MainFunction> functionMap) {
        this.regex = regex;
        this.desc = desc;
        this.usage = usage;
        this.threadCommands = threadCommands;
        this.functionMap = functionMap;
    }

    public String getDesc() {
        return desc;
    }

    public String getUsage() {
        return usage;
    }

    public String getRegex() { return regex; }

    public Class<? extends MainFunction> getFunctionMap() {
        return functionMap;
    }

    public boolean threadCmd(String cmd) {
        if(threadCommands == null) return false;
        for(String s : threadCommands) {
            if(s.equals(cmd)) return true;
        }
        return false;
    }

    public static List<Command> fromString(String s) {
        LinkedList<Command> matches = new LinkedList<>();
        for(Command c : values()) {
            if(s.matches(c.getRegex()))
                matches.add(c);
        }
        return matches;
    }

    public static List<Command> fromFunction(MainFunction function) {
        LinkedList<Command> matches = new LinkedList<>();
        for(Command c : values()) {
            if(function.getClass().equals(c.getFunctionMap()))
                matches.add(c);
        }
        return matches;
    }

}
