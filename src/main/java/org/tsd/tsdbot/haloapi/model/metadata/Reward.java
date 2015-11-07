package org.tsd.tsdbot.haloapi.model.metadata;

import java.util.List;

public class Reward {

    // The amount of XP that will be awarded.
    int xp;

    // The set of requisition packs (if any) that will be awarded.
    List<RequisitionPack> requisitionPacks;

    // The ID that uniquely identifies this reward.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public int getXp() {
        return xp;
    }

    public List<RequisitionPack> getRequisitionPacks() {
        return requisitionPacks;
    }

    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }

}
