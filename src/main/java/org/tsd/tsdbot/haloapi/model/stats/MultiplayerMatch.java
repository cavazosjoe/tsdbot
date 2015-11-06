package org.tsd.tsdbot.haloapi.model.stats;

import java.util.List;

public abstract class MultiplayerMatch {

    // A list of stats for each team who in the match. Note that in Free For All modes,
    // there is an entry for every player.
    List<TeamStat> TeamStats;

    // Indicates if the match is completed or not. Some match details are available while
    // the match is in-progress, but the behavior for incomplete matches in undefined.
    boolean IsMatchOver;

    // The length of the match. This is expressed as an ISO 8601 Duration.
    String TotalDuration;

    // The variant of the map for this match. Map variants are available via the Metadata
    // API.
    String MapVariantId;

    // The variant of the game for this match. Game variants are available via the Metadata
    // API.
    String GameVariantId;

    // The playlist ID of the match. Playlists are available via the Metadata API.
    String PlaylistId;

    // The ID of the base map for this match. Maps are available via the Metadata API.
    String MapId;

    // The ID of the game base variant for this match. Game base variants are available via
    // the Metadata API.
    String GameBaseVariantId;

    // Whether this was a team-based game or not.
    boolean IsTeamGame;

    // Internal use only. This will always be null.
    Object SeasonId;

    public List<TeamStat> getTeamStats() {
        return TeamStats;
    }

    public boolean isMatchOver() {
        return IsMatchOver;
    }

    public String getTotalDuration() {
        return TotalDuration;
    }

    public String getMapVariantId() {
        return MapVariantId;
    }

    public String getGameVariantId() {
        return GameVariantId;
    }

    public String getPlaylistId() {
        return PlaylistId;
    }

    public String getMapId() {
        return MapId;
    }

    public String getGameBaseVariantId() {
        return GameBaseVariantId;
    }

    public boolean isTeamGame() {
        return IsTeamGame;
    }

    public Object getSeasonId() {
        return SeasonId;
    }
}
