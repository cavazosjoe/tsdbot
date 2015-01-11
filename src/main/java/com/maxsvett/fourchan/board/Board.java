package com.maxsvett.fourchan.board;

import java.util.Locale;

/**
 * Represents a board on 4chan
 * 
 * @author MaxSvett
 */
public class Board {
	
	private String name;
	private String path;
    private boolean nsfw; /*schoolyd*/
	
	/**
	 * Name should be the name of the board (/b/ is Random, /g/ is Technology)
	 * <br>
	 * Path should be the board path, eg. /b/, /g/, /tv/
	 * <br>
	 * <b>Remember the forward slashes</b>
	 * 
	 * @param name
	 * @param path
	 */
	public Board(String name, String path, boolean nsfw) {
		this.name = name;
		this.path = path;
        this.nsfw = nsfw;
	}

    public boolean isNsfw() {
        return nsfw;
    }

    public String getName() {
		return name;
	}
	
	public String getPath() {
		return path;
	}
	
	@Override
	public String toString() {
		return String.format(Locale.US, "%s - %s", path, name);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Board) {
			return ((Board) obj).getPath().equals(this.getPath());
		} else {
			return false;
		}
	}

    public static Board getBoard(String path) {
        for(Board board : BOARDS) {
            if(board.getPath().equals(path)) return board;
        }
        return null;
    }
	
	/**
	 * All the boards on 4chan supported by the API
	 */
	public static final Board[] BOARDS = new Board[]
			{
				new Board("3DCG", "/3/",false),
				new Board("Advice", "/adv/",false),
				new Board("Alternative sports", "/asp/",false),
				new Board("Animals & Nature", "/an/",false),
				new Board("Animated gif", "/gif/",true),
				new Board("Anime & Manga", "/a/",false),
				new Board("Anime/Cute", "/c/",false),
				new Board("Anime/Wallpapers", "/w/",false),
				new Board("Artwork/Critique", "/ic/",false),
				new Board("Auto", "/o/",false),
				new Board("Comics & Cartoons", "/co/",false),
				new Board("Cosplay & CGL", "/cgl/",false),
				new Board("Cute/Male", "/cm/",false),
				new Board("Discussion", "/q/",false),
				new Board("Do-It-Yourself", "/diy/",false),
				new Board("Ecchi", "/e/",true),
				new Board("Fashion", "/fa/",false),
				new Board("Food & Cooking", "/ck/",false),
				new Board("Graphic design", "/gd/",false),
				new Board("Handsome men", "/hm/",true),
				new Board("Hardcore", "/hc/",true),
				new Board("Health & Fitness", "/fit/",false),
				new Board("Hentai", "/h/",true),
				new Board("Hentai/Alternative", "/d/",true),
				new Board("High resolution", "/hr/",true),
				new Board("International", "/int/",false),
				new Board("LGBT", "/lgbt/",false),
				new Board("Literature", "/lit/",false),
				new Board("Mecha", "/m/",false),
				new Board("Music", "/mu/",false),
				new Board("Oekaki", "/i/",false),
				new Board("Otaku culture", "/jp/",false),
				new Board("Outdoors", "/out/",false),
				new Board("Papercraft & Origami", "/po/",false),
				new Board("Paranormal", "/x/",false),
				new Board("Photography", "/p/",false),
				new Board("Pok�mon", "/vp/",false),
				new Board("Politically incorrect", "/pol/",true),
				new Board("Pony", "/mlp/",false),
				new Board("Random", "/b/",true),
				new Board("Request", "/r/",true),
				new Board("Retro games", "/vr/",false),
				new Board("Robot 9001", "/r9k/",true),
				new Board("Sexy beautiful women", "/s/",true),
				new Board("Shit 4chan says", "/s4s/",true),
				new Board("Social", "/soc/",true),
				new Board("Sports", "/sp/",false),
				new Board("Technology", "/g/",false),
				new Board("Television & Film", "/tv/",false),
				new Board("Torrents", "/t/",true),
				new Board("Toys", "/toy/",false),
				new Board("Traditional games", "/tg/",false),
				new Board("Transportation", "/n/",false),
				new Board("Travel", "/trv/",false),
				new Board("Video games", "/v/",false),
				new Board("Video games general", "/vg/",false),
				new Board("Wallpapers/General", "/wg/",false),
				new Board("Weapons", "/k/",false),
				new Board("Worksafe gif", "/wsg/",false),
				new Board("Yaoi", "/y/",true),
				new Board("Yuri", "/u/", true)
			};
}