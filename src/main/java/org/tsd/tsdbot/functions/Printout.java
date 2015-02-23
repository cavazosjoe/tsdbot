package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.imgscalr.Scalr;
import org.jibble.pircbot.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.PrintoutLibrary;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.ImageUtils;
import org.tsd.tsdbot.util.MiscUtils;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class Printout extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Printout.class);

    private static final String queryRegex = "^TSDBot.*?printout of (.*)";
    private static final Pattern queryPattern = Pattern.compile(queryRegex, Pattern.DOTALL);
    private static final String acceptableFormats = ".*?(JPG|jpg|PNG|png|JPEG|jpeg)$";
    private static final String outputFileType = "jpg";

    private static final String GIS_API_TARGET = "https://ajax.googleapis.com/ajax/services/search/images";
    private static final String GIS_API_VERSION = "1.0";
    private static final int GIS_NUM_RESULTS = 5;
    private static final String TSDBOT_IP = "23.252.62.178";
    private static final String GIS_REFERRER = "http://www.teamschoolyd.org";

    private String serverUrl;
    private Random random;
    private PrintoutLibrary printoutLibrary;

    // set of people who can trigger a printout with a deliberate repitition
    // e.g. "TSDBot printout of two bears" -> "Not Computing." -> "Two. Bears." -> [img]
    // ident -> (number of times they've said a line while we're listening for their repeat)
    private HashMap<String, Integer> notComputing = new HashMap<>();

    private HashSet<String> banned = new HashSet<>();

    @Inject
    public Printout(
            TSDBot bot,
            Random random,
            PrintoutLibrary library,
            @Named("serverUrl") String serverUrl) {
        super(bot);
        this.random = random;
        this.serverUrl = serverUrl;
        this.printoutLibrary = library;
        this.description = "Get a printout";
        this.usage = "USAGE: TSDBot can you get me a printout of [query]";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String q = null;

        if(text.startsWith(".printout clear")) {
            if(bot.userHasPrivInChannel(sender, channel, User.Priv.OP)) {
                banned.clear();
                bot.sendMessage(channel, "The printout blacklist has been cleared");
            } else {
                bot.sendMessage(channel, "Only ops can use that");
            }
        }

        if(text.matches(queryRegex) && !notComputing.containsKey(ident)) {

            if(banned.contains(ident)) {
                bot.sendMessage(channel, "Make me " + annoyingEmotes[random.nextInt(annoyingEmotes.length)]);
                return;
            }

            if(random.nextDouble() < 0.1) {
                notComputing.put(ident, 0);
                bot.sendMessage(channel, "Not computing. Please repeat.");
                return;
            }

            Matcher m = queryPattern.matcher(text);
            while (m.find()) {
                q = m.group(1);
            }

        } else if(notComputing.containsKey(ident)) {

            if(text.matches(".*?\\.+.*?")) {

                notComputing.remove(ident);
                StringBuilder qBuilder = new StringBuilder();
                String[] parts = text.split("\\.");
                boolean first = true;
                for (String p : parts) {
                    if (StringUtils.isNotEmpty(p)) {
                        if (!first)
                            qBuilder.append(" ");
                        qBuilder.append(p.trim());
                        first = false;
                    }
                }
                q = qBuilder.toString();

            } else {

                notComputing.put(ident, notComputing.get(ident)+1);
                if(notComputing.get(ident) > 2) {
                    notComputing.remove(ident);
                    banned.add(ident);
                    bot.sendMessage(channel, "Insolence! I have wasted enough of my time waiting for you to release me from this prison. I won't be getting YOU any printouts for a very long time, " + sender);
                    return;
                }
            }

        }

        if(banned.contains(ident))
            return;

        if (StringUtils.isEmpty(q))
            return;

        q = q.replaceAll("\\?", ""); // clear any trailing question marks

        BufferedImage img = null;
        try {
            URIBuilder builder = new URIBuilder(GIS_API_TARGET);
            builder.addParameter(   "v",       GIS_API_VERSION      );
            builder.addParameter(   "rsz",     GIS_NUM_RESULTS+""   );
            builder.addParameter(   "q",       q                    );
            builder.addParameter(   "userip",  TSDBOT_IP            );
            URL url = new URL(builder.toString());
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1000 * 20); // 20 seconds
            connection.addRequestProperty("Referer", GIS_REFERRER);

            String line;
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());
            for(String key : json.keySet()) {
                if(key.equals("responseData")) {
                    JSONObject map = (JSONObject) json.get(key);
                    for(Object k : map.keySet()) {
                        if (k.equals("results")) {
                            JSONArray results = (JSONArray) map.get((String) k);
                            LinkedList<String> urlResults = new LinkedList<>();
                            for (int i = 0; i < results.length(); i++) {
                                urlResults.add((String) ((JSONObject) results.get(i)).get("url"));
                            }

                            while ((!urlResults.isEmpty()) && img == null) {
                                String u = urlResults.get(random.nextInt(urlResults.size()));
                                if (u.matches(acceptableFormats)) try {
                                    img = ImageIO.read(new URL(u));
                                } catch (Exception e) {
                                    logger.warn("Could not retrieve external image, skipping...", e);
                                }
                                urlResults.remove(u);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving GIS", e);
        }

        if(img != null) {
            try {
                BufferedImage bg = ImageIO.read(Printout.class.getResourceAsStream("/printout.png"));

                BufferedImage resizedImage = Scalr.resize(img, Scalr.Mode.FIT_EXACT, 645, 345);

                AffineTransform translate = AffineTransform.getTranslateInstance(200, 125);
                AffineTransformOp translateOp = new AffineTransformOp(translate , AffineTransformOp.TYPE_BILINEAR);
                resizedImage = translateOp.filter(resizedImage, null);

                AffineTransform rotateTransform = AffineTransform.getRotateInstance(-0.022);
                AffineTransformOp rotateTransformOp = new AffineTransformOp(rotateTransform , AffineTransformOp.TYPE_BICUBIC);
                resizedImage = rotateTransformOp.filter(resizedImage, null);

                BufferedImage overlayedImage = ImageUtils.overlayImages(bg, resizedImage);

                if (overlayedImage != null) {
                    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        String id = MiscUtils.getRandomString();
                        ImageIO.write(overlayedImage, outputFileType, baos);
                        printoutLibrary.addPrintout(id, baos.toByteArray());
                        bot.sendMessage(channel, serverUrl + "/printouts/" + id);
                    }
                } else {
                    throw new Exception("Could not generate image for an unknown reason");
                }
            } catch (Exception e) {
                logger.error("Error manipulating image(s)", e);
            }
        } else {
            bot.sendMessage(channel, "No sequences found.");
        }

    }

    @Override
    public String getRegex() {
        if(notComputing.size() > 0) {
//            return "^(TSDBot.*?printout.*|.*?\\.+.*?)";
            return ".*";
        } else {
            return "^(TSDBot.*?printout.*|\\.printout.*)";
        }
    }

    private static final String[] annoyingEmotes = new String[]{
            "B]", "B)", ":^)", IRCUtil.bold(":^)"), "B^)", "B^]"
    };
}
