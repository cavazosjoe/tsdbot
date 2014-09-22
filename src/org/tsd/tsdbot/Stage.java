package org.tsd.tsdbot;

/**
 * Created by Joe on 9/21/2014.
 */
public enum Stage {
    dev,
    production;

    public static Stage fromString(String s) {
        for(Stage stage : values()) {
            if(s.equalsIgnoreCase(stage.toString()))
                return stage;
        }
        return null;
    }
}
