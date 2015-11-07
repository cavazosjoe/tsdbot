package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "weapons")
public class WeaponMeta implements Metadata {

    // A localized name for the object, suitable for display to users. The text is title
    // cased.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // The type of the vehicle. Options are:
    //   - Grenade
    //   - Turret
    //   - Vehicle
    //   - Standard
    //   - Power
    String type;

    // A reference to a large image for icon use. This may be null if there is no image
    // defined.
    String largeIconImageUrl;

    // A reference to a small image for icon use. This may be null if there is no image
    // defined.
    String smallIconImageUrl;

    // Indicates whether the weapon is usable by a player.
    boolean isUsableByPlayer;

    // The ID that uniquely identifies the weapon.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getLargeIconImageUrl() {
        return largeIconImageUrl;
    }

    public String getSmallIconImageUrl() {
        return smallIconImageUrl;
    }

    public boolean isUsableByPlayer() {
        return isUsableByPlayer;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
