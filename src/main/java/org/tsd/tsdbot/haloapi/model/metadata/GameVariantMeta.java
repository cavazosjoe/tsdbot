package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "game-variants", list = false)
public class GameVariantMeta implements Metadata {

    // A localized name, suitable for display to users.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // The ID of the game base variant this is a variant for. Game Base Variants are
    // available via the Metadata API.
    String gameBaseVariantId;

    // An icon image for the game variant.
    String iconUrl;

    // The ID that uniquely identifies this game variant.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getGameBaseVariantId() {
        return gameBaseVariantId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
