package org.tsd.tsdbot.model.dboft;

/**
 * Created by Joe on 2/7/2015.
 */
public enum CharacterClass {
    hunter,
    warlock,
    titan;

    public static CharacterClass fromString(String s) {
        for(CharacterClass cc : values()) {
            if(cc.toString().equalsIgnoreCase(s))
                return cc;
        }
        return null;
    }
}
