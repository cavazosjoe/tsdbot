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

    public static class Category {

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

    public static class Level {

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

    public static class RequiredLevel {

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
