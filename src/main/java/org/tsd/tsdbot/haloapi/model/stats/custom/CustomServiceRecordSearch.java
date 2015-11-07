package org.tsd.tsdbot.haloapi.model.stats.custom;

import java.util.List;

public class CustomServiceRecordSearch {

    List<CustomServiceRecordSearchResult> Results;

    // Internal use only. A set of related resource links.
    Object Links;

    public List<CustomServiceRecordSearchResult> getResults() {
        return Results;
    }

    public Object getLinks() {
        return Links;
    }
}
