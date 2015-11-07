package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "map-variants", list = false)
public class MapVariantMeta implements Metadata {

    // A localized name, suitable for display to users.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // A reference to an image. This may be null if there is no image defined.
    String mapImageUrl;

    // The ID of the map this is a variant for. Maps are available via the Metadata API.
    String mapId;

    // The ID that uniquely identifies this map variant.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMapImageUrl() {
        return mapImageUrl;
    }

    public String getMapId() {
        return mapId;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
