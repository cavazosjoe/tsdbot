package org.tsd.tsdbot.util;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
        int occurrences = 0;
        for(Action a : actionHistory.get(id)) {
            historyStart = Math.min(a.time, historyStart);
            if(a.actionType.equals(type)) {
                occurrences++;
            }
        }

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
        long now = System.currentTimeMillis();
        if(actionHistory.containsKey(id)) {
            Iterator<Action> it = actionHistory.get(id).iterator();
            while(it.hasNext()) {
                Action a = it.next();
                if(now - a.time > actionHistoryLength) {
                    it.remove();
                }
            }
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
