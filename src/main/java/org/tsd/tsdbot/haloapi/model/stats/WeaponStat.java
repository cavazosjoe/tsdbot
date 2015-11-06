package org.tsd.tsdbot.haloapi.model.stats;

public class WeaponStat {

    WeaponId WeaponId;

    // The number of shots fired for this weapon.
    int TotalShotsFired;

    // The number of shots landed for this weapon.
    int TotalShotsLanded;

    // The number of headshots for this weapon.
    int TotalHeadshots;

    // The number of kills for this weapon.
    int TotalKills;

    // The total damage dealt for this weapon.
    double TotalDamageDealt;

    // The total possession time for this weapon. This is expressed as an ISO 8601
    // Duration.
    String TotalPossessionTime;

}
