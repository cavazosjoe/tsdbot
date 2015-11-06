package org.tsd.tsdbot.haloapi.model.stats;

import java.util.List;

public class MetaCommendationDelta {

    // The commendation ID. Commendations are available via the Metadata API.
    String Id;

    // The progress the player had made towards the commendation level before the
    // match. In C#, this can be reassembled into a Guid in the following manner:
    // new Guid((int)Data1, (short)Data2, (short)Data3,
    // BitConverter.GetBytes((long)Data4)).
    List<MetRequirement> PreviousMetRequirements;

    // The progress the player had made towards the commendation level after the
    // match. In C#, this can be reassembled into a Guid in the following manner:
    // new Guid((int)Data1, (short)Data2, (short)Data3,
    // BitConverter.GetBytes((long)Data4)).
    List<MetRequirement> MetRequirements;
}
