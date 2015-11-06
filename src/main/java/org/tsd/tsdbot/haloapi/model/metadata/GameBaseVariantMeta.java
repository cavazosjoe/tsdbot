package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "game-base-variants")
public class GameBaseVariantMeta implements Metadata {

    // A localized name for the game base variant, suitable for display to users. The
    // text is title cased.
    String name;

    // Internal use. The internal non-localized name for the the game base variant.
    String internalName;

    // An image to use as the game base variant for the designation.
    String iconUrl;

    // A list that indicates what game modes this base variant is available within.
    // Options are:
    //   - Arena
    //   - Campaign
    //   - Custom
    //   - Warzone
    String[] supportedGameModes;

    // The ID that uniquely identifies this game base variant.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String[] getSupportedGameModes() {
        return supportedGameModes;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
