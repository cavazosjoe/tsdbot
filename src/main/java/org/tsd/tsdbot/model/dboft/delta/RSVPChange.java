package org.tsd.tsdbot.model.dboft.delta;

/**
 * Created by Joe on 2/7/2015.
 */
public class RSVPChange implements Delta {

    private ChangeType changeType;

    private String fieldName;
    private String oldValue;
    private String newValue;

    public RSVPChange(ChangeType changeType, String fieldName, String oldValue, String newValue) {
        this.changeType = changeType;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public enum ChangeType {
        deleted,
        created,
        modified
    }
}
