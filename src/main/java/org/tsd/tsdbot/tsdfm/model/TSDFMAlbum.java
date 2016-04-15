package org.tsd.tsdbot.tsdfm.model;

import java.io.File;
import java.util.TreeSet;

public class TSDFMAlbum {

    private final File albumDirectory;
    private final File albumImage;
    private final TSDFMArtist artist;

    private final TreeSet<TSDFMSong> songs = new TreeSet<>();

    public TSDFMAlbum(File albumDirectory, TSDFMArtist artist) {
        this.albumDirectory = albumDirectory;
        this.artist = artist;
        File image = new File(albumDirectory, ".img");
        this.albumImage = image.exists() ? image : null;
    }

    public File getAlbumDirectory() {
        return albumDirectory;
    }

    public File getAlbumImage() {
        return albumImage;
    }

    public TSDFMArtist getArtist() {
        return artist;
    }

    public TreeSet<TSDFMSong> getSongs() {
        return songs;
    }

    public String getName() {
        return albumDirectory.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDFMAlbum that = (TSDFMAlbum) o;

        return albumDirectory.equals(that.albumDirectory);

    }

    @Override
    public int hashCode() {
        return albumDirectory.hashCode();
    }
}
