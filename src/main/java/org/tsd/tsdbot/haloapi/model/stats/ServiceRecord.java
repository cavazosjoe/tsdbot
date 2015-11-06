package org.tsd.tsdbot.haloapi.model.stats;

import org.tsd.tsdbot.haloapi.model.Player;

public abstract class ServiceRecord {

    // Information about the player for whom this data was returned.
    Player PlayerId;

    // The player's Spartan Rank.
    int SpartanRank;

    // The player's XP.
    int Xp;
    
}
