package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.util.DatabaseLogic;
import org.tsd.tsdbot.util.IRCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
@Function(initialRegex = "^\\.tc.*")
public class TomCruise extends MainFunctionImpl implements Persistable {

    private static Logger logger = LoggerFactory.getLogger(TomCruise.class);

    private DBConnectionProvider connectionProvider;
    private Random random;

    @Inject
    public TomCruise(Bot bot, DBConnectionProvider connectionProvider, Random random) throws SQLException {
        super(bot);
        this.description = "Generate a random Tom Cruise clip or quote";
        this.usage = "USAGE: .tc [ clip | quote ]";
        this.connectionProvider = connectionProvider;
        this.random = random;
        initDB();
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");
        TomCruiseItemType itemType = null;

        if(cmdParts.length == 1) {
            itemType = null;
        } else if(cmdParts[1].equals("quote")) {
            itemType = TomCruiseItemType.quote;
        } else if(cmdParts[1].equals("clip")) {
            itemType = TomCruiseItemType.clip;
        } else {
            bot.sendMessage(channel, usage);
            return;
        }

        bot.sendMessages(channel, IRCUtil.splitLongString(getRandom(itemType)));
    }

    public String getRandom(TomCruiseItemType itemType) {

        if(itemType == null) {
            itemType = (random.nextBoolean()) ? TomCruiseItemType.clip : TomCruiseItemType.quote;
        }

        Connection conn = connectionProvider.get();

        String sql = "select data from TOMCRUISE where type = '%s' order by rand() limit 1";
        sql = String.format(sql, itemType.dbEnum);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Maybe return something from an emergency collection defined in this class
    }

    @Override
    public void initDB() throws SQLException {
        logger.info("Initializing Tom Cruise database...");
        String tomCruiseTable = "TOMCRUISE";
        Connection connection = connectionProvider.get();

        if(!DatabaseLogic.tableExists(connection, tomCruiseTable)) {

            logger.info("Table {} does NOT exist, creating...", tomCruiseTable);

            String create = String.format("create table if not exists %s (" +
                    "id int auto_increment," +
                    "type varchar," +
                    "data clob," +
                    "primary key (id))", tomCruiseTable);

            try(PreparedStatement ps = connection.prepareStatement(create)) {
                logger.info("TOMCRUISE: {}", create);
                ps.executeUpdate();
            }

            StringBuilder insertBuilder = new StringBuilder();
            insertBuilder.append(String.format("insert into %s (type, data) values ",tomCruiseTable));
            boolean first = true;
            for(String quote : quotes) {
                if(!first) insertBuilder.append(",");
                insertBuilder.append(String.format("('quote','%s')",quote));
                first = false;
            }
            for(String quote : clips) {
                if(!first) insertBuilder.append(",");
                insertBuilder.append(String.format("('clip','%s')",quote));
                first = false;
            }

            try(PreparedStatement ps = connection.prepareCall(insertBuilder.toString())) {
                ps.executeUpdate();
            }

            connection.commit();
        }
    }

    @Override
    public void initDB2(JdbcConnectionSource connectionSource) {

    }

    // stuff below is used for database seeding

