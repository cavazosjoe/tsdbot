package org.tsd.tsdbot.tsdfm.model;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.tsd.tsdbot.util.TSDFMUtil;

import java.io.File;

public class TSDFMSong implements Comparable<TSDFMSong> {

    private final File musicFile;
    private final String title;
    private final TSDFMAlbum album;
    private final TSDFMArtist artist;

    private Integer songNumber = null;

    public TSDFMSong(File musicFile, TSDFMAlbum album, TSDFMArtist artist) {
        this.musicFile = musicFile;
        this.album = album;
        this.artist = artist;
        this.title = TSDFMUtil.getSanitizedSongName(musicFile);
        //TSDFMUtil.getSongNumber(musicFile);
    }

    public File getMusicFile() {
        return musicFile;
    }

    public String getTitle() {
        return title;
    }

    public TSDFMAlbum getAlbum() {
        return album;
    }

    public TSDFMArtist getArtist() {
        return artist;
    }

    public Integer getSongNumber() {
        return songNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDFMSong tsdfmSong = (TSDFMSong) o;

        return musicFile.equals(tsdfmSong.musicFile);
    }

    @Override
    public int hashCode() {
        return musicFile.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TSDFMSong{ path=").append(musicFile.getAbsolutePath());
        if(album != null)
            sb.append(" ; album=").append(album.getName());
        sb.append(" ; artist=").append(artist.getName()).append(" }");
        return sb.toString();
    }

    @Override
    public int compareTo(TSDFMSong o) {
        return new CompareToBuilder()
                .append(this.songNumber, o.getSongNumber())
                .append(this.title.toLowerCase(), o.getTitle().toLowerCase())
                .build();
    }
}
