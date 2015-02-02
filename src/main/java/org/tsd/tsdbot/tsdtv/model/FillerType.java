package org.tsd.tsdbot.tsdtv.model;

/**
* Created by Joe on 2/1/2015.
*/
public enum FillerType {
    bump            (".bumps", "Bumps"),
    commercial      (".commercials", "Commercials"),
    show_intro      (null, "Show Intro"),
    block_intro     (null, "Block Intro"),
    block_outro     (null, "Block Outro");

    private String scheduleId;
    private String displayString;

    FillerType(String scheduleId, String displayString) {
        this.scheduleId = scheduleId;
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static FillerType fromSchedule(String scheduleId) {
        for(FillerType ft : values()) {
            if(scheduleId.equals(ft.scheduleId))
                return ft;
        }
        return null;
    }
}