    private static final String[] quotes = new String[] {
            "Get with it. Millions of galaxies of hundreds of millions of stars, in a speck on one in a blink. That’s " +
                    "us, lost in space. The cop, you, me… Who notices?  ",
            "You want me to kill Jappos, I''ll kill Jappos. You want me to kill THE ENEMIES of Jappos, I''ll kill " +
                    "THE ENEMIES of Jappos... Rebs, or Sioux, or Cheyenne... For 500 bucks a month I''ll kill whoever " +
                    "you want. But keep one thing in mind: I''d happily kill you for free.  ",
            "THATSRIGHT, Ice... man. I am dangerous. *chomp*  ",
            "Even in my dreams, I’m an idiot who knows he’s about to wake up to reality.  ",
            "Show me the money!  ",
            "I am out here for you. You don’t know what it’s like to be ME out here for YOU. It is an up-at-dawn, " +
                    "pride-swallowing siege that I will never fully tell you about, ok?  ",
            "That’s more than a dress. That’s an Audrey Hepburn movie.  ",
            "I will not rest until I have you holding a Coke, wearing your own shoe, playing a Sega game featuring you, " +
                    "while singing your own song in a new commercial, starring you, broadcast during the Superbowl, in " +
                    "a game that you are winning, and I will not sleep until that happens. I’ll give you fifteen minutes" +
                    " to call me back.  ",
            "Twenty-four hours ago, man, I was hot! Now… I’m a cautionary tale. You see this jacket I’m wearing, you " +
                    "like it? Because I don’t really need it. Because I’m cloaked in failure! I lost the number one " +
                    "draft picked the night before the draft! Why? Let’s recap: Because a hockey player’s kid made me " +
                    "feel like a superficial jerk. I ate two slices of bad pizza, went to bed and grew a conscience! " +
                    " ",
            "You... complete me.  ",
            "Evildoers are easier, and they taste better.  ",
            "I''m sorry ma''am, I lied to you. I’m very sorry about that. That man right there is my brother and if he " +
                    "doesn’t get to watch ’People’s Court’ in about 30 seconds, he’s gonna throw a fit right here on your " +
                    "porch. Now you can help me or you can stand there and watch it happen.  ",
            "I feel the need -- the need for speed. Ow!  ",
            "You don''t have time to think up there. If you think, you''re dead.  ",
            "This is what I call a target-rich environment.  ",
            "It seems to me that if there were any logic to our language, trust would be a four letter word.  ",
            "Porsche. There is no substitute.  ",
            "I''ve got a trig midterm tomorrow and I''m being chased by Guido the Killer Pimp.  ",
            "All right, tell me what''s going on, tell me what''s going on. I''m BLIND, goddammit!  ",
            "We just rolled up a snowball and threw it into Hell. Now we''ll see if it has a chance.  ",
            "I am the last barman poet / I see America drinking the fabulous cocktails I make / Americans getting " +
                    "stinky on something I stir or shake.  ",
            "Talk to me, Goose.  ",
            "That son of a bitch cut me off!  ",
            "Any of you boys seen an aircraft carrier around here?  ",
            "Jesus Christ, and you think I''m reckless? When I fly, I''ll have you know that my crew and my plane come first!  ",
            "Just want to serve my country, be the best pilot in the Navy, sir.  ",
            "No, actually I had this counter in mind.  ",
            "Too close for missiles, I''m switching to guns.  ",
            "I can see it''s dangerous for you, but if the government trusts me, maybe you could. *smile*  ",
            "There was once a battle at a place called Thermopylae, where three hundred brave Greeks held off a " +
                    "Persian army of a million men... a million, you understand this number?  ",
            "Spring, 1877. This marks the longest I''ve stayed in one place since I left the farm at 17. There is so " +
                    "much here I will never understand. I''ve never been a church going man, and what I''ve seen on the " +
                    "field of battle has led me to question God''s purpose. But there is indeed something spiritual in " +
                    "this place. And though it may forever be obscure to me, I cannot but be aware of its power. I do " +
                    "know that it is here that I''ve known my first untroubled sleep in many years.  ",
            "You have no idea what I have done.  ",
            "I know why you don''t talk. Because you''re angry. You''re angry because they make you wear a dress.  ",
            "Okay, look, here''s the deal. Man, you were gonna drive me around tonight, never be the wiser, but El " +
                    "Gordo got in front of a window, did his high dive, we''re into Plan B. Still breathing? Now we " +
                    "gotta make the best of it, improvise, adapt to the environment, Darwin, shit happens, I Ching, " +
                    "whatever man, we gotta roll with it.  ",
            "Max, six billion people on the planet, you''re getting bent out of shape cause of one fat guy.  ",
            "Someday? Someday my dream will come? One night you will wake up and discover it never happened. It''s " +
                    "all turned around on you. It never will. Suddenly you are old. Didn''t happen, and it never will, " +
                    "because you were never going to do it anyway. You''ll push it into memory and then zone out in " +
                    "your barco lounger, being hypnotized by daytime TV for the rest of your life. Don''t you talk to " +
                    "me about murder. All it ever took was a down payment on a Lincoln town car. That girl, you " +
                    "can''t even call that girl. What the fuck are you still doing driving a cab?  "
    };

    private static final String[] clips = new String[] {
            "https://www.youtube.com/watch?v=7yP9MmzyTIg",
            "https://www.youtube.com/watch?v=LMT1r9IpIf0",
            "https://www.youtube.com/watch?v=B9XyCwmh-5Q",
            "https://www.youtube.com/watch?v=QE3yMEfpk6E",
            "https://www.youtube.com/watch?v=GhT2XYQ5Yj0",
            "https://www.youtube.com/watch?v=rZ855676Hvo",
            "https://www.youtube.com/watch?v=ISfFg4zHImQ",
            "https://www.youtube.com/watch?v=IejpzbxKUZE",
            "https://www.youtube.com/watch?v=nf7_eayBkxA",
            "https://www.youtube.com/watch?v=XO4f-JqXG7g"
    };

    private static enum TomCruiseItemType {
        clip    ("clip"),
        quote   ("quote");

        public String dbEnum;

        TomCruiseItemType(String dbEnum) {
            this.dbEnum = dbEnum;
        }
    }
}
