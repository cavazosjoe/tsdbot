package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "spartan-ranks")
public class SpartanRankMeta implements Metadata {

    // The amount of XP required to enter this rank.
    int startXp;

    // The reward the player will receive for earning this Spartan Rank.
    Reward reward;

    // The ID that uniquely identifies this Spartan Rank.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public int getStartXp() {
        return startXp;
    }

    public Reward getReward() {
        return reward;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
