package org.tsd.tsdbot.haloapi.model.metadata;

import java.util.List;

@HaloMeta(path = "commendations")
public class CommendationMeta implements Metadata {

    // Indicates the type of commendation. This is one of the two following options:
    //   - "Progressive"
    //   - "Meta"
    // Progressive commendations have a series of increasingly difficult thresholds
    // (levels) a player must cross to receive increasingly greater rewards.
    // Meta commendations are unlocked when a player has completed one or more other
    // commendation levels. We model this by giving meta commendations one level with
    // dependencies rather than a threshold.
    String type;

    // A localized name for the commendation, suitable for display to users. The text is
    // title cased.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // An image that is used as the icon for this commendation.
    String iconImageUrl;

    // One or more levels that model what a player must do to earn rewards and complete
    // the commendation.
    List<Level> levels;

    // For meta commendations, the commendation is considered "completed" when all
    // required levels have been "completed". This list contains one or more Level Ids
    // from other commendations. For progressive commendations, this list is empty.
    List<RequiredLevel> requiredLevels;

    // The reward the player will receive for earning this commendation.
    Reward reward;

    // Information about how this commendation should be categorized when shown to users.
    Category category;

    // Whether this commendation is enabled or not.
    boolean enabled;

    // The ID that uniquely identifies this commendation.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconImageUrl() {
        return iconImageUrl;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public List<RequiredLevel> getRequiredLevels() {
        return requiredLevels;
    }

    public Reward getReward() {
        return reward;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }

    public class Category {

        // A localized name for the category, suitable for display to users. The text is
        // title cased.
        String name;

        // An image that is used as the icon for this category.
        String iconImageUrl;

        // Internal use. The order in which the category should be displayed relative to
        // other categories. The lower the value, the more important the category - more
        // important categories should be shown before or ahead of less important
        // categories.
        int order;

        // The ID that uniquely identifies this category.
        String id;

        // Internal use only. Do not use.
        String contentId;

        public String getName() {
            return name;
        }

        public String getIconImageUrl() {
            return iconImageUrl;
        }

        public int getOrder() {
            return order;
        }

        public String getId() {
            return id;
        }

        public String getContentId() {
            return contentId;
        }
    }

    public class Level {

        // The reward the player will receive for earning this level.
        Reward reward;

        // For progressive commendations this indicates the threshold that the player
        // must meet or exceed to consider the commendation level "completed". For meta
        // commendations, this value is always zero.
        int threshold;

        // The ID that uniquely identifies this commendation level.
        String id;

        // Internal use only. Do not use.
        String contentId;

        public Reward getReward() {
            return reward;
        }

        public int getThreshold() {
            return threshold;
        }

        public String getId() {
            return id;
        }

        public String getContentId() {
            return contentId;
        }
    }

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

    public class RequisitionPack {

        // A localized name for the pack, suitable for display to users. The text
        // is title cased.
        String name;

        // A localized description, suitable for display to users.
        String description;

        // A large image for the pack.
        String largeImageUrl;

        // A medium image for the pack.
        String mediumImageUrl;

        // A small image for the pack.
        String smallImageUrl;

        // Internal use. Whether the item should be featured ahead of others.
        boolean isFeatured;

        // Internal use. Whether the item should be labeled as "new!"
        boolean isNew;

        // If the pack is purchasable via credits, this value contains the number
        // of credits a player must spend to acquire one pack. This value is zero
        // when isPurchasableWithCredits is false.
        int creditPrice;

        // If the pack is currently available for purchase by spending credits,
        // then this value is true.
        boolean isPurchasableWithCredits;

        // If the pack might be obtainable through the Xbox Live Marketplace, then
        // this value is true.
        boolean isPurchasableFromMarketplace;

        // If this pack might be obtainable through the Xbox Live Marketplace, this
        // is the product ID. Note: Pricing and availability within the Xbox Live
        // marketplace is controlled independently of this value. The presence of
        // an Id in this field is not a guarantee the product is purchasable. There
        // may be geographic restrictions restricting purchase in certain regions,
        // or the item may not be currently purchasable at all.
        String xboxMarketplaceProductId;

        // If this pack might be obtainable through the Xbox Live Marketplace, this
        // is the URL to the product.
        String xboxMarketplaceProductUrl;

        // Internal use. The order in which packs are shown for merchandising
        // purposes.
        int merchandisingOrder;

        // Internal use. Indicates the visual treatment of the pack. This is one of
        // the following options:
        //   - None
        //   - New
        //   - Hot
        //   - LeavingSoon
        //   - MaximumValue
        //   - Limitedtime
        //   - Featured
        //   - BestSeller
        //   - Popular
        String flair;

        // The ID that uniquely identifies this pack.
        String id;

        // Internal use only. Do not use.
        String contentId;

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getLargeImageUrl() {
            return largeImageUrl;
        }

        public String getMediumImageUrl() {
            return mediumImageUrl;
        }

        public String getSmallImageUrl() {
            return smallImageUrl;
        }

        public boolean isFeatured() {
            return isFeatured;
        }

        public boolean isNew() {
            return isNew;
        }

        public int getCreditPrice() {
            return creditPrice;
        }

        public boolean isPurchasableWithCredits() {
            return isPurchasableWithCredits;
        }

        public boolean isPurchasableFromMarketplace() {
            return isPurchasableFromMarketplace;
        }

        public String getXboxMarketplaceProductId() {
            return xboxMarketplaceProductId;
        }

        public String getXboxMarketplaceProductUrl() {
            return xboxMarketplaceProductUrl;
        }

        public int getMerchandisingOrder() {
            return merchandisingOrder;
        }

        public String getFlair() {
            return flair;
        }

        public String getId() {
            return id;
        }

        public String getContentId() {
            return contentId;
        }
    }

    public class RequiredLevel {

        // The threshold that the player must meet or exceed in order to consider the
        // level requirement met.
        int threshold;

        // The ID of the commendation level that must be met in order to consider the
        // level requirement met.
        String id;

        // Internal use only. Do not use.
        String contentId;

        public int getThreshold() {
            return threshold;
        }

        public String getId() {
            return id;
        }

        public String getContentId() {
            return contentId;
        }
    }
}
