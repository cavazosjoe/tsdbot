package org.tsd.tsdbot.tsdfm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TSDFMBlock {

    private final String id;
    private final String name;
    private final String intro;
    private final int duration; // minutes
    private final Set<String> tagsToPlay = new HashSet<>();

    public TSDFMBlock(String id, String name, String intro, int duration, String... tags) {
        this.id = id;
        this.name = name;
        this.intro = intro;
        this.duration = duration;
        this.tagsToPlay.addAll(Arrays.asList(tags));
    }

    public int getDuration() {
        return duration;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIntro() {
        return intro;
    }

    public Set<String> getTagsToPlay() {
        return tagsToPlay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDFMBlock that = (TSDFMBlock) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
