package com.maxsvett.fourchan;

import java.util.Scanner;

import com.maxsvett.fourchan.board.Board;
import com.maxsvett.fourchan.captcha.Captcha;
import com.maxsvett.fourchan.post.SubmitPost;

public class Testing {
	
	public static void main(String[] args) {
		/*
		 *	Example of how to post to a thread
		 *	with the FourChan.submitPost method 
		 */
		
		Scanner scnr = new Scanner(System.in);
		
		SubmitPost post = new SubmitPost();
		post.setBoard(new Board("Technology", "/g/", false));
		post.setThreadNo(34260042);
		post.setName("Anonymous");
		post.setComment("Testing123");
		
		Captcha captcha = Captcha.newChallenge();
		System.out.println("Captcha image: " + captcha.getImageURL().toString());
		System.out.print(">");
		String captchaResponse = scnr.nextLine();
		
		post.setCaptchaChallenge(captcha.getChallenge());
		post.setCaptchaResponse(captchaResponse);
		
		if (FourChan.submitPost(post)) {
			System.out.println("Post submitted successfully");
		} else {
			System.out.println("An error occured");
		}
		
		scnr.close();
	}
}
