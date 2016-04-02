package org.tsd.tsdbot.notifications;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.NotificationType;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.model.dboft.*;
import org.tsd.tsdbot.module.NotifierChannels;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class DboFireteamManager extends NotificationManager<DboFireteamManager.DboftNotification> {

    private static final Logger logger = LoggerFactory.getLogger(DboFireteamManager.class);

    private static final Pattern fireteamIdPattern = Pattern.compile("<td class=\"event-name\">.*?href=\"/ftb/(\\d+)\".*?", Pattern.DOTALL);

    // matches the "subtitle" div if the user DID supply a title -- the event is included in the subtitle
    private static final Pattern withTitlePattern = Pattern.compile("\\((.*?): (Normal|Hard), Level (\\d+)\\)", Pattern.DOTALL);

    // matches the "subtitle" div if the user did NOT supply a title -- the fireteam's title is the event name
    private static final Pattern noTitlePattern = Pattern.compile("\\((Normal|Hard), Level (\\d+)\\)", Pattern.DOTALL);

    private static final Pattern platformPattern = Pattern.compile("<span class=\"data platform (\\w+)\"", Pattern.DOTALL);

    private static final Pattern dataPattern = Pattern.compile("<span class=\"data\">(.*?)</span>", Pattern.DOTALL);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMMM d, yyyy 'at' h:mm a", Locale.US);

    static {
        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        HtmlSanitizer.allowedTags = Pattern.compile("^()$");
        HtmlSanitizer.forbiddenTags = Pattern.compile("^(b|p|i|s|a|img|table|thead|tbody|tfoot|tr|th|td|dd|dl|dt|em|h1|h2|h3|h4|h5|h6|li|ul|ol|span|div|strike|strong|"
                + "sub|sup|pre|del|code|blockquote|strike|kbd|br|hr|area|map|object|embed|param|link|form|small|big|script|object|embed|link|style|form|input)$");
    }

    protected WebClient webClient;
    protected JdbcConnectionProvider connectionProvider;

    @Inject
    public DboFireteamManager(Bot bot,
                              WebClient webClient,
                              JdbcConnectionProvider connectionProvider,
                              @NotifierChannels Map notifierChannels) {
        super(bot, 20, false);
        this.webClient = webClient;
        this.connectionProvider = connectionProvider;
        this.channels = (List<String>) notifierChannels.get("dboft");
    }

    @Override
    public NotificationType getNotificationType() {
        return null;
    }

    @Override
    protected LinkedList<DboftNotification> sweep() {
        JdbcConnectionSource connectionSource = null;
        try {

            logger.debug("Beginning sweep of DBO Fireteams...");
            LinkedList<DboftNotification> notifications = new LinkedList<>();

            connectionSource = connectionProvider.get();
            Dao<Fireteam, Integer> fireteamDao = DaoManager.createDao(connectionSource, Fireteam.class);
            Dao<FireteamRSVP, Integer> rsvpDao = DaoManager.createDao(connectionSource, FireteamRSVP.class);
            Dao<DboUser, Integer> userDao = DaoManager.createDao(connectionSource, DboUser.class);

            HashMap<Fireteam, Boolean> fireteamsInDb = new HashMap<>();
            for(Fireteam fireteam : fireteamDao.query(fireteamDao.queryBuilder().where().eq("deleted", 0).prepare())) {
                logger.debug("Found undeleted fireteam in DB: id={} {}", fireteam.getId(), fireteam.toString());
                fireteamsInDb.put(fireteam, false);
            }

            HtmlPage indexPage = webClient.getPage("http://destiny.bungie.org/forum/index.php?mode=fireteambuilder");
            webClient.waitForBackgroundJavaScript(1000*10);
            String index = indexPage.asXml();

            Matcher indexMatcher = fireteamIdPattern.matcher(index);
            while(indexMatcher.find()) {

                int fireteamId = Integer.valueOf(indexMatcher.group(1));
                logger.debug("***** Found fireteam on page: id={} *****", fireteamId);

                Fireteam fireteam = fireteamDao.queryForId(fireteamId);

                boolean creating = false; //creating or updating
                if(fireteam == null) {
                    logger.debug("Could not find fireteam in database, creating...");
                    fireteam = new Fireteam(fireteamId);
                    creating = true;
                } else {
                    logger.debug("Found fireteam in database!");
                    if(fireteam.isDeleted()) {
                        logger.debug("Fireteam is already deleted in the db, skipping...");
                        continue;
                    } else if(fireteam.getEventTime().before(new Date())) {
                        logger.debug("Fireteam date of {} is before current time, deleting...", fireteam.getEventTime());
                        fireteam.setDeleted(true);
                        fireteamsInDb.remove(fireteam);
                        fireteamDao.update(fireteam);
                        if(fireteam.isSubscribed()) {
                            FireteamNotification completed =
                                    new FireteamNotification(fireteam, DboftNotificationType.COMPLETE, null);
                            notifications.add(completed);
                        }
                        continue;
                    } else {
                        fireteamsInDb.put(fireteam, true);
                    }
                }

                HtmlPage fireteamPage = webClient.getPage("http://destiny.bungie.org/forum/index.php?mode=fireteambuilder&event=" + fireteamId);

                String activity = null;
                Difficulty difficulty = null;
                Integer level = null;
                String title = null;

                HtmlSpan titleElement = (HtmlSpan) fireteamPage.getByXPath("//span[@class='title']").get(0);

                HtmlSpan subtitleElement = null;
                List<HtmlSpan> spans = (List<HtmlSpan>) fireteamPage.getByXPath("//span[@class='subtitle']");
                if(spans.size() > 0) {
                    subtitleElement = spans.get(0);
                    Matcher noTitleMatcher = noTitlePattern.matcher(subtitleElement.asText());
                    if(noTitleMatcher.matches()) {
                        while(noTitleMatcher.find()) {
                            difficulty = Difficulty.fromString(noTitleMatcher.group(1).trim());
                            level = Integer.parseInt(noTitleMatcher.group(2).trim());
                            title = titleElement.asText().trim();
                            activity = title;
                        }
                    } else {
                        Matcher withTitleMatcher = withTitlePattern.matcher(subtitleElement.asText());
                        while(withTitleMatcher.find()) {
                            title = titleElement.asText().trim();
                            activity = withTitleMatcher.group(1).trim();
                            difficulty = Difficulty.fromString(withTitleMatcher.group(2).trim());
                            level = Integer.parseInt(withTitleMatcher.group(3).trim());
                        }
                    }
                } else {
                    title = titleElement.asText().trim();
                }

                Platform platform = null;
                HtmlDivision platformDiv = (HtmlDivision) fireteamPage.getByXPath("//div[@id='event-platform']").get(0);
                for(DomElement de : platformDiv.getChildElements()) {
                    Matcher platformMatcher = platformPattern.matcher(de.asXml());
                    while(platformMatcher.find() && platform == null) {
                        platform = Platform.fromString(platformMatcher.group(1).trim());
                    }
                }

                Date date = null;
                HtmlDivision dateDiv = (HtmlDivision) fireteamPage.getByXPath("//div[@id='event-time']").get(0);
                for(DomElement de : dateDiv.getChildElements()) {
                    Matcher dateMatcher = dataPattern.matcher(de.asXml());
                    while(dateMatcher.find() && date == null) {
                        date = sdf.parse(dateMatcher.group(1).trim().replaceAll("\\s+", " "));
                    }
                }

                DboUser creator = null;
                HtmlDivision creatorDiv = (HtmlDivision) fireteamPage.getByXPath("//div[@id='event-creator']").get(0);
                for(DomElement de : creatorDiv.getChildElements()) {
                    Matcher creatorMatcher = dataPattern.matcher(de.asXml());
                    while(creatorMatcher.find() && creator == null) {
                        creator = getUserFromDiv(userDao, creatorMatcher.group(1).trim());
                    }
                }

                if(creator == null) {
                    logger.warn("Could not parse creator for fireteam {}, skipping...", fireteamId);
                    continue;
                }

                String description = null;
                List descriptionDivs = fireteamPage.getByXPath("//div[@id='event-description']");
                if(descriptionDivs != null && descriptionDivs.size() > 0) {
                    HtmlDivision descriptionDiv = (HtmlDivision) descriptionDivs.get(0);
                    for (DomElement de : descriptionDiv.getChildElements()) {
                        Matcher descriptionMatcher = dataPattern.matcher(de.asXml());
                        while (descriptionMatcher.find() && description == null) {
                            description = HtmlSanitizer.sanitize(descriptionMatcher.group(1).trim());
                        }
                    }
                }

                // we now have all the fireteam data. analyze the RSVPs

                Set<FireteamRSVP> rsvpsOnSite = parseRSVPs(fireteam, (HtmlTable) fireteamPage.getByXPath("//table[@id='rsvp-list']").get(0), userDao);
                logger.debug("Found {} RSVPs on DBO", rsvpsOnSite.size());

                List<String[]> fireteamChanges = new LinkedList<>();
                HashSet<FireteamRSVP> deletedRsvps = new HashSet<>();
                HashSet<FireteamRSVP> addedRsvps = new HashSet<>();
                HashSet<FireteamRSVP> updatedRsvps = new HashSet<>();

                if(!creating) {

                    // this fireteam already exists, update info and notify of changes

                    String[] ftChange = diffFields("Platform", fireteam.getPlatform(), platform);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    ftChange = diffFields("Activity", fireteam.getActivity(), activity);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    ftChange = diffFields("Difficulty", fireteam.getDifficulty(), difficulty);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    ftChange = diffFields("Level", fireteam.getLevel(), level);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    ftChange = diffFields("Title", fireteam.getTitle(), title);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    ftChange = diffFields("Event Time", fireteam.getEventTime(), date);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    ftChange = diffFields("Description", fireteam.getDescription(), description);
                    if(ftChange != null) fireteamChanges.add(ftChange);

                    // analyze changes to RSVPs

                    for(FireteamRSVP rsvpInDb : fireteam.getRsvps()) {

                        logger.debug("Analyzing RSVP in database: {}", rsvpInDb.toString());

                        if(rsvpsOnSite.contains(rsvpInDb)) {

                            logger.debug("Matched this RSVP with one on the site");
                            // in DB and on site -- updated
                            // get the one on the site
                            FireteamRSVP onSite = null;
                            for(FireteamRSVP r : rsvpsOnSite) {
                                if(r.equals(rsvpInDb)) {
                                    onSite = r;
                                    onSite.setId(rsvpInDb.getId());
                                    break;
                                }
                            }

                            List<String[]> changes = new LinkedList<>();

                            String[] charClassChg = diffFields("Class", rsvpInDb.getCharacterClass(), onSite.getCharacterClass());
                            if(charClassChg != null)
                                changes.add(charClassChg);

                            String[] levelChg = diffFields("Level", rsvpInDb.getLevel(), onSite.getLevel());
                            if(levelChg != null)
                                changes.add(levelChg);

                            String[] commentChg = diffFields("Comment", rsvpInDb.getComment(), onSite.getComment());
                            if(commentChg != null)
                                changes.add(commentChg);

                            String[] tentativeChg = diffFields("Tentative", rsvpInDb.isTentative(), onSite.isTentative());
                            if(tentativeChg != null)
                                changes.add(tentativeChg);

                            logger.debug("Recorded {} changes for RSVP {}", changes.size(), onSite.toString());
                            updatedRsvps.add(onSite);
                            if(changes.size() > 0 && fireteam.isSubscribed()) {
                                RSVPNotification notification = new RSVPNotification(fireteam, DboftNotificationType.UPDATE, changes, onSite);
                                notifications.add(notification);
                            }
                            rsvpsOnSite.remove(rsvpInDb);

                        } else {
                            // in DB but no longer on site -- deleted
                            logger.debug("Could not find this RSVP on the site, marking for deletion...");
                            deletedRsvps.add(rsvpInDb);
                        }
                    }

                    // rsvpsOnSite now only contains new RSVPs
                    for(FireteamRSVP addingRsvp : rsvpsOnSite) {
                        logger.debug("Marking RSVP for addition: {}", addingRsvp.toString());
                        addedRsvps.add(addingRsvp);
                    }

                }

                fireteam.setCreator(creator);
                fireteam.setPlatform(platform);
                fireteam.setActivity(activity);
                fireteam.setDifficulty(difficulty);
                fireteam.setLevel(level);
                fireteam.setTitle(title);
                fireteam.setEventTime(date);
                fireteam.setDescription(description);

                if(!creating) {
                    logger.debug("Updating fireteam {}...", fireteam.toString());
                    for(FireteamRSVP updatedRsvp : updatedRsvps) {
                        logger.debug("Updating RSVP {}...", updatedRsvp.toString());
                        rsvpDao.update(updatedRsvp);
                    }
                    for(FireteamRSVP addedRsvp : addedRsvps) {
                        logger.debug("Creating RSVP {}...", addedRsvp.toString());
                        rsvpDao.create(addedRsvp);
                        if(fireteam.isSubscribed()) {
                            RSVPNotification notification = new RSVPNotification(fireteam, DboftNotificationType.CREATE, null, addedRsvp);
                            notifications.add(notification);
                        }
                        if(!fireteam.getRsvps().contains(addedRsvp))
                            fireteam.getRsvps().add(addedRsvp);
                    }
                    for(FireteamRSVP deletedRsvp : deletedRsvps) {
                        logger.debug("Deleting RSVP {}...", deletedRsvp.toString());
                        rsvpDao.delete(deletedRsvp);
                        if(fireteam.isSubscribed()) {
                            RSVPNotification notification = new RSVPNotification(fireteam, DboftNotificationType.DELETE, null, deletedRsvp);
                            notifications.add(notification);
                        }
                        if(fireteam.getRsvps().contains(deletedRsvp))
                            fireteam.getRsvps().remove(deletedRsvp);
                    }
                    fireteamDao.update(fireteam);

                    if(fireteamChanges.size() > 0 && fireteam.isSubscribed()) {
                        FireteamNotification notification = new FireteamNotification(fireteam, DboftNotificationType.UPDATE, fireteamChanges);
                        notifications.add(notification);
                    }

                } else {
                    logger.debug("Creating fireteam {}...", fireteam.toString());
                    fireteamDao.create(fireteam);
                    for(FireteamRSVP rsvp : rsvpsOnSite) {
                        logger.debug("Creating RSVP {}...", rsvp.toString());
                        rsvpDao.create(rsvp);
                    }
                    FireteamNotification notification = new FireteamNotification(fireteam, DboftNotificationType.CREATE, null);
                    notifications.add(notification); // always notify on new fireteams
                }
            }

            for(Fireteam fireteam : fireteamsInDb.keySet()) {
                if(!fireteamsInDb.get(fireteam)) {
                    logger.debug("Deleting fireteam {}...", fireteam.toString());
                    fireteam.setDeleted(true);
                    fireteamDao.update(fireteam);

                    if(fireteam.isSubscribed()) {
                        FireteamNotification notification = new FireteamNotification(fireteam, DboftNotificationType.DELETE, null);
                        notifications.add(notification);
                    }
                }
            }

            logger.debug("Fireteam sweep ended normally, compiled {} notifications", notifications.size());

            return notifications;

        } catch (Exception e) {
            logger.error("Unhandled error sweeping fireteams", e);
        } finally {
            if(connectionSource != null && connectionSource.isOpen())
                connectionSource.closeQuietly();
        }

        return new LinkedList<>();
    }

    private DboUser getUserFromDiv(Dao<DboUser, Integer> userDao, String html) throws SQLException {
        Pattern creatorPattern = Pattern.compile("<a href=\"index\\.php\\?mode=user&amp;show_user=(\\d+)\".*?>(.*?)</a>", Pattern.DOTALL);
        Matcher creatorMatcher = creatorPattern.matcher(html);
        while(creatorMatcher.find()) {
            int userId = Integer.parseInt(creatorMatcher.group(1).trim());
            String handle = creatorMatcher.group(2).trim();
            logger.debug("Analyzing user on site, id={} handle={}", userId, handle);
            DboUser foundUser = userDao.queryForId(userId);
            if(foundUser == null) {
                logger.debug("Could not find user in DB, creating...");
                foundUser = new DboUser(userId, handle);
                userDao.create(foundUser);
            } else {
                logger.debug("Found user in DB, updating...");
                foundUser.setHandle(handle);
                userDao.update(foundUser);
            }
            return foundUser;
        }
        logger.warn("Couldn't parse any user info from the site");
        return null;
    }

    private Set<FireteamRSVP> parseRSVPs(Fireteam fireteam, HtmlTable rsvpsTable, Dao<DboUser, Integer> userDao) throws SQLException {

        logger.debug("Parsing RSVPs on site for fireteam {}...", fireteam.getId());

        Set<FireteamRSVP> rsvps = new HashSet<>();

        String gamertag = null;
        CharacterClass characterClass = null;
        Integer level = null;
        String comment = null;
        boolean tentative = false;
        DboUser creator;

        for(HtmlTableRow row : rsvpsTable.getRows()) {

            HtmlTableCell cell = row.getCell(0);
            if(StringUtils.isEmpty(row.getCell(0).asText())) // reached the end
                break;
            gamertag = cell.asText().trim();
            logger.debug("gamertag: {}", gamertag);

            cell = row.getCell(1);
            if(StringUtils.isNotEmpty(cell.asText())) {
                characterClass = CharacterClass.fromString(cell.asText().trim());
            }
            logger.debug("characterClass: {}", characterClass);

            cell = row.getCell(2);
            if(StringUtils.isNotEmpty(cell.asText())) {
                level = Integer.parseInt(cell.asText().trim());
            }
            logger.debug("level: {}", level);

            cell = row.getCell(3);
            if(StringUtils.isNotEmpty(cell.asText())) {
                comment = cell.asText().replace("&ldquo;", "").replace("&rdquo;", "").trim();
            }
            logger.debug("comment: {}", comment);

            cell = row.getCell(4);
            if(StringUtils.isNotEmpty(cell.asText())) {
                tentative = true;
            }
            logger.debug("tentative: {}", tentative);

            cell = row.getCell(6);
            creator = getUserFromDiv(userDao, cell.asXml());

            FireteamRSVP rsvp = new FireteamRSVP(fireteam, creator, gamertag);
            rsvp.setCharacterClass(characterClass);
            rsvp.setLevel(level);
            rsvp.setComment(comment);
            rsvp.setTentative(tentative);

            rsvps.add(rsvp);

        }

        return rsvps;

    }

    private String[] diffFields(String fieldName, Object val1, Object val2) {
        logger.debug("diffFields {}: {} -> {}", new Object[]{fieldName, val1, val2});
        if(val1 == null && val2 == null) {
            return null;
        } else {
            if(val1 == null)
                return new String[]{fieldName, "(empty)", val2.toString()};
            else if(val2 == null)
                return new String[]{fieldName, val1.toString(), "(empty)"};
            else if(val1.equals(val2))
                return null;
            else
                return new String[]{fieldName, val1.toString(), val2.toString()};
        }
    }

    public abstract class DboftNotification extends NotificationEntity {

        protected Fireteam fireteam;
        protected DboftNotificationType notificationType;
        protected List<String[]> changes;

        protected DboftNotification(Fireteam fireteam, DboftNotificationType notificationType, List<String[]> changes) {
            this.fireteam = fireteam;
            this.notificationType = notificationType;
            this.changes = changes;
        }

        @Override
        public String getPreview() {
            return null;
        }

        @Override
        public String[] getFullText() {
            return new String[0];
        }

        @Override
        public String getKey() {
            return null;
        }

    }

    class FireteamNotification extends DboftNotification {

        protected FireteamNotification(Fireteam fireteam, DboftNotificationType notificationType, List<String[]> changes) {
            super(fireteam, notificationType, changes);
        }

        @Override
        public String getInline() {
            StringBuilder sb = new StringBuilder();
            sb.append("[DBOFT] ");
            switch(notificationType) {
                case CREATE: {
                    sb.append("[New Fireteam] ").append(fireteam.toString())
                            .append(" -- ").append(IRCUtil.shortenUrl(fireteam.getUrl()));
                    break;
                }
                case UPDATE: {
                    sb.append("[Fireteam update] ").append(fireteam.toBriefString());
                    for(String[] change : changes)
                        sb.append(" || ").append(change[0]).append(": ")
                                .append(change[1]).append(" -> ").append(change[2]);
                    break;
                }
                case DELETE: {
                    sb.append("[Fireteam DELETED] ").append(fireteam.toBriefString());
                    break;
                }
                case COMPLETE: {
                    sb.append("[Event starting] ").append(fireteam.toString());
                    break;
                }
            }
            return sb.toString();
        }

    }

    class RSVPNotification extends DboftNotification {

        protected FireteamRSVP rsvp;

        protected RSVPNotification(Fireteam fireteam, DboftNotificationType notificationType, List<String[]> changes, FireteamRSVP rsvp) {
            super(fireteam, notificationType, changes);
            this.rsvp = rsvp;
        }

        @Override
        public String getInline() {
            StringBuilder sb = new StringBuilder();
            sb.append("[DBOFT] ");
            switch(notificationType) {
                case CREATE: {
                    sb.append("[New RSVP] ").append(fireteam.getEffectiveTitle()).append(" || ")
                            .append(rsvp.toString()).append(" -- ").append(IRCUtil.shortenUrl(fireteam.getUrl()));
                    break;
                }
                case UPDATE: {
                    sb.append("[RSVP changed] ").append(fireteam.getEffectiveTitle()).append(", ").append(rsvp.getGamertag());
                    for(String[] change : changes)
                        sb.append(" || ").append(change[0]).append(": ")
                                .append(change[1]).append(" -> ").append(change[2]);
                    sb.append(" -- ").append(IRCUtil.shortenUrl(fireteam.getUrl()));
                    break;
                }
                case DELETE: {
                    sb.append("[RSVP deleted] ").append(fireteam.getEffectiveTitle()).append(" || ")
                            .append(rsvp.toString()).append(" -- ").append(IRCUtil.shortenUrl(fireteam.getUrl()));
                    break;
                }
                case COMPLETE: {
                    throw new IllegalStateException("An RSVP can't be completed");
                }
            }
            return sb.toString();
        }
    }

    enum DboftNotificationType {
        CREATE,
        UPDATE,
        COMPLETE,
        DELETE
    }


}
