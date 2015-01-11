package com.maxsvett.fourchan.post;

import com.maxsvett.fourchan.board.Board;

import java.io.File;

/**
 * Passed to {@link #SubmitPost()}
 * 
 * @author MaxSvett
 */
public class SubmitPost {
	
	private Board board;
	private String subject;
	private String comment;
	private String email;
	private String name;
	private String captchaChallenge;
	private String captchaResponse;
	private long threadNo;
	private File imageFile;
	
	public SubmitPost() {
		this.board = null;
		this.subject = "";
		this.comment = "";
		this.email = "";
		this.name = "";
		this.captchaChallenge = "";
		this.captchaResponse = "";
		this.threadNo = 0L;
		this.imageFile = null;
	}

	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCaptchaChallenge() {
		return captchaChallenge;
	}

	public void setCaptchaChallenge(String captchaChallenge) {
		this.captchaChallenge = captchaChallenge;
	}

	public String getCaptchaResponse() {
		return captchaResponse;
	}

	public void setCaptchaResponse(String captchaResponse) {
		this.captchaResponse = captchaResponse;
	}

	public long getThreadNo() {
		return threadNo;
	}

	public void setThreadNo(long threadNo) {
		this.threadNo = threadNo;
	}

	public File getImageFile() {
		return imageFile;
	}

	public void setImageFile(File imageFile) {
		this.imageFile = imageFile;
	}
}
