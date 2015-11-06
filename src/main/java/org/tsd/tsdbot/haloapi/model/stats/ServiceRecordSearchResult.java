package org.tsd.tsdbot.haloapi.model.stats;

public abstract class ServiceRecordSearchResult {

    // The player's gamertag.
    String Id;

    // The result of the query for the player. One of the following:
    //   Success = 0,
    //   NotFound = 1,
    //   ServiceFailure = 2,
    //   ServiceUnavailable = 3
    // It is possible for different requests from the batch to succeed and fail
    // independently.
    int ResultCode;

    public String getId() {
        return Id;
    }

    public int getResultCode() {
        return ResultCode;
    }
}
