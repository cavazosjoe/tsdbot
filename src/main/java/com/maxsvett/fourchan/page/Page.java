package com.maxsvett.fourchan.page;

import com.google.gson.JsonArray;
import com.maxsvett.fourchan.board.Board;
import com.maxsvett.fourchan.thread.Thread;

/**
 * A page on 4chan
 * 
 * @author MaxSvett
 */
public class Page {
	
	private Board board;
	private Thread[] threads;
	
	public Page(final Board board, final JsonArray jsonThreads) {
		this.board = board;
		this.threads = new Thread[jsonThreads.size()];
		
		for (int i = 0; i < jsonThreads.size(); i++) {
			final JsonArray jsonPosts = jsonThreads.get(i)
					.getAsJsonObject().getAsJsonArray("posts");
			threads[i] = new Thread(board, jsonPosts);
		}
	}
	
	public Thread[] getThreads() {
		return threads;
	}
	
	public Board getBoard() {
		return board;
	}
}
