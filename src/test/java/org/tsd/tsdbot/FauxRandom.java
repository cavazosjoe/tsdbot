package org.tsd.tsdbot;

import java.util.Random;

/**
 * Created by Joe on 3/29/2015.
 */
public class FauxRandom extends Random {

    private int intVal;
    private double doubleVal;

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    @Override
    public double nextDouble() {
        return doubleVal;
    }

    @Override
    public int nextInt(int n) {
        return intVal;
    }
}
