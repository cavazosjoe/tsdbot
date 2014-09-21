package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.maxsvett.fourchan.board.Board;
import com.maxsvett.fourchan.page.Page;
import com.maxsvett.fourchan.post.Post;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class FourChan extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(FourChan.class);

    private HttpClient httpClient;

    @Inject
    public FourChan(TSDBot bot, HttpClient httpClient) {
        super(bot);
        this.httpClient = httpClient;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length != 2) {
            bot.sendMessage(channel, TSDBot.Command.FOURCHAN.getUsage());
            return;
        }

        String boardRegex = "^/??(\\w{1,3})/??$";
        if(!cmdParts[1].matches(boardRegex)) {
            bot.sendMessage(channel, "Could not understand which board you want. Ex: .4chan /v/");
            return;
        }

        Pattern boardPattern = Pattern.compile(boardRegex);
        Matcher boardMatcher = boardPattern.matcher(cmdParts[1]);
        while(boardMatcher.find()) {

            HttpGet indexGet = null;
            try {

                String boardPath = "/" + boardMatcher.group(1) + "/";
                Board board = Board.getBoard(boardPath);
                if(board.isNsfw()) {
                    bot.sendMessage(channel, "I don't support NSFW boards... yet.");
                    return;
                }

                indexGet = new HttpGet("https://a.4cdn.org" + boardPath + "1.json");
                indexGet.setHeader("User-Agent", "Mozilla/4.0");
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String jsonResponse = httpClient.execute(indexGet, responseHandler);

                Page page = com.maxsvett.fourchan.FourChan.parsePage(board, jsonResponse);
                Random rand = new Random();
                com.maxsvett.fourchan.thread.Thread randomThread = page.getThreads()[1 + rand.nextInt(page.getThreads().length-1)];
                LinkedList<Post> imagePosts = new LinkedList<>();
                imagePosts.add(randomThread.getOP());
                for(Post post : randomThread.getPosts()) {
                    if(post.hasImage()) imagePosts.add(post);
                }

                Post chosen = imagePosts.get(rand.nextInt(imagePosts.size()));
                String comment = IRCUtil.trimToSingleMsg(HtmlSanitizer.sanitize(chosen.getComment().replace("<br>", " ")));
                if(comment != null && (!comment.isEmpty()) && comment.length() < 100)   // also send the comment if
                    bot.sendMessage(channel, comment);                                  // it's small enough
                String ext = chosen.getImageURL().toString().substring(chosen.getImageURL().toString().length() - 4);
                String ret = "(" + chosen.getImageName() + ext + ") " + chosen.getImageURL();

                bot.sendMessage(channel, ret);

            } catch (Exception e) {
                logger.error("fourchan() error", e);
                bot.sendMessage(channel, "Error retrieving board");
                return;
            } finally {
                if(indexGet != null) indexGet.releaseConnection();
            }
        }
    }
}
