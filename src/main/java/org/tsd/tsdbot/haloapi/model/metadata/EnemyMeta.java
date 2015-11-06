package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "enemies")
public class EnemyMeta implements Metadata {

    // The faction that this enemy is affiliated with. One of the following options:
    //   - UNSC
    //   - Covenant
    //   - Promethean
    String faction;

    // A localized name for the object, suitable for display to users. The text is title
    // cased.
    String name;

    // A localized description, suitable for display to users. Note: This may be null.
    String description;

    // A reference to a large image for icon use. This may be null if there is no image
    // defined.
    String lageIconImageUrl;

    // A reference to a small image for icon use. This may be null if there is no image
    // defined.
    String smallIconImageUrl;

    // The ID that uniquely identifies this enemy.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getFaction() {
        return faction;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLageIconImageUrl() {
        return lageIconImageUrl;
    }

    public String getSmallIconImageUrl() {
        return smallIconImageUrl;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
