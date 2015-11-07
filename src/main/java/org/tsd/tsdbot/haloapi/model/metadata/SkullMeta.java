package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "skulls")
public class SkullMeta implements Metadata {

    // A localized name, suitable for display to users. The text is title cased.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // Indicates what mission this skull can be located within. Null when the skull is
    // not found in a mission. Missions are available via the Metadata API.
    String missionId;

    // missing from online docs
    String imageUrl;

    // The ID that uniquely identifies this skull.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMissionId() {
        return missionId;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
