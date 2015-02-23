package org.tsd.tsdbot.model.dboft.delta;

/**
 * Created by Joe on 2/7/2015.
 */
public class FieldChange implements Delta {

    private String fieldName;
    private String oldValue;
    private String newValue;

    public FieldChange(String fieldName, String oldValue, String newValue) {
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
