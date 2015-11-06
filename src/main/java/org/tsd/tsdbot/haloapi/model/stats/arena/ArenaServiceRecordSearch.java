package org.tsd.tsdbot.haloapi.model.stats.arena;

import java.util.List;

public class ArenaServiceRecordSearch {

    List<ArenaServiceRecordSearchResult> Results;

    // Internal use only. A set of related resource links.
    Object Links;

    public List<ArenaServiceRecordSearchResult> getResults() {
        return Results;
    }

}
