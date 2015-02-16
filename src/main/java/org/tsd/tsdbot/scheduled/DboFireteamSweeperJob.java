package org.tsd.tsdbot.scheduled;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Stage;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.model.dboft.*;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/7/2015.
 */
public class DboFireteamSweeperJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(DboFireteamSweeperJob.class);

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

    @Inject
    protected TSDBot bot;

    @Inject
    protected WebClient webClient;

    @Inject
    protected JdbcConnectionProvider connectionProvider;

    @Inject
    protected Stage stage;

    private String destinyChannel;
    private Dao<Fireteam, Integer> fireteamDao;
    private Dao<FireteamRSVP, Integer> rsvpDao;
    private Dao<DboUser, Integer> userDao;

    // fireteam -> seen in fetch
    private HashMap<Fireteam, Boolean> fireteamsInDb = new HashMap<>();

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        this.destinyChannel = (this.stage.equals(Stage.dev)) ? "#tsdbot" : "#tsd";
        JdbcConnectionSource connectionSource = null;
        try {

            logger.info("Beginning sweep of DBO Fireteams...");

            connectionSource = connectionProvider.get();
            fireteamDao = DaoManager.createDao(connectionSource, Fireteam.class);
            rsvpDao = DaoManager.createDao(connectionSource, FireteamRSVP.class);
            userDao = DaoManager.createDao(connectionSource, DboUser.class);

            for(Fireteam fireteam : fireteamDao.query(fireteamDao.queryBuilder().where().eq("deleted", 0).prepare())) {
                logger.info("Found undeleted fireteam in DB: id={} {}", fireteam.getId(), fireteam.toString());
                fireteamsInDb.put(fireteam, false);
            }

            HtmlPage indexPage = webClient.getPage("http://destiny.bungie.org/forum/index.php?mode=fireteambuilder");
            webClient.waitForBackgroundJavaScript(1000*10);
            String index = indexPage.asXml();

            Matcher indexMatcher = fireteamIdPattern.matcher(index);
            while(indexMatcher.find()) {

                int fireteamId = Integer.valueOf(indexMatcher.group(1));
                logger.info("***** Found fireteam on page: id={} *****", fireteamId);

                Fireteam fireteam = fireteamDao.queryForId(fireteamId);

                boolean creating = false; //creating or updating
                if(fireteam == null) {
                    logger.info("Could not find fireteam in database, creating...");
                    fireteam = new Fireteam(fireteamId);
                    creating = true;
                } else {
                    logger.info("Found fireteam in database!");
                    if(fireteam.isDeleted()) {
                        logger.info("Fireteam is already deleted in the db, skipping...");
                        continue;
                    } else if(fireteam.getEventTime().before(new Date())) {
                        logger.info("Fireteam date of {} is before current time, deleting...", fireteam.getEventTime());
                        fireteam.setDeleted(true);
                        fireteamsInDb.remove(fireteam);
                        fireteamDao.update(fireteam);
                        String shortUrl = IRCUtil.shortenUrl(fireteam.getUrl());
                        bot.sendMessage(destinyChannel, "[DBOFT] [EVENT STARTING] " + fireteam.toString() + " -- " + shortUrl);
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
                        creator = getUserFromDiv(creatorMatcher.group(1).trim());
                    }
                }

                if(creator == null) {
                    logger.warn("Could not parse creator for fireteam {}, skipping...", fireteamId);
                    continue;
                }

                String description = null;
                HtmlDivision descriptionDiv = (HtmlDivision) fireteamPage.getByXPath("//div[@id='event-description']").get(0);
                for(DomElement de : descriptionDiv.getChildElements()) {
                    Matcher descriptionMatcher = dataPattern.matcher(de.asXml());
                    while(descriptionMatcher.find() && description == null) {
                        description = HtmlSanitizer.sanitize(descriptionMatcher.group(1).trim());
                    }
                }

                // we now have all the fireteam data. analyze the RSVPs

                Set<FireteamRSVP> rsvpsOnSite = parseRSVPs(fireteam, (HtmlTable) fireteamPage.getByXPath("//table[@id='rsvp-list']").get(0));
                logger.info("Found {} RSVPs on DBO", rsvpsOnSite.size());

                List<String[]> fireteamChanges = new LinkedList<>();
                HashSet<FireteamRSVP> deletedRsvps = new HashSet<>();
                HashSet<FireteamRSVP> addedRsvps = new HashSet<>();

                // fireteam -> list<change> | change=String[0:field, 1:oldValue, 2:newValue]
                HashMap<FireteamRSVP, List<String[]>> updatedRsvps = new HashMap<>();

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

                        logger.info("Analyzing RSVP in database: {}", rsvpInDb.toString());

                        if(rsvpsOnSite.contains(rsvpInDb)) {

                            logger.info("Matched this RSVP with one on the site");
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

                            logger.info("Recorded {} changes for RSVP {}", changes.size(), onSite.toString());
                            updatedRsvps.put(onSite, changes);
                            rsvpsOnSite.remove(rsvpInDb);

                        } else {
                            // in DB but no longer on site -- deleted
                            logger.info("Could not find this RSVP on the site, marking for deletion...");
                            deletedRsvps.add(rsvpInDb);
                        }
                    }

                    // rsvpsOnSite now only contains new RSVPs
                    for(FireteamRSVP addingRsvp : rsvpsOnSite) {
                        logger.info("Marking RSVP for addition: {}", addingRsvp.toString());
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
                    logger.info("Updating fireteam {}...", fireteam.toString());
//                    fireteamDao.update(fireteam);
                    for(FireteamRSVP updatedRsvp : updatedRsvps.keySet()) {
                        logger.info("Updating RSVP {}...", updatedRsvp.toString());
                        rsvpDao.update(updatedRsvp);
                    }
                    for(FireteamRSVP addedRsvp : addedRsvps) {
                        logger.info("Creating RSVP {}...", addedRsvp.toString());
                        rsvpDao.create(addedRsvp);
                        if(!fireteam.getRsvps().contains(addedRsvp))
                            fireteam.getRsvps().add(addedRsvp);
                    }
                    for(FireteamRSVP deletedRsvp : deletedRsvps) {
                        logger.info("Deleting RSVP {}...", deletedRsvp.toString());
                        rsvpDao.delete(deletedRsvp);
                        if(fireteam.getRsvps().contains(deletedRsvp))
                            fireteam.getRsvps().remove(deletedRsvp);
                    }
                    fireteamDao.update(fireteam);
                    notifyFireteamChanges(fireteam, fireteamChanges, updatedRsvps, addedRsvps, deletedRsvps);
                } else {
                    logger.info("Creating fireteam {}...", fireteam.toString());
                    fireteamDao.create(fireteam);
                    for(FireteamRSVP rsvp : rsvpsOnSite) {
                        logger.info("Creating RSVP {}...", rsvp.toString());
                        rsvpDao.create(rsvp);
                    }
                    notifyNewFireteam(fireteam);
                }
            }

            for(Fireteam fireteam : fireteamsInDb.keySet()) {
                if(!fireteamsInDb.get(fireteam)) {
                    logger.info("Deleting fireteam {}...", fireteam.toString());
                    fireteam.setDeleted(true);
                    fireteamDao.update(fireteam);
                    bot.sendMessage(destinyChannel, "[DBOFT] [FIRETEAM DELETED] " + fireteam.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Unhandled error sweeping fireteams", e);
        } finally {
            if(connectionSource != null && connectionSource.isOpen())
                connectionSource.closeQuietly();
        }
    }

    private DboUser getUserFromDiv(String html) throws SQLException {
        Pattern creatorPattern = Pattern.compile("<a href=\"index\\.php\\?mode=user&amp;show_user=(\\d+)\".*?>(.*?)</a>", Pattern.DOTALL);
        Matcher creatorMatcher = creatorPattern.matcher(html);
        while(creatorMatcher.find()) {
            int userId = Integer.parseInt(creatorMatcher.group(1).trim());
            String handle = creatorMatcher.group(2).trim();
            logger.info("Analyzing user on site, id={} handle={}", userId, handle);
            DboUser foundUser = userDao.queryForId(userId);
            if(foundUser == null) {
                logger.info("Could not find user in DB, creating...");
                foundUser = new DboUser(userId, handle);
                userDao.create(foundUser);
            } else {
                logger.info("Found user in DB, updating...");
                foundUser.setHandle(handle);
                userDao.update(foundUser);
            }
            return foundUser;
        }
        logger.warn("Couldn't parse any user info from the site");
        return null;
    }

    private Set<FireteamRSVP> parseRSVPs(Fireteam fireteam, HtmlTable rsvpsTable) throws SQLException {

        logger.info("Parsing RSVPs on site for fireteam {}...", fireteam.getId());

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
            creator = getUserFromDiv(cell.asXml());

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

    private void notifyNewFireteam(Fireteam fireteam) {
        String shortUrl = IRCUtil.shortenUrl(fireteam.getUrl());
        bot.sendMessage(destinyChannel, "[DBOFT] [NEW FIRETEAM] " + fireteam.toString() + " -- " + shortUrl);
    }

    private void notifyFireteamChanges(Fireteam fireteam,
                                       List<String[]> fireteamChanges,
                                       HashMap<FireteamRSVP, List<String[]>> rsvpChanges,
                                       Set<FireteamRSVP> newRsvps,
                                       Set<FireteamRSVP> deletedRsvps) {

        StringBuilder sb;

        if(fireteamChanges != null && fireteamChanges.size() > 0) {
            sb = new StringBuilder();

            if (StringUtils.isEmpty(fireteam.getTitle()))
                sb.append(fireteam.getActivity());
            else
                sb.append(fireteam.getTitle());

            sb.append(" (").append(fireteam.getPlatform().getDisplayString()).append(")");

            for (String[] change : fireteamChanges) {
                sb.append(" || ");
                sb.append(change[0]).append(": ").append(change[1]).append(" -> ").append(change[2]);
            }

            bot.sendMessage(destinyChannel, "[DBOFT] [FIRETEAM UPDATE] " + sb.toString());
        }

        if(rsvpChanges != null && rsvpChanges.size() > 0) {
            for(FireteamRSVP changedRsvp : rsvpChanges.keySet()) {
                if(rsvpChanges.get(changedRsvp).size() > 0) { // only print if it has one or more changes

                    sb = new StringBuilder();

                    if (StringUtils.isEmpty(fireteam.getTitle()))
                        sb.append(fireteam.getActivity());
                    else
                        sb.append(fireteam.getTitle());

                    sb.append(" (").append(fireteam.getPlatform().getDisplayString()).append(") ");
                    sb.append(changedRsvp.getGamertag());

                    for (String[] change : rsvpChanges.get(changedRsvp)) {
                        sb.append(" || ");
                        sb.append(change[0]).append(": ").append(change[1]).append(" -> ").append(change[2]);
                    }

                    bot.sendMessage(destinyChannel, "[DBOFT] [RSVP CHANGED] " + sb.toString());
                }
            }
        }

        if(newRsvps != null && newRsvps.size() > 0) {
            sb = new StringBuilder();

            if (StringUtils.isEmpty(fireteam.getTitle()))
                sb.append(fireteam.getActivity());
            else
                sb.append(fireteam.getTitle());

            sb.append(" (").append(fireteam.getPlatform().getDisplayString()).append(")");

            for (FireteamRSVP rsvp : newRsvps) {
                sb.append(" || ");
                sb.append(rsvp.getGamertag());
                if(rsvp.getCharacterClass() != null)
                    sb.append(", ").append(rsvp.getCharacterClass());
                if(rsvp.getLevel() != null)
                    sb.append(", Level ").append(rsvp.getLevel());
                if(rsvp.isTentative())
                    sb.append(" (tentative)");
            }

            bot.sendMessage(destinyChannel, "[DBOFT] [NEW RSVPs] " + sb.toString());
        }

        if(deletedRsvps != null && deletedRsvps.size() > 0) {
            sb = new StringBuilder();

            if (StringUtils.isEmpty(fireteam.getTitle()))
                sb.append(fireteam.getActivity());
            else
                sb.append(fireteam.getTitle());

            sb.append(" (").append(fireteam.getPlatform().getDisplayString()).append(")");

            for (FireteamRSVP rsvp : deletedRsvps) {
                sb.append(" || ");
                sb.append(rsvp.getGamertag());
                if(rsvp.getCharacterClass() != null)
                    sb.append(", ").append(rsvp.getCharacterClass());
                if(rsvp.getLevel() != null)
                    sb.append(", Level ").append(rsvp.getLevel());
            }

            bot.sendMessage(destinyChannel, "[DBOFT] [CANCELED RSVPs] " + sb.toString());
        }


    }


}
