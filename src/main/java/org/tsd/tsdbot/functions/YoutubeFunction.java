package org.tsd.tsdbot.functions;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;
import org.tsd.tsdbot.history.*;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.tsdtv.model.TSDTVShow;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * Created by Joe on 4/24/2015.
 */
@Singleton
@Function(initialRegex = "^\\.smush$")
public class YoutubeFunction extends MainFunctionImpl {

    private static final Logger log = LoggerFactory.getLogger(YoutubeFunction.class);

    private static final int videoLength = 9;
    private static final int numClips = 3;
    private static final double clipDuration = videoLength/numClips;

    private MessageFilter messageFilter = null;

    private YouTube youTube;
    private HistoryBuff historyBuff;
    private TSDTVLibrary library;
    private Random random;
    private String ffmpegExec;
    private ExecutorService executorService;
    private InjectableMsgFilterStrategyFactory msgFilterFact;

    @Inject
    public YoutubeFunction(
            Bot bot,
            HistoryBuff historyBuff,
            ExecutorService executorService,
            InjectableMsgFilterStrategyFactory msgFilterFact,
            Random random,
            TSDTVLibrary library,
            YouTube youTube,
            @Named("ffmpegExec") String ffmpegExec) {
        super(bot);
        this.msgFilterFact = msgFilterFact;
        this.random = random;
        this.library = library;
        this.ffmpegExec = ffmpegExec;
        this.youTube = youTube;
        this.historyBuff = historyBuff;
        this.executorService = executorService;
    }

    @Override
    public void run(final String channel, String sender, String ident, String text) {

        if(messageFilter == null) {
            NoCommandsStrategy noCmdStrat = new NoCommandsStrategy();
            msgFilterFact.injectStrategy(noCmdStrat);
            LengthStrategy lengthStrat = new LengthStrategy(null, 50);
            messageFilter = MessageFilter.create().addFilter(noCmdStrat).addFilter(lengthStrat);
        }

        Runnable youtubeThread = new Runnable() {
            @Override
            public void run() {
                HashSet<File> clips = new HashSet<>();
                File clipList = null;
                try {

                    List<TSDTVShow> allShows = library.getAllShows();

                    for (int i = 0; i < numClips; i++) try {
                        TSDTVShow randomShow = allShows.get(random.nextInt(allShows.size()));
                        TSDTVEpisode randomEpisode = randomShow.getRandomEpisode(random);
                        log.info("selected random episode: {}", randomEpisode.getFile().getAbsolutePath());

                        long durationInMillis = randomEpisode.getDuration(ffmpegExec);
                        int durationInSeconds = (int) (durationInMillis / 1000);
                        int opEdBufferInSeconds = 200; // so we pull openings and endings less often
                        int clipStartSeconds = opEdBufferInSeconds + random.nextInt(durationInSeconds - (opEdBufferInSeconds+videoLength));
                        log.info("clipStartSeconds = {}", clipStartSeconds);

                        // ffmpeg -ss [clipStart] -i /path/to/movie.mp4 -t [durationInSeconds] -c:v libx264 -y /path/to/out.mp4
                        File tempFile = File.createTempFile(RandomStringUtils.randomAlphanumeric(10), ".mp4");
                        log.info("writing clip to temp file {}", tempFile.getAbsolutePath());
                        ProcessBuilder pb = new ProcessBuilder(
                                ffmpegExec,
                                "-ss", clipStartSeconds+"",
                                "-i", randomEpisode.getFile().getAbsolutePath(),
                                "-t", clipDuration + "",
                                "-map", "0:0",
                                "-map", "0:1",
                                "-c:v", "libx264",
                                "-r", "24",
                                "-b:v", "1048k",
                                "-pix_fmt", "yuv420p",
                                "-vf", "scale=480:480",
//                        "-flags:v", "+global_header",
                                "-preset", "veryslow",
                                "-ac", "1",
                                "-c:a", "aac",
                                "-strict", "experimental",
//                        "-flags:a", "+global_header",
                                "-ar", "44100",
                                "-b:a", "128k",
                                "-y",
                                tempFile.getAbsolutePath());
                        Process p = pb.start();
                        p.waitFor();
                        if(p.exitValue() != 0)
                            throw new Exception("Error creating clip!");
                        log.info("successfully clipped");
                        clips.add(tempFile);
                    } catch (Exception e) {
                        log.error("Failed to process clip, SKIPPING...", e);
                    }

                    clipList = File.createTempFile(RandomStringUtils.randomAlphanumeric(10), ".txt");
                    PrintWriter writer = new PrintWriter(clipList, "UTF-8");
                    for(File clip : clips)
                        writer.println("file '" + clip.getAbsolutePath() + "'");
                    writer.close();
                    log.info("clip list written to {}, combining videos...", clipList.getAbsolutePath());

                    File combinedVid = File.createTempFile(RandomStringUtils.randomAlphanumeric(10), ".mp4");

                    ProcessBuilder pb = new ProcessBuilder(
                            ffmpegExec,
                            "-f", "concat",
                            "-i", clipList.getAbsolutePath(),
                            "-c", "copy",
                            "-y",
                            combinedVid.getAbsolutePath()
                    );
                    Process p = pb.start();
                    p.waitFor();
                    logStream(p);
                    p.destroy();

                    log.info("successfully combined video, uploading to the youtubes...");

                    HistoryBuff.Message m = historyBuff.getRandomFilteredMessage(channel, null, messageFilter);
                    String title = (m != null) ? m.text : RandomStringUtils.randomAlphanumeric(10);

                    m = historyBuff.getRandomFilteredMessage(channel, null, messageFilter);
                    String description = (m != null) ? m.text : RandomStringUtils.randomAlphanumeric(10);

                    Video returnedVideo = uploadToYoutube(title, description, combinedVid);
                    String videoId = returnedVideo.getId();

                    log.info("video uploaded, waiting to finish processing...");

//                    long timeoutMillis = 1000 * 120;
//                    long millisToSleep = 1000 * 5;
//                    long millisWaited = 0;
//                    String status;
//                    do {
//                        Thread.sleep(millisToSleep);
//                        millisWaited += millisToSleep;
//                        returnedVideo = youTube.videos().list("id="+videoId).execute().getItems().get(0);
//                        status = returnedVideo.getProcessingDetails().getProcessingStatus();
//                        log.info("PROCESSING STATUS = {}", status);
//                    } while(millisWaited < timeoutMillis && !"terminated".equalsIgnoreCase(status));

                    Thread.sleep(1000 * 25);

                    bot.sendMessage(channel, "https://www.youtube.com/watch?v=" + videoId);

                } catch (Exception e) {
                    log.error("Error creating youtube", e);
                    bot.sendMessage(channel, "Actually, nah");
                } finally {
                    log.info("deleting temp files...");
                    if(clipList != null)
                        clipList.delete();
                    for(File f : clips)
                        f.delete();
                }
            }
        };

        executorService.submit(youtubeThread);
        bot.sendMessage(channel, "Consider it done");

    }

