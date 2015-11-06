package org.tsd.tsdbot.haloapi.model.stats;

import org.tsd.tsdbot.haloapi.model.Count;
import org.tsd.tsdbot.haloapi.model.Timelapse;

import java.util.List;

public class FlexibleStats {

    // The set of flexible stats that are derived from medal events.
    List<Count> MedalStatCounts;

    // The set of flexible stats that are derived from impulse events.
    List<Count> ImpulseStatCounts;

    // The set of flexible stats that are derived from medal time lapses.
    List<Timelapse> MedalTimelapses;

    // The set of flexible stats that are derived from impulse time lapses.
    List<Timelapse> ImpulseTimelapses;

}
