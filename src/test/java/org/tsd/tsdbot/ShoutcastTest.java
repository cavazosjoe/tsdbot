package org.tsd.tsdbot;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;

@RunWith(MockitoJUnitRunner.class)
public class ShoutcastTest {

    @Test
    public void test() throws Exception {
        InputStream mp3 = new BufferedInputStream(new FileInputStream(new File("C:/Users/Joe/Music/outrun.mp3")));

//        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY,
//                new UsernamePasswordCredentials("source", "1-Enter!"));
//        CloseableHttpClient client =
//                HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
//
        HttpClient client = HttpClients.createMinimal();
        String header = "Basic ";
        String headerValue = "source" + ":" + "1-Enter!";
        String encodedHeaderValue = Base64.encodeBase64String(headerValue.getBytes());
        String headerBasic =  header + encodedHeaderValue;

//        HttpPut put1 = new HttpPut("http://127.0.0.1:8000/stream");
//        put1.addHeader("Content-Type", "audio/mpeg");
//        put1.addHeader("Authorization", headerBasic);
//
//        HttpResponse response = client.execute(put1);
//        assertEquals(2, response.getStatusLine().getStatusCode()/100);


//        > PUT /testsendung.mp3 HTTP/1.1
//                > Authorization: Basic REDACTED=
//        > Host: example.com:8001
//                > Accept: */*
//> Content-Type: audio/ogg
//> Ice-Public: 1
//> Ice-Name: Teststream
//> Ice-Description: This is just a simple test stream
//> Ice-URL: http://example.org
//> Ice-Genre: Rock
//> Expect: 100-continue

//        HttpPut put2 = new HttpPut("http://127.0.0.1:8000/stream");
//        put2.addHeader("Content-Type", "audio/mpeg");
//        put2.addHeader("Authorization", headerBasic);
//        put2.addHeader("Ice-Public", "1");
//        put2.addHeader("Ice-Description", "Testing desc");
//        put2.addHeader("Ice-URL", "http://tsd.org");
//        put2.addHeader("Ice-Genre", "Eh");
//        put2.addHeader("Expect", "100-continue");
//        put2.setEntity(new BufferedHttpEntity(new InputStreamEntity(mp3)));
////        put2.setEntity(new ByteArrayEntity(IOUtils.toByteArray(new InputStreamReader(mp3), Charset.forName("UTF-8"))));
//
//        HttpResponse response = client.execute(put2);

        HttpPost post = new HttpPost("192.168.1.100:8090/feed2.ffm");

//        assertEquals(2, response.getStatusLine().getStatusCode()/100);
    }

    @Test
    public void testFFserver() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("C:/Users/Joe/Desktop/ffmpeg/bin/ffmpeg.exe -re -i C:/Users/Joe/Desktop/music.mp3 http://192.168.1.100:8090/feed2.ffm");
        try {
            Process p = pb.start();
            try {
                p.waitFor();
            } finally {
                p.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