    private Video uploadToYoutube(String title, String description, File combinedVid) throws IOException {
        Video video = new Video();

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public");
        video.setStatus(status);

        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        video.setSnippet(snippet);

        InputStreamContent streamContent = new InputStreamContent("video/mp4", new FileInputStream(combinedVid));

        YouTube.Videos.Insert videoInsert = youTube.videos()
                .insert("snippet,statistics,status", video, streamContent);

        MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(true);

        MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
            public void progressChanged(MediaHttpUploader uploader) throws IOException {
                switch (uploader.getUploadState()) {
                    case INITIATION_STARTED:
                        log.warn("Initiation Started");
                        break;
                    case INITIATION_COMPLETE:
                        log.warn("Initiation Completed");
                        break;
                    case MEDIA_IN_PROGRESS:
                        log.warn("Upload in progress");
                        log.warn("Bytes uploaded: " + uploader.getNumBytesUploaded());
                        break;
                    case MEDIA_COMPLETE:
                        log.warn("Upload Completed!");
                        break;
                    case NOT_STARTED:
                        log.warn("Upload Not Started!");
                        break;
                }
            }
        };
        uploader.setProgressListener(progressListener);

        return videoInsert.execute();
    }

//    private long createPost(String sessionKey, String vineId, File vid) throws Exception {
//
//        String thumbUrl = uploadThumbnail(sessionKey, vineId);
//        String vidUrl = uploadVideo(sessionKey, vineId, vid);
//
//        HashMap<String, String> postInfo = new HashMap<>();
//        postInfo.put("entities",        "");
//        postInfo.put("videoUrl",        vidUrl);
//        postInfo.put("thumbnailUrl",    thumbUrl);
//        postInfo.put("description",     "aaaaaaaaa");
//        HttpResponse postResponse = HttpUtil.post(Constants.VINE_API_POST_CREATE_PATH, sessionKey, postInfo);
//        InputStream is = HttpUtil.getInputStreamHttpResponse(postResponse);
//        JSONObject jsonResponse = (JSONObject) JSONUtil.fromJSONStream(is);
//        log.info("Create post response: {}", jsonResponse.toJSONString());
//        boolean success = JSONUtil.getBoolean(jsonResponse, "success");
//        if(!success)
//            throw new Exception(JSONUtil.getString(jsonResponse, "error"));
//        return (long) JSONUtil.getJSONObject(jsonResponse, "data").get("postId");
//    }
//
//    private String uploadThumbnail(String sessionKey, String vineId) throws IOException {
//        String thumbTarget = String.format(MediaConstants.VINE_MEDIA_UPLOAD_THUMB, vineId + ".mp4.jpg");
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        IOUtils.copy(Printout.class.getResourceAsStream("/vinethumb.jpg"), baos);
//
//        HttpEntity thumbEntity = new ByteArrayEntity(baos.toByteArray());
//        HttpPut thumbPut = new HttpPut(thumbTarget);
//        thumbPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_HOST_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_HOST);
//        thumbPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_PROXY_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_PROXY);
//        thumbPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_IMG);
//        thumbPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_VINE_CLIENT_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_VINE_CLIENT);
//        thumbPut.addHeader(Constants.VINE_HTTPHEADER_ACCEPT_LANGUAGE_TEXT, Constants.VINE_HTTPHEADER_ACCEPT_LANGUAGE);
//        thumbPut.addHeader(Constants.VINE_HTTPHEADER_ACCEPT_TEXT, Constants.VINE_HTTPHEADER_ACCEPT);
//        thumbPut.addHeader(Constants.VINE_HTTPHEADER_VINE_SESSION_ID, sessionKey);
//        thumbPut.addHeader(Constants.VINE_HTTPHEADER_ACCEPT_ENCODING_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_ACCEPT_ENCODING);
//        thumbPut.addHeader("Connection", "keep-alive");
//        thumbPut.addHeader(Constants.VINE_HTTPHEADER_USER_AGENT_TEXT, Constants.VINE_HTTPHEADER_USER_AGENT);
//
//        thumbPut.setEntity(thumbEntity);
//        HttpResponse response = httpClient.execute(thumbPut);
//        return response.getHeaders("X-Upload-Key")[0].getValue();
//    }
//
//    private String uploadVideo(String sessionKey, String vineId, File vid) throws IOException {
//        String vidTarget = String.format(MediaConstants.VINE_MEDIA_UPLOAD_VIDEO, vineId + ".mp4");
//        HttpEntity vidEntity = new FileEntity(vid);
//        HttpPut vidPut = new HttpPut(vidTarget);
//        vidPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_HOST_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_HOST);
//        vidPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_PROXY_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_PROXY);
//        vidPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_CONTENT_TYPE_VIDEO);
//        vidPut.addHeader(MediaConstants.VINE_MEDIA_HTTPHEADER_VINE_CLIENT_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_VINE_CLIENT);
//        vidPut.addHeader(Constants.VINE_HTTPHEADER_ACCEPT_LANGUAGE_TEXT, Constants.VINE_HTTPHEADER_ACCEPT_LANGUAGE);
//        vidPut.addHeader(Constants.VINE_HTTPHEADER_ACCEPT_TEXT, Constants.VINE_HTTPHEADER_ACCEPT);
//        vidPut.addHeader(Constants.VINE_HTTPHEADER_VINE_SESSION_ID, sessionKey);
//        vidPut.addHeader(Constants.VINE_HTTPHEADER_ACCEPT_ENCODING_TEXT, MediaConstants.VINE_MEDIA_HTTPHEADER_ACCEPT_ENCODING);
//        vidPut.addHeader("Connection", "keep-alive");
//        vidPut.addHeader(Constants.VINE_HTTPHEADER_USER_AGENT_TEXT, Constants.VINE_HTTPHEADER_USER_AGENT);
//
//        vidPut.setEntity(vidEntity);
//        HttpResponse response = httpClient.execute(vidPut);
//        return response.getHeaders("X-Upload-Key")[0].getValue();
//    }

    private void logStream(Process process) throws IOException {
        try(
                InputStream out = process.getErrorStream();
                InputStreamReader reader = new InputStreamReader(out);
                BufferedReader br = new BufferedReader(reader)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                log.debug(line);
            }
        }
    }
}
