package org.tsd.tsdbot.haloapi.model.stats.warzone;

import java.util.List;

public class WarzoneServiceRecordSearch {

    List<WarzoneServiceRecordSearchResult> Results;

    // Internal use only. A set of related resource links.
    Object Links;

    public List<WarzoneServiceRecordSearchResult> getResults() {
        return Results;
    }

}
