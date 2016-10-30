package org.tsd.tsdbot.model.dbo.fireteam;

/**
 * Created by Joe on 2/7/2015.
 */
public enum Difficulty {
    Normal,
    Hard;

    public static Difficulty fromString(String s) {
        for(Difficulty d : values()) {
            if(d.toString().equals(s))
                return d;
        }
        return null;
    }
}
