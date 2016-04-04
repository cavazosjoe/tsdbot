package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.FilenameLibrary;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.IRCUtil;

import java.io.IOException;
import java.util.List;

@Singleton
@Function(initialRegex = "^\\.(filename|fname).*")
public class Filename extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(Filename.class);

    private static final String filenamesLocation = "http://www.teamschoolyd.org/filenames/";

    private final FilenameLibrary library;

    @Inject
    public Filename(Bot bot, FilenameLibrary library) {
        super(bot);
        this.description = "Pull a random entry from the TSD Filenames Database";
        this.usage = "USAGE: .fname [ add <filename> <URL> ] [ submit <filename> <URL> ] [ get <query> ]";
        this.library = library;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {
            try {
                bot.sendMessage(channel, library.get(null));
            } catch (FilenameLibrary.FilenameRetrievalException e) {
                bot.sendMessage(channel, "Error: " + e.getMessage());
            }
            return;
        }

        switch(cmdParts[1]) {
            case "add": {
                if(!bot.userHasGlobalPriv(sender, User.Priv.OP)) {
                    bot.sendMessage(channel, "Only ops can add filenames directly. Please use \".fname submit <filename> <URL>\"");
                    return;
                }
                if(cmdParts.length != 4) {
                    bot.sendMessage(channel, usage);
                    return;
                }
                String name = cmdParts[2];
                String url = cmdParts[3];
                try {
                    String path = library.addFilename(name, url);
                    bot.sendMessage(channel, "Filename successfully added: " + path);
                } catch (FilenameLibrary.FilenameValidationException e) {
                    bot.sendMessage(channel, "Error: " + e.getMessage());
                } catch (IOException ioe) {
                    bot.sendMessage(channel, "Error downloading file, please check logs");
                }
                break;
            }
            case "submit": {
                if(cmdParts.length != 4) {
                    bot.sendMessage(channel, usage);
                    return;
                }
                String name = cmdParts[2];
                String url = cmdParts[3];
                try {
                    String id = library.submitFilename(name, url, sender, ident);
                    bot.sendMessage(channel, String.format("Submitted filename for approval (id = %s)", id));
                } catch (FilenameLibrary.FilenameValidationException e) {
                    bot.sendMessage(channel, "Error: " + e.getMessage());
                }
                break;
            }
            case "get": {
                try {
                    if (cmdParts.length == 2) {
                        bot.sendMessage(channel, library.get(null));
                    } else {
                        bot.sendMessage(channel, library.get(cmdParts[2]));
                    }
                } catch (FilenameLibrary.FilenameRetrievalException e) {
                    bot.sendMessage(channel, "Error: " + e.getMessage());
                }
                break;
            }
            case "approve": {
                if(!bot.userHasGlobalPriv(sender, User.Priv.OP)) {
                    bot.sendMessage(channel, "Only ops can approve filenames");
                    return;
                }

                if(cmdParts.length != 3) {
                    bot.sendMessage(channel, "USAGE: .fname approve <id>");
                    return;
                }

                try {
                    String url = library.approve(cmdParts[2]);
                    bot.sendMessage(channel, "Filename approved: " + url);
                } catch (IOException e) {
                    bot.sendMessage(channel, "Error approving file, please check logs");
                }
                break;
            }
            case "deny": {
                if(!bot.userHasGlobalPriv(sender, User.Priv.OP)) {
                    bot.sendMessage(channel, "Only ops can deny filenames");
                    return;
                }

                if(cmdParts.length != 3) {
                    bot.sendMessage(channel, "USAGE: .fname deny <id>");
                    return;
                }

                try {
                    library.deny(cmdParts[2]);
                    bot.sendMessage(channel, "Filename " + IRCUtil.color("DENIED", IRCUtil.IRCColor.red));
                } catch (FilenameLibrary.FilenameRetrievalException e) {
                    bot.sendMessage(channel, "Error: " + e.getMessage());
                }
                break;
            }
            case "list": {
                List<FilenameLibrary.FilenameSubmission> submissions = library.listSubmissions();
                if(submissions.size() == 0) {
                    bot.sendMessage(channel, "There are no filenames pending approval");
                } else {
                    bot.sendMessage(channel, "Sending you a list of the current filename submission queue, " + sender);
                    for(FilenameLibrary.FilenameSubmission submission : submissions) {
                        bot.sendMessage(sender, String.format("( %s ) %s: %s -- %s",
                                submission.getId(),
                                submission.getSubmitterNick(),
                                submission.getName(),
                                submission.getUrl()));
                    }
                }
                break;
            }
            default: {
                bot.sendMessage(channel, usage);
            }
        }


    }

}
