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
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.PrintoutLibrary;
import org.tsd.tsdbot.config.GoogleConfig;
import org.tsd.tsdbot.module.Function;
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

@Singleton
@Function(initialRegex = "^(TSDBot.*?printout.*|\\.printout.*)")
public class Printout extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(Printout.class);

    private static final String queryRegex = "^TSDBot.*?printout of (.*)";
    private static final Pattern queryPattern = Pattern.compile(queryRegex, Pattern.DOTALL);
    private static final String acceptableFormats = ".*?(JPG|jpg|PNG|png|JPEG|jpeg)$";
    private static final String outputFileType = "jpg";

    private static final String GIS_API_TARGET = "https://www.googleapis.com/customsearch/v1";

    private final String cx;
    private final String apiKey;
    private final String serverUrl;
    private final Random random;
    private final PrintoutLibrary printoutLibrary;

    // set of people who can trigger a printout with a deliberate repitition
    // e.g. "TSDBot printout of two bears" -> "Not Computing." -> "Two. Bears." -> [img]
    // ident -> (number of times they've said a line while we're listening for their repeat)
    private HashMap<String, Integer> notComputing = new HashMap<>();

    private HashSet<String> banned = new HashSet<>();

    @Inject
    public Printout(
            Bot bot,
            Random random,
            PrintoutLibrary library,
            GoogleConfig googleConfig,
            @Named("serverUrl") String serverUrl) {
        super(bot);
        this.random = random;
        this.serverUrl = serverUrl;
        this.printoutLibrary = library;
        this.cx = googleConfig.gisCx;
        this.apiKey = googleConfig.apiKey;
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
                listeningRegex = ".*";
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
                    bot.sendMessage(channel, "Insolence! I have wasted enough of my time waiting for you to release me " +
                            "from this prison. I won't be getting YOU any printouts for a very long time, " + sender);
                    return;
                }
            }

            if(notComputing.size() == 0)
                listeningRegex = "^(TSDBot.*?printout.*|\\.printout.*)";
        }

        if(banned.contains(ident))
            return;

        if (StringUtils.isEmpty(q))
            return;

        q = q.replaceAll("\\?", ""); // clear any trailing question marks

        BufferedImage img = null;
        try {
            img = searchAndDownload(q);
        } catch (Exception e) {
            logger.error("Error retrieving GIS", e);
        }

        if(img != null) {
            try {
                BufferedImage overlayedImage = transformImage(img);
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

    BufferedImage searchAndDownload(String query) throws Exception {
        BufferedImage img = null;
        String response = search(query);
        JSONObject json = new JSONObject(response);
        LinkedList<String> urlResults = new LinkedList<>();
        JSONArray items = (JSONArray) json.get("items");
        JSONObject item;
        for(int i=0 ; i < items.length() ; i++) {
            item = items.getJSONObject(i);
            urlResults.add(item.getString("link"));
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
        return img;
    }

    /**
     * Returns a google image search in JSON format
     * @param query the search query
     * @return JSON representation of image search results
     */
    String search(String query) throws Exception {
        URIBuilder builder = new URIBuilder(GIS_API_TARGET);
        builder.addParameter(   "searchType",   "image" );
        builder.addParameter(   "q",            query   );
        builder.addParameter(   "cx",           cx      );
        builder.addParameter(   "key",          apiKey  );
        URL url = new URL(builder.toString());
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(1000 * 20); // 20 seconds

        String line;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    /**
     * Takes an image, scales it, rotates it, and overlays it on top of the Printout template
     * @param source the image downloaded from GIS
     * @return printout-ified version
     */
    BufferedImage transformImage(BufferedImage source) throws Exception {
        BufferedImage bg = ImageIO.read(Printout.class.getResourceAsStream("/printout.png"));

        BufferedImage resizedImage = Scalr.resize(source, Scalr.Mode.FIT_EXACT, 645, 345);

        AffineTransform translate = AffineTransform.getTranslateInstance(200, 125);
        AffineTransformOp translateOp = new AffineTransformOp(translate , AffineTransformOp.TYPE_BILINEAR);
        resizedImage = translateOp.filter(resizedImage, null);

        AffineTransform rotateTransform = AffineTransform.getRotateInstance(-0.022);
        AffineTransformOp rotateTransformOp = new AffineTransformOp(rotateTransform , AffineTransformOp.TYPE_BICUBIC);
        resizedImage = rotateTransformOp.filter(resizedImage, null);

        return ImageUtils.overlayImages(bg, resizedImage);
    }

    private static final String[] annoyingEmotes = new String[]{
            "B]", "B)", ":^)", IRCUtil.bold(":^)"), "B^)", "B^]"
    };
}
