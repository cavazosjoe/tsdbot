package org.tsd.tsdbot.util;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class SurgeProtector {

    private static final Logger log = LoggerFactory.getLogger(SurgeProtector.class);

    private static final int actionMinimum = 5;
    private static final long actionHistoryLength = TimeUnit.MINUTES.toMillis(5);

    private static final Map<ActionType, Double> maxActionsPerMinute = new HashMap<>();

    static{
        maxActionsPerMinute.put(ActionType.TSDTV_PLAY, 6.0);
        maxActionsPerMinute.put(ActionType.FUNCTION, 10.0);
    }

    private final Map<String, List<Action>> actionHistory = new ConcurrentHashMap<>();

    public void logAction(ActionType type, String id) throws FloodException {
        trimHistory(id);
        if(!actionHistory.containsKey(id)) {
            actionHistory.put(id, new LinkedList<>());
        }
        actionHistory.get(id).add(new Action(id, type));
        double maxPerMinute = maxActionsPerMinute.get(type);
        long historyStart = System.currentTimeMillis();
        long occurrences = actionHistory.get(id).stream()
                .filter(action -> action.actionType.equals(type))
                .count();

        if(occurrences >= actionMinimum) {
            long elapsedTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - historyStart);
            double maxAllowable = elapsedTimeInMinutes*maxPerMinute;
            if(occurrences > maxAllowable) {
                log.warn("Detected an action flood: id={} / action={} / timeInMinutes={} / occurrences={}",
                        new Object[]{id, type, elapsedTimeInMinutes, occurrences});
                throw new FloodException();
            }
        }
    }

    private void trimHistory(String id) {
        if(actionHistory.containsKey(id)) {
            long now = System.currentTimeMillis();
            actionHistory.get(id).removeIf(action -> now - action.time > actionHistoryLength);
        }
    }

    public static class Action {
        final String id;
        final ActionType actionType;
        final long time;

        public Action(String id, ActionType actionType) {
            this.id = id;
            this.actionType = actionType;
            this.time = System.currentTimeMillis();
        }
    }

    public enum ActionType {
        TSDTV_PLAY,
        FUNCTION
    }

    public class FloodException extends Exception {}

}
