package org.tsd.tsdbot.tsdfm.model;

import org.tsd.tsdbot.tsdfm.TSDFMLibrary;

import java.io.File;
import java.util.TreeSet;

public class TSDFMAlbum implements Comparable<TSDFMAlbum> {

    private final File albumDirectory;
    private final File albumImage;
    private final TSDFMArtist artist;

    private final TreeSet<TSDFMSong> songs = new TreeSet<>();

    public TSDFMAlbum(File albumDirectory, TSDFMArtist artist) {
        this.albumDirectory = albumDirectory;
        this.artist = artist;
        File image = new File(albumDirectory, ".img");
        this.albumImage = image.exists() ? image : null;

        for(File file : albumDirectory.listFiles()) {
            if(!file.isDirectory()) {
                String filename = file.getName();
                int idx = filename.lastIndexOf(".");
                if(idx > 0 && TSDFMLibrary.allowedFileTypes.contains(filename.substring(idx+1).toLowerCase())) {
                    songs.add(new TSDFMSong(file, this, artist));
                }
            }
        }
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

    @Override
    public int compareTo(TSDFMAlbum o) {
        return getName().compareToIgnoreCase(o.getName());
    }
}
