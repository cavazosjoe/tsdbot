package org.tsd.tsdbot.history;

import com.google.inject.Inject;
import org.tsd.tsdbot.functions.MainFunction;

import java.util.Set;

/**
 * Created by Joe on 1/14/2015.
 */
public class NoCommandsStrategy implements MessageFilterStrategy {

    @Inject
    private Set<MainFunction> functions;

    @Override
    public boolean apply(HistoryBuff.Message m) {
        for(MainFunction func : functions) {
            if(m.text.matches(func.getRegex()))
                return false;
        }
        return true;
    }
}
