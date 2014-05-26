package com.maxsvett.fourchan;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URL;

public class Util {
	
	/**
	 * Downloads the source of a webpage.
	 * 
	 * @param url
	 * @return
	 */
	public static String downloadString(final URL url) {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url.toString());
		ResponseHandler<String> response = new BasicResponseHandler();
		String responseBody = null;
		try {
			responseBody = client.execute(get, response);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}
		return responseBody;
	}
}
