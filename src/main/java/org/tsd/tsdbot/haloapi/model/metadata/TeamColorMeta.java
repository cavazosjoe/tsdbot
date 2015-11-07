package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "team-colors")
public class TeamColorMeta implements Metadata {

    // A localized name, suitable for display to users.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // A seven-character string representing the team color in "RGB Hex" notation. This
    // notation uses a "#" followed by a hex triplet.
    String color;

    // A reference to an image for icon use. This may be null if there is no image
    // defined.
    String iconUrl;

    // The ID that uniquely identifies this color. This will be the same as the team's ID
    // in responses from the Stats API.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
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
