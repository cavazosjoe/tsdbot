package org.tsd.tsdbot.history.filter;

import com.google.inject.Inject;
import org.tsd.tsdbot.functions.MainFunction;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.Set;

public class NoCommandsStrategy implements MessageFilterStrategy {

    @Inject
    private Set<MainFunction> functions;

    @Override
    public boolean apply(HistoryBuff.Message m) {
        for(MainFunction func : functions) {
            if(m.text.matches(func.getListeningRegex())) {
                return false;
            }
        }
        return true;
    }
}
