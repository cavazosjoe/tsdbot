package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "flexible-stats")
public class FlexibleStatMeta implements Metadata {

    // A localized name for the data point, suitable for display to users. The text is
    // title cased.
    private String name;

    // The type of stat this represents, it is one of the following options:
    //   - Count
    //   - Duration
    private String type;

    // The ID that uniquely identifies this stat.
    private String id;

    // Internal use only. Do not use.
    private String contentId;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
