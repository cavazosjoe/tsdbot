package org.tsd.tsdbot.model.dbo.fireteam;

/**
* Created by Joe on 2/7/2015.
*/
public enum Platform {
    x360    ("X360"),
    xb1     ("XBONE"),
    ps3     ("PS3"),
    ps4     ("PS4");

    private String displayString;

    Platform(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static Platform fromString(String s) {
        for(Platform p : values()) {
            if(p.toString().equals(s)) {
                return p;
            }
        }
        return null;
    }
}
