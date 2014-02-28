package com.maxsvett.fourchan.post;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

import com.google.gson.JsonObject;
import com.maxsvett.fourchan.board.Board;

/**
 * A 4chan post
 * 
 * @author MaxSvett
 */
public class Post {

	protected Board board;
	protected long no = 0L;
	protected Calendar time = null;
	protected String subject = "";
	protected String name = "";
	protected String tripcode = "";
	protected String email = "";
	protected String capcode = "";
	protected String countryCode = "";
	protected String countryName = "";
	protected String comment = "";
	protected long imageId = 0L;
	protected String imageName = "";
	protected String imageExtension = "";

	public Post(final Board board, final JsonObject jsonPost) {
		this.board = board;

		Calendar calendar = Calendar.getInstance();

		if (jsonPost.get("no") != null)
			this.no = jsonPost.get("no").getAsLong();

		if (jsonPost.get("time") != null) {
			calendar.setTimeInMillis(1000L * jsonPost.get("time").getAsLong());
			this.time = calendar;
		}

		if (jsonPost.get("sub") != null)
			this.subject = jsonPost.get("sub").getAsString();

		if (jsonPost.get("name") != null)
			this.name = jsonPost.get("name").getAsString();

		if (jsonPost.get("trip") != null)
			this.tripcode = jsonPost.get("trip").getAsString();

		if (jsonPost.get("email") != null)
			this.email = jsonPost.get("email").getAsString();

		if (jsonPost.get("capcode") != null)
			this.capcode = jsonPost.get("capcode").getAsString();

		if (jsonPost.get("country") != null)
			this.countryCode = jsonPost.get("country").getAsString();

		if (jsonPost.get("country_name") != null)
			this.countryName = jsonPost.get("country_name").getAsString();

		if (jsonPost.get("com") != null)
			this.comment = jsonPost.get("com").getAsString();

		if (jsonPost.get("tim") != null)
			this.imageId = jsonPost.get("tim").getAsLong();

		if (jsonPost.get("filename") != null)
			this.imageName = jsonPost.get("filename").getAsString();

		if (jsonPost.get("ext") != null)
			this.imageExtension = jsonPost.get("ext").getAsString();
	}

	public Board getBoard() {
		return board;
	}

	public long getNo() {
		return no;
	}

	public Calendar getTime() {
		return time;
	}

	public String getSubject() {
		return subject;
	}

	public String getName() {
		return name;
	}

	public String getTripcode() {
		return tripcode;
	}

	public String getEmail() {
		return email;
	}

	public String getCapcode() {
		return capcode;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getCountryName() {
		return countryName;
	}

	public String getComment() {
		return comment;
	}

	/**
	 * Returns a link to an image of where the poster is from (null if an error
	 * occurs). <br>
	 * <b>Works only on certain boards (eg. /int/).</b>
	 * 
	 * @return
	 */
	public URL getCountryImageURL() {
		if ("".equals(countryName))
			return null;
		final String link = String.format(Locale.US,
				"http://static.4chan.org/image/country/%s.gif", countryCode);
		URL url = null;
		try {
			url = new URL(link);
		} catch (MalformedURLException e) {
			return null;
		}
		return url;
	}

	/**
	 * Returns a link to the image in the post (null if there is no image).
	 * 
	 * @return
	 */
	public URL getImageURL() {
		if (imageId == 0L)
			return null;
		final String link = String.format(Locale.US,
				"http://images.4chan.org%ssrc/%d%s", board.getPath(), imageId,
				imageExtension);
		URL url = null;
		try {
			url = new URL(link);
		} catch (MalformedURLException e) {
			return null;
		}
		return url;
	}
	
	/**
	 * Returns link to the thumbnail. A thumbnail is much smaller in size
	 * compared to an image which makes it very suitable over slow connections.
	 * 
	 * @return
	 */
	public URL getThumbnailURL() {
		if (imageId == 0L)
			return null;
		final String link = String.format(Locale.US,
				"http://0.thumbs.4chan.org%sthumb/%ds%s", board.getPath(),
				imageId, imageExtension);
		URL url = null;
		try {
			url = new URL(link);
		} catch (MalformedURLException e) {
			return null;
		}
		return url;
	}

	public String getImageName() {
		return imageName;
	}

	public String getImageId() {
		return String.valueOf(imageId);
	}

	public boolean isSage() {
		return email.equals("sage");
	}

	public boolean hasSubject() {
		return !"".equals(subject);
	}

	public boolean hasTripcode() {
		return !"".equals(tripcode);
	}

	public boolean hasImage() {
		return imageId != 0L;
	}

	public boolean hasCountryImage() {
		return !"".equals(countryName);
	}
}
