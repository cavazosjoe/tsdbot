package org.tsd.tsdbot.tsdfm.model;

import org.tsd.tsdbot.tsdfm.TSDFMLibrary;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class TSDFMArtist implements Comparable<TSDFMArtist> {

    private final File artistDirectory;
    private final String name;
    private final File artistImage;

    private Set<TSDFMAlbum> albums = new TreeSet<>();
    private Set<TSDFMSong> songsNoAlbum = new TreeSet<>();

    public TSDFMArtist(File artistDirectory) {
        this.artistDirectory = artistDirectory;
        this.name = artistDirectory.getName();
        File image = new File(artistDirectory, ".img");
        artistImage = image.exists() ? image : null;

        for(File file : artistDirectory.listFiles()) {
            if(file.isDirectory()) {
                albums.add(new TSDFMAlbum(file, this));
            } else {
                String filename = file.getName();
                int idx = filename.lastIndexOf(".");
                if(idx > 0 && TSDFMLibrary.allowedFileTypes.contains(filename.substring(idx+1).toLowerCase())) {
                    songsNoAlbum.add(new TSDFMSong(file, null, this));
                }
            }
        }
    }

    public Set<TSDFMAlbum> getAlbums() {
        return albums;
    }

    public Set<TSDFMSong> getSongsNoAlbum() {
        return songsNoAlbum;
    }

    public Set<TSDFMSong> getAllSongs() {
        Set<TSDFMSong> songs = new HashSet<>();
        songs.addAll(songsNoAlbum);
        for(TSDFMAlbum album : albums) {
            songs.addAll(album.getSongs());
        }
        return songs;
    }

    public File getArtistDirectory() {
        return artistDirectory;
    }

    public String getName() {
        return name;
    }

    public File getArtistImage() {
        return artistImage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDFMArtist that = (TSDFMArtist) o;

        return artistDirectory.equals(that.artistDirectory);

    }

    @Override
    public int hashCode() {
        return artistDirectory.hashCode();
    }

    @Override
    public String toString() {
        return "TSDFMArtist{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public int compareTo(TSDFMArtist o) {
        return this.name.compareToIgnoreCase(o.getName());
    }
}
