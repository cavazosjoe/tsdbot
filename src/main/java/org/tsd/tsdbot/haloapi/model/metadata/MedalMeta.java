package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "medals")
public class MedalMeta implements Metadata {

    // A localized name for the medal, suitable for display to users.
    String name;

    // A localized description, suitable for display to users.
    String description;

    // The type of this medal. It will be one of the following options:
    //   - Unknown
    //   - Multi-kill
    //   - Spree
    //   - Style
    //   - Vehicle
    //   - Breakout
    //   - Objective
    String classification;

    // The anticipated difficulty, relative to all other medals of this classification.
    // The difficulty is ordered from easiest to most difficult.
    int difficulty;

    // The location on the sprite sheet for the medal.
    SpriteLocation spriteLocation;

    // The ID that uniquely identifies this map medal.
    String id;

    // Internal use only. Do not use.
    String contentId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getClassification() {
        return classification;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public SpriteLocation getSpriteLocation() {
        return spriteLocation;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }

    public class SpriteLocation {

        // A reference to an image that contains all the sprites.
        String spriteSheetUri;

        // The X coordinate where the top-left corner of the sprite is located.
        int left;

        // The Y coordinate where the top-left corner of the sprite is located.
        int top;

        // The width of the full sprite sheet (in pixels). The dimensions of the full sheet
        // are included so that the sheet can be resized.
        int width;

        // The height of the full sprite sheet (in pixels). The dimensions of the full
        // sheet are included so that the sheet can be resized.
        int height;

        // The width of this sprite (in pixels).
        int spriteWidth;

        // The height of this sprite (in pixels).
        int spriteHeight;

        public String getSpriteSheetUri() {
            return spriteSheetUri;
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getSpriteWidth() {
            return spriteWidth;
        }

        public int getSpriteHeight() {
            return spriteHeight;
        }
    }

}
