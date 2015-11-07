package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "playlists")
public class PlaylistMeta implements Metadata {

    // A localized name for the playlist, suitable for display to users. The text is
    // title cased.
    String name;

    // A localized description for the playlist, suitable for display to users.
    String description;

    // Indicates if a CSR (competitive skill rank) is shown for players who participate
    // in this playlist.
    boolean isRanked;

    // An image used to illustrate this playlist.
    String imageUrl;

    // The game mode played in this playlist. Options are:
    //   - Arena
    //   - Campaign
    //   - Custom
    //   - Warzone
    String gameMode;

    // Indicates if this playlist is currently available for play.
    boolean isActive;

    // The ID that uniquely identifies this playlist.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRanked() {
        return isRanked;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getGameMode() {
        return gameMode;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
