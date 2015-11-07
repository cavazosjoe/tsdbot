package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "impulses")
public class ImpulseMeta implements Metadata {

    // Internal use. The non-localized name of the impulse.
    String internalName;

    // The ID that uniquely identifies this impulse.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getInternalName() {
        return internalName;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
