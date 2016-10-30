package org.tsd.tsdbot.model.dbo.fireteam;

/**
 * Created by Joe on 2/7/2015.
 */
public enum CharacterClass {
    hunter("Hunter"),
    warlock("Warlock"),
    titan("Titan");

    private String displayString;

    CharacterClass(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static CharacterClass fromString(String s) {
        for(CharacterClass cc : values()) {
            if(cc.toString().equalsIgnoreCase(s))
                return cc;
        }
        return null;
    }
}
