package org.tsd.tsdbot.haloapi.model.stats;

import org.tsd.tsdbot.haloapi.model.Count;

import java.util.List;

public abstract class Stat {

    // Total number of kills done by the player. This includes melee kills, shoulder
    // bash kills and Spartan charge kills, all power weapons, AI kills and vehicle
    // destructions.
    int TotalKills;

    // Total number of headshots done by the player.
    int TotalHeadshots;

    // Total weapon damage dealt by the player.
    double TotalWeaponDamage;

    // Total number of shots fired by the player.
    int TotalShotsFired;

    // Total number of shots landed by the player.
    int TotalShotsLanded;

    // The weapon the player used to get the most kills this match.
    WeaponStat WeaponWithMostKills;

    // Total number of melee kills by the player.
    int TotalMeleeKills;

    // Total melee damage dealt by the player.
    double TotalMeleeDamage;

    // Total number of assassinations by the player.
    int TotalAssassinations;

    // Total number of ground pound kills by the player.
    int TotalGroundPoundKills;

    // Total ground pound damage dealt by the player.
    double TotalGroundPoundDamage;

    // Total number of shoulder bash kills by the player.
    int TotalShoulderBashKills;

    // Total shoulder bash damage dealt by the player.
    double TotalShoulderBashDamage;

    // Total grenade damage dealt by the player.
    double TotalGrenadeDamage;

    // Total number of power weapon kills by the player.
    int TotalPowerWeaponKills;

    // Total power weapon damage dealt by the player.
    double TotalPowerWeaponDamage;

    // Total number of power weapon grabs by the player.
    int TotalPowerWeaponGrabs;

    // Total power weapon possession by the player. This is expressed as an ISO 8601
    // Duration.
    String TotalPowerWeaponPossessionTime;

    // Total number of deaths by the player.
    int TotalDeaths;

    // Total number of assists by the player.
    int TotalAssists;

    // Not used.
    int TotalGamesCompleted;

    // Not used.
    int TotalGamesWon;

    // Not used.
    int TotalGamesLost;

    // Not used.
    int TotalGamesTied;

    // Total timed played in this match by the player.
    String TotalTimePlayed;

    // Total number of grenade kills by the player.
    int TotalGrenadeKills;

    // The set of Medals earned by the player.
    List<MedalAward> MedalAwards;

    // List of enemy vehicles destroyed. Vehicles are available via the Metadata API.
    // Note: this stat measures enemy vehicles, not any vehicle destruction.
    List<EnemyKill> DestroyedEnemyVehicles;

    // List of enemies killed, per enemy type. Enemies are available via the Metadata
    // API.
    List<EnemyKill> EnemyKills;

    // The set of weapons (weapons and vehicles included) used by the player.
    List<WeaponStat> WeaponStats;

    // The set of Impulses (invisible Medals) earned by the player.
    List<Count> Impulses;

    // Total number of Spartan kills by the player.
    int TotalSpartanKills;

    public int getTotalKills() {
        return TotalKills;
    }

    public int getTotalHeadshots() {
        return TotalHeadshots;
    }

    public double getTotalWeaponDamage() {
        return TotalWeaponDamage;
    }

    public int getTotalShotsFired() {
        return TotalShotsFired;
    }

    public int getTotalShotsLanded() {
        return TotalShotsLanded;
    }

    public WeaponStat getWeaponWithMostKills() {
        return WeaponWithMostKills;
    }

    public int getTotalMeleeKills() {
        return TotalMeleeKills;
    }

    public double getTotalMeleeDamage() {
        return TotalMeleeDamage;
    }

    public int getTotalAssassinations() {
        return TotalAssassinations;
    }

    public int getTotalGroundPoundKills() {
        return TotalGroundPoundKills;
    }

    public double getTotalGroundPoundDamage() {
        return TotalGroundPoundDamage;
    }

    public int getTotalShoulderBashKills() {
        return TotalShoulderBashKills;
    }

    public double getTotalShoulderBashDamage() {
        return TotalShoulderBashDamage;
    }

    public double getTotalGrenadeDamage() {
        return TotalGrenadeDamage;
    }

    public int getTotalPowerWeaponKills() {
        return TotalPowerWeaponKills;
    }

    public double getTotalPowerWeaponDamage() {
        return TotalPowerWeaponDamage;
    }

    public int getTotalPowerWeaponGrabs() {
        return TotalPowerWeaponGrabs;
    }

    public String getTotalPowerWeaponPossessionTime() {
        return TotalPowerWeaponPossessionTime;
    }

    public int getTotalDeaths() {
        return TotalDeaths;
    }

    public int getTotalAssists() {
        return TotalAssists;
    }

    public int getTotalGamesCompleted() {
        return TotalGamesCompleted;
    }

    public int getTotalGamesWon() {
        return TotalGamesWon;
    }

    public int getTotalGamesLost() {
        return TotalGamesLost;
    }

    public int getTotalGamesTied() {
        return TotalGamesTied;
    }

    public String getTotalTimePlayed() {
        return TotalTimePlayed;
    }

    public int getTotalGrenadeKills() {
        return TotalGrenadeKills;
    }

    public List<MedalAward> getMedalAwards() {
        return MedalAwards;
    }

    public List<EnemyKill> getDestroyedEnemyVehicles() {
        return DestroyedEnemyVehicles;
    }

    public List<EnemyKill> getEnemyKills() {
        return EnemyKills;
    }

    public List<WeaponStat> getWeaponStats() {
        return WeaponStats;
    }

    public List<Count> getImpulses() {
        return Impulses;
    }

    public int getTotalSpartanKills() {
        return TotalSpartanKills;
    }
}
