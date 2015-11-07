package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "maps")
public class MapMeta implements Metadata {

    // A localized name, suitable for display to users.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // This lists all the game modes to which this map is available. Options are:
    //   - Arena
    //   - Campaign
    //   - Custom
    //   - Warzone
    String[] supportedGameModes;

    // A reference to an image. This may be null if there is no image defined.
    String imageUrl;

    // The ID that uniquely identifies this map.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getSupportedGameModes() {
        return supportedGameModes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
