package com.maxsvett.fourchan.captcha;

import com.maxsvett.fourchan.Util;

import java.net.MalformedURLException;
import java.net.URL;

public class Captcha {
	
	private String challenge;
	private URL imageURL;
	
	private Captcha(String challenge, URL imageURL) {
		this.challenge = challenge;
		this. imageURL = imageURL;
	}
	
	public String getChallenge() {
		return challenge;
	}
	
	public URL getImageURL() {
		return imageURL;
	}
	
	public static Captcha newChallenge() {
		final String CHALLENGE = "http://api.recaptcha.net/challenge?k=6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";
		URL url = null;
		URL imageURL = null;
		try {
			url = new URL(CHALLENGE);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		final String source = Util.downloadString(url);
		final String challenge = source.split("'")[3];
		try {
			imageURL = new URL("http://www.google.com/recaptcha/api/image?c=" + challenge);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return new Captcha(challenge, imageURL);
	}
}
