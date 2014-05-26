package com.maxsvett.fourchan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maxsvett.fourchan.board.Board;
import com.maxsvett.fourchan.page.Page;
import com.maxsvett.fourchan.post.SubmitPost;
import com.maxsvett.fourchan.thread.Thread;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * 4chan interaction
 * 
 * @author MaxSvett
 */
public class FourChan {
	
	private static final JsonParser PARSER = new JsonParser();
	private static final int TIMEOUT = 10000;
	
	/**
	 * Submit a post to a thread on 4chan.
	 * If the method returns true the post was posted
	 * successfully, if it returns false an error
	 * has occurred.
	 * 
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static boolean submitPost(final SubmitPost post) {
		if (post.getBoard() == null) {
			throw new IllegalArgumentException("post.getBoard() returned null");
		}
		if ("".equals(post.getCaptchaChallenge())) {
			throw new IllegalArgumentException("Missing captcha challenge");
		}
		if ("".equals(post.getCaptchaResponse())) {
			throw new IllegalArgumentException("Missing captcha response");
		}
		if ("".equals(post.getComment())) {
			throw new IllegalArgumentException("Missing post comment");
		}
		if (post.getThreadNo() == 0L) {
			throw new IllegalArgumentException("Invalid thread No.");
		}
		
		final String postLink = "https://sys.4chan.org" + post.getBoard().getPath() + "post";
		final HttpPost httpPost = new HttpPost(postLink);
		final MultipartEntity mpEntity = new MultipartEntity();
		
		try {
			mpEntity.addPart("name", new StringBody(post.getName()));
			mpEntity.addPart("email", new StringBody(post.getEmail()));
			mpEntity.addPart("sub", new StringBody(post.getSubject()));
			mpEntity.addPart("com", new StringBody(post.getComment()));
			mpEntity.addPart("resto", new StringBody(String.valueOf(post.getThreadNo())));
			mpEntity.addPart("recaptcha_challenge_field", new StringBody(post.getCaptchaChallenge()));
			mpEntity.addPart("recaptcha_response_field", new StringBody(post.getCaptchaResponse()));
			mpEntity.addPart("mode", new StringBody("regist"));
			mpEntity.addPart("pwd", new StringBody(""));
		} catch (UnsupportedEncodingException e) {
			return false;
		}
		
		if (post.getImageFile() != null && post.getImageFile().exists()) {
			mpEntity.addPart("upfile", new FileBody(post.getImageFile()));
		}
		
		httpPost.setEntity(mpEntity);
		
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
		
		final HttpClient client = new DefaultHttpClient(httpParams);
		client.getParams().setBooleanParameter("http.protocol.expect-continue", false);
		
		HttpResponse response = null;
		
		boolean status = true;
		try {
			response = client.execute(httpPost);
		} catch (ClientProtocolException e) {
			status = false;
		} catch (IOException e) {
			status = false;
		}
		
		if (response.getStatusLine().getStatusCode() == 200) {
			try {
				HttpEntity entity = response.getEntity();
				String htmlResponse = EntityUtils.toString(entity);
				if (!htmlResponse.substring(120, 300).contains("Post successful!")) {
					status = false;
				}
			} catch (ParseException e) {
				status = false;
			} catch (IOException e) {
				status = false;
			}
		} else {
			status = false;
		}
		
		client.getConnectionManager().shutdown();
		
		return status;
	}
	
	/**
	 * Parse a json page string.
	 * 
	 * @param board
	 * @param json
	 * @return
	 */
	public static Page parsePage(Board board, String json) {
		JsonObject jsonObject = (JsonObject) PARSER.parse(json);
		JsonArray jsonThreads = jsonObject.getAsJsonArray("threads");
		return new Page(board, jsonThreads);
	}
	
	
	/**
	 * Parse a json thread string.
	 * 
	 * @param board
	 * @param json
	 * @return
	 */
	public static Thread parseThread(Board board, String json) {
		JsonObject jsonObject = (JsonObject) PARSER.parse(json);
		JsonArray jsonPosts = jsonObject.getAsJsonArray("posts");
		return new Thread(board, jsonPosts);
	}
	
	/**
	 * Get the URL of a 4chan json page. A page is a an array of threads.
	 * 
	 * @param board
	 * @param index
	 * @return
	 */
	public static URL getJsonPageURL(Board board, int index) {
		if (index < 0 || index > 15)
			throw new IllegalArgumentException("index has to have a value between 0 and 15");
		URL url = null;
		try {
			url = new URL(String.format(Locale.US, "http://api.4chan.org%s%d.json",
					board.getPath(), index));
		} catch (MalformedURLException e) {
			return null;
		}
		return url;
	}

	/**
	 * Get the URL of a json thread. A thread is an array of posts.
	 * 
	 * @param board
	 * @param no
	 * @return
	 */
	public static URL getJsonThreadURL(Board board, int no) {
		URL url = null;
		try {
			url = new URL(String.format(Locale.US, "http://api.4chan.org%sres/%d.json",
					board.getPath(), no));
		} catch (MalformedURLException e) {
			return null;
		}
		return url;
	}
}
