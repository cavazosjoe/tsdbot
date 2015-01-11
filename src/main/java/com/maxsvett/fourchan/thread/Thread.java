package com.maxsvett.fourchan.thread;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.maxsvett.fourchan.board.Board;
import com.maxsvett.fourchan.post.OriginalPost;
import com.maxsvett.fourchan.post.Post;

/**
 * A 4chan thread
 * 
 * @author MaxSvett
 */
public class Thread {
	
	private Board board;
	private OriginalPost op;
	private Post[] posts;
	
	public Thread(final Board board, final JsonArray jsonPosts) {
		this.board = board;
		this.posts = new Post[jsonPosts.size()];
		
		for (int i = 0; i < jsonPosts.size(); i++) {
			final JsonObject jsonPost = (JsonObject) jsonPosts.get(i);
			if (i == 0) {
				final OriginalPost op = new OriginalPost(board, jsonPost);
				this.posts[i] = op;
				this.op = op;
			} else {
				final Post p = new Post(board, jsonPost);
				this.posts[i] = p;
			}
		}
	}

	public OriginalPost getOP() {
		return op;
	}

	public Post[] getPosts() {
		return posts;
	}

	public Board getBoard() {
		return board;
	}
}