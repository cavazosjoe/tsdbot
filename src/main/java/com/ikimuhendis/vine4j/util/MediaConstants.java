package com.ikimuhendis.vine4j.util;

/**
 * Created by Joe on 4/23/2015.
 */
public class MediaConstants {

    /*
     * Additional stuff for uploading data
     */
    public static final String VINE_MEDIA_UPLOAD_BASE_URL = "https://media.vineapp.com/upload";
    public static final String VINE_MEDIA_UPLOAD_THUMB = VINE_MEDIA_UPLOAD_BASE_URL+"/thumbs/%s";
    public static final String VINE_MEDIA_UPLOAD_VIDEO = VINE_MEDIA_UPLOAD_BASE_URL+"/videos/%s";

    public static final String VINE_MEDIA_HTTPHEADER_HOST_TEXT = "Host";
    public static final String VINE_MEDIA_HTTPHEADER_HOST = "media.vineapp.com";

    public static final String VINE_MEDIA_HTTPHEADER_PROXY_TEXT = "Proxy-Connection";
    public static final String VINE_MEDIA_HTTPHEADER_PROXY = "keep-alive";

    public static final String VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_TEXT = "Content-Type";
    public static final String VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_IMG = "image/jpeg";
    public static final String VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_VIDEO = "video/mp4";

    public static final String VINE_MEDIA_HTTPHEADER_VINE_CLIENT_TEXT = "X-Vine-Client";
    public static final String VINE_MEDIA_HTTPHEADER_VINE_CLIENT = "ios/1.0.5"; // or 1.3.1?

    public static final String VINE_MEDIA_HTTPHEADER_LENGTH_TEXT = "Content-Length";

    public static final String VINE_MEDIA_HTTPHEADER_ACCEPT_ENCODING = "gzip, deflate";

}
