package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "campaign-missions")
public class CampaignMissionMeta implements Metadata {

    // The order of the mission in the story. The first mission is #1.
    private int missionNumber;

    // A localized name suitable for display.
    private String name;

    // A localized description, suitable for display to users.
    private String description;

    // An image that is used as the background art for this mission.
    private String imageUrl;

    // The team for the mission. One of the following values:
    //   - BlueTeam
    //   - OsirisTeam
    private String type;

    // The ID that uniquely identifies this campaign mission.
    private String id;

    // Internal use only. Do not use.
    private String contentId;

    public int getMissionNumber() {
        return missionNumber;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
