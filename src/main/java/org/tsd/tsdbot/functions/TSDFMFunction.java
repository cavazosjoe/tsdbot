package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.tsdfm.TSDFM;
import org.tsd.tsdbot.tsdfm.TSDFMLibrary;
import org.tsd.tsdbot.tsdfm.model.TSDFMAlbum;
import org.tsd.tsdbot.tsdfm.model.TSDFMArtist;
import org.tsd.tsdbot.tsdfm.model.TSDFMSong;

import java.util.LinkedList;
import java.util.List;

@Singleton
@Function(initialRegex = "^\\.tsdfm.*")
public class TSDFMFunction extends MainFunctionImpl {

    private static final Logger log = LoggerFactory.getLogger(TSDFMFunction.class);

    private final TSDFM tsdfm;
    private final TSDFMLibrary library;

    @Inject
    public TSDFMFunction(TSDBot bot, TSDFM tsdfm, TSDFMLibrary library) {
        super(bot);
        this.tsdfm = tsdfm;
        this.library = library;
        this.description = "The TSDFM Official Radio Station of TSDFM";
        this.usage = "USAGE: .tsdfm [ tag [tag_options] ] [ request [request_options] ]";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");
        if(cmdParts.length < 2) {
            bot.sendMessage(channel, usage);
            return;
        }

        switch(cmdParts[1]) {
            case "tag": {

                if(!bot.userIsOwner(sender)) {
                    bot.sendMessage(channel, "Only my owner can tag songs");
                    return;
                }

                if(cmdParts.length < 5) {
                    bot.sendMessage(channel, "USAGE: .tsdfm tag [-artist <q>] [-album <q>] [-song <q>] <tag1> <tag2>");
                } else try {
                    TaggingInfo taggingInfo = new TaggingInfo(cmdParts);
                    if(taggingInfo.queryInfo.songQuery == null && taggingInfo.queryInfo.albumQuery == null && taggingInfo.queryInfo.artistQuery == null) {
                        bot.sendMessage(channel, "Must specify either an artist, album, or song query to tag");
                    } else if(taggingInfo.tags.isEmpty()) {
                        bot.sendMessage(channel, "Could not detect any tags");
                    } else if(taggingInfo.queryInfo.songQuery != null) {
                        TSDFMSong song = library.getSong(taggingInfo.queryInfo.songQuery, taggingInfo.queryInfo.albumQuery, taggingInfo.queryInfo.artistQuery);
                        library.tagSong(song, taggingInfo.tags);
                    } else if(taggingInfo.queryInfo.albumQuery != null) {
                        TSDFMAlbum album = library.getAlbum(taggingInfo.queryInfo.albumQuery, taggingInfo.queryInfo.artistQuery);
                        library.tagAlbum(album, taggingInfo.tags);
                    } else {
                        TSDFMArtist artist = library.getArtist(taggingInfo.queryInfo.artistQuery);
                        library.tagArtist(artist, taggingInfo.tags);
                    }
                } catch (Exception qe) {
                    bot.sendMessage(channel, "Tagging error: " + qe.getMessage());
                }
                break;
            }
            case "request": {
                if(cmdParts.length < 4) {
                    bot.sendMessage(channel, "USAGE: .tsdfm request [-artist <q>] [-album <q>] [-song <q>]");
                } else try {
                    QueryInfo queryInfo = new QueryInfo(cmdParts);
                    if(queryInfo.songQuery == null && queryInfo.albumQuery == null && queryInfo.artistQuery == null) {
                        bot.sendMessage(channel, "Must specify either an artist, album, or song query to tag");
                        return;
                    }

                    TSDFMSong song;
                    if(queryInfo.songQuery != null) {
                        song = library.getSong(queryInfo.songQuery, queryInfo.albumQuery, queryInfo.artistQuery);
                    } else if(queryInfo.albumQuery != null) {
                        TSDFMAlbum album = library.getAlbum(queryInfo.albumQuery, queryInfo.artistQuery);
                        song = library.getRandomSongFromAlbum(album);
                    } else {
                        TSDFMArtist artist = library.getArtist(queryInfo.artistQuery);
                        song = library.getRandomSongFromArtist(artist);
                    }

                    tsdfm.request(sender, channel, song);
                    bot.sendMessage(channel, "Song queued: " + song.getArtist().getName() + " - " + song.getTitle());

                } catch (Exception e) {
                    log.error("Error requesting song", e);
                    bot.sendMessage(channel, "Request error: " + e.getMessage());
                }
                break;
            }
            case "reload": {
                if(!bot.userIsOwner(sender)) {
                    bot.sendMessage(channel, "Only my owner can do that");
                    return;
                }
            }
            default: {
                bot.sendMessage(channel, usage);
            }
        }

    }

    static class TaggingInfo {
        
        public QueryInfo queryInfo = new QueryInfo();
        public List<String> tags = new LinkedList<>();

        public TaggingInfo(String[] parts) {
            int i=0;
            while(i < parts.length) {
                if(parts[i].equalsIgnoreCase("-artist")) {
                    i++;
                    queryInfo.artistQuery = parts[i];
                } else if(parts[i].equalsIgnoreCase("-album")) {
                    i++;
                    queryInfo.albumQuery = parts[i];
                } else if(parts[i].equalsIgnoreCase("-song")) {
                    i++;
                    queryInfo.songQuery = parts[i];
                } else if(i > 3) {
                    // minimums: .tsdfm tag -artist blah tag1 tag2
                    tags.add(parts[i]);
                }
                i++;
            }
        }
    }

    static class QueryInfo {
        public String artistQuery = null;
        public String albumQuery = null;
        public String songQuery = null;
        
        public QueryInfo() {}
        
        public QueryInfo(String[] parts) {
            int i=0;
            while(i < parts.length) {
                if(parts[i].equalsIgnoreCase("-artist")) {
                    i++;
                    artistQuery = parts[i];
                } else if(parts[i].equalsIgnoreCase("-album")) {
                    i++;
                    albumQuery = parts[i];
                } else if(parts[i].equalsIgnoreCase("-song")) {
                    i++;
                    songQuery = parts[i];
                }
                i++;
            }
        }
    }

}
