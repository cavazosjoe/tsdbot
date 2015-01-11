package com.maxsvett.fourchan.post;

import com.google.gson.JsonObject;
import com.maxsvett.fourchan.board.Board;

/**
 * The first post in a 4chan thread.
 * An {@link OriginalPost} contains more information
 * compared to a {@link Post} object.
 * 
 * @author MaxSvett
 */
public class OriginalPost extends Post {
	
	private int replyCount;
	private int imageCount;
	private int postsOmitted;
	private int imagesOmitted;
	private boolean isSticky;
	
	public OriginalPost(final Board board, final JsonObject jsonPost) {
		super(board, jsonPost);
		
		if (jsonPost.get("replies") != null)
			this.replyCount = jsonPost.get("replies").getAsInt();
		
		if (jsonPost.get("images") != null)
			this.imageCount = jsonPost.get("images").getAsInt();
		
		if (jsonPost.get("omitted_posts") != null)
			this.postsOmitted = jsonPost.get("omitted_posts").getAsInt();
		
		if (jsonPost.get("omitted_images") != null)
			this.imagesOmitted = jsonPost.get("omitted_images").getAsInt();
		
		if (jsonPost.get("sticky") != null)
			this.isSticky = jsonPost.get("sticky").getAsInt() != 0;
	}

	public int getReplyCount() {
		return replyCount;
	}

	public int getImageCount() {
		return imageCount;
	}

	public int getPostsOmitted() {
		return postsOmitted;
	}

	public int getImagesOmitted() {
		return imagesOmitted;
	}

	public boolean isSticky() {
		return isSticky;
	}
}
