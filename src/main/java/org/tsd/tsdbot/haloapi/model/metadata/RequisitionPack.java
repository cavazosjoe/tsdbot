package org.tsd.tsdbot.haloapi.model.metadata;

@HaloMeta(path = "requisition-packs", list = false)
public class RequisitionPack implements Metadata {

    // A localized name for the pack, suitable for display to users. The text is title cased.
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

    // If the pack is purchasable via credits, this value contains the number of credits a player must spend to acquire
    // one pack. This value is zero when isPurchasableWithCredits is false.
    int creditPrice;

    // If the pack is currently available for purchase by spending credits, then this value is true.
    boolean isPurchasableWithCredits;

    // If the pack might be obtainable through the Xbox Live Marketplace, then this value is true.
    boolean isPurchasableFromMarketplace;

    // If this pack might be obtainable through the Xbox Live Marketplace, this is the product ID. Note: Pricing and
    // availability within the Xbox Live marketplace is controlled independently of this value. The presence of an Id
    // in this field is not a guarantee the product is purchasable. There may be geographic restrictions restricting
    // purchase in certain regions, or the item may not be currently purchasable at all.
    String xboxMarketplaceProductId;

    // If this pack might be obtainable through the Xbox Live Marketplace, this is the URL to the product.
    String xboxMarketplaceProductUrl;

    // Internal use. The order in which packs are shown for merchandising purposes.
    int merchandisingOrder;

    // Internal use. Indicates the visual treatment of the pack. This is one of the following options:
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

    @Override
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }
}
