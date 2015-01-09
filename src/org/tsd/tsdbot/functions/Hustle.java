package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.util.ShapeUtilities;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.CircularBuffer;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.MiscUtils;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Joe on 1/3/2015.
 */
@Singleton
public class Hustle extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Hustle.class);

    private static final int period = 5; // every [period]th message will be sent for hustle analysis
    private static final DecimalFormat decimalFormat = new DecimalFormat("##0.00");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss a z");
    private static final String fmt = "Current Hustle/Hate ratio for %s: %s -- %s";

    static {
        timeFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    private HttpClient httpClient;
    private String apiKey;
    private String printoutDir;

    private DataPoint lastDataPoint = null;
    CircularBuffer<DataPoint> huffleBustle = new CircularBuffer<>(50);

    private int msgCnt = 0;

    @Inject
    public Hustle(TSDBot bot, HttpClient httpClient, Properties properties) {
        super(bot);
        this.httpClient = httpClient;
        this.apiKey = properties.getProperty("mashape.apiKey");
        this.printoutDir = properties.getProperty("printout.dir");
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {

        logger.info("Generating hustle analysis...");

        try {

            TreeSet<DataPoint> orderedByImpact = new TreeSet<>(new Comparator<DataPoint>() {
                @Override
                public int compare(DataPoint o1, DataPoint o2) {
                    return Double.compare(Math.abs(o1.delta), Math.abs(o2.delta));
                }
            });

            TimeSeries timeSeries = new TimeSeries("Hustle");
            for (DataPoint dataPoint : huffleBustle) {
                timeSeries.add(new Second(dataPoint.date), dataPoint.newHhr);
                orderedByImpact.add(dataPoint);
            }

            TimeSeriesCollection dataset = new TimeSeriesCollection(timeSeries);

            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    "Maybe we should hustle as hard as we hate",
                    "Time",
                    "HHR",
                    dataset
            );

            XYPlot plot = chart.getXYPlot();

            DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
            domainAxis.setDateFormatOverride(timeFormat);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setNumberFormatOverride(decimalFormat);
            double mid = rangeAxis.getRange().getCentralValue();

            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            renderer.setSeriesShape(0, ShapeUtilities.createDiamond(5));
            renderer.setSeriesShapesVisible(0, true);

            int limit = 5;
            int i = 0;
            for(DataPoint dp : orderedByImpact.descendingSet()) {
                if(i++ > limit)
                    break;
                logger.info("Adding annotation, hhr = {}, delta = {}", dp.newHhr, dp.delta);
                TimeSeriesDataItem importantItem = timeSeries.getDataItem(new Second(dp.date));
                double x = importantItem.getPeriod().getFirstMillisecond();
                double y = importantItem.getValue().doubleValue();
                double r = (y > mid) ? (Math.PI / 2) : (3 * Math.PI / 2);
                String s = trimMessage(dp.text);
                XYPointerAnnotation a = new XYPointerAnnotation(s, x, y, r);
                a.setLabelOffset(10);
                a.setFont(new Font("SansSerif", Font.PLAIN, 12));
                a.setOutlineStroke(new BasicStroke(5));
                plot.addAnnotation(a);
            }

            String fileName = MiscUtils.getRandomString() + ".png";
            File file = new File(printoutDir + fileName);
            ChartUtilities.saveChartAsPNG(file, chart, 2000, 500);

            logger.info("Done generating hustle chart");
            bot.sendMessage(channel, String.format(fmt, channel, decimalFormat.format(calculateCurrentHhr()), "http://irc.teamschoolyd.org/printouts/" + fileName));

        } catch (Exception e) {
            logger.error("Error generating hustle chart", e);
            bot.sendMessage(channel, "Error generating hustle analysis. Everything sucks.");
        }
    }

    public void process(String channel, String text) {
        if(msgCnt++ < period)
            return;
        else msgCnt = 0;

        logger.info("Sending latest message for sentiment analysis...");

        HttpPost post = null;
        try {
            post = new HttpPost("https://community-sentiment.p.mashape.com/text/");
            post.addHeader("X-Mashape-Key", apiKey);
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            LinkedList<NameValuePair> params = new LinkedList<>();
            params.add(new BasicNameValuePair("txt", text));
            post.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());

            JSONObject json = new JSONObject(responseString);
            for(String key : json.keySet()) {
                if(key.equals("result")) {
                    double confidence = Double.parseDouble(json.getJSONObject(key).getString("confidence"));
                    Sentiment sentiment = Sentiment.fromString(json.getJSONObject(key).getString("sentiment"));
                    logger.info("Analysis result: {} (Confidence {})", sentiment, confidence);
                    double lastHhr = (lastDataPoint == null) ? 0 : lastDataPoint.newHhr;
                    logger.info("Previous HHR: {}", lastHhr);
                    lastDataPoint = new DataPoint(text, sentiment, confidence);
                    huffleBustle.add(lastDataPoint);
                    lastDataPoint.newHhr = calculateCurrentHhr();
                    lastDataPoint.delta = lastDataPoint.newHhr - lastHhr;
                    logger.info("New HHR: {} (delta {})", lastDataPoint.newHhr, lastDataPoint.delta);
                }
            }

            EntityUtils.consumeQuietly(response.getEntity());

        } catch (Exception e) {
            logger.error("Error retrieving text sentiment", e);
            bot.sendMessage(channel, "(Error calculating hustle quotient, please check logs)");
        } finally {
            if(post != null)
                post.releaseConnection();
        }
    }

    private double calculateCurrentHhr() {
        double hustle = 1;
        double hate = 1;
        for(DataPoint dataPoint : huffleBustle) {
            switch (dataPoint.sentiment) {
                case Positive: hustle += dataPoint.getScore(); break;
                case Negative: hate += dataPoint.getScore(); break;
                case Neutral: {
                    hustle += dataPoint.getScore();
                    hate += dataPoint.getScore();
                    break;
                }
            }
        }
        return hustle/hate;
    }

    private String trimMessage(String text) {
        int maxChars = 50;
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        int i_c = 0;
        for(String word : words) {
            if(i_c > maxChars) {
                sb.append("...");
                return sb.toString();
            } else if(i_c != 0)  {
                sb.append(" ");
                i_c++;
            }
            sb.append(word);
            i_c += word.length();
        }
        return sb.toString();
    }

    enum Sentiment {
        Positive,
        Negative,
        Neutral;

        public static Sentiment fromString(String s) {
            for(Sentiment sentiment : values()) {
                if(sentiment.toString().toLowerCase().equals(s.toLowerCase()))
                    return sentiment;
            }
            return null;
        }
    }

    class DataPoint {

        public String text;
        public Sentiment sentiment;
        public double confidence;
        public Date date;
        public double newHhr;
        public double delta;

        DataPoint(String text, Sentiment sentiment, double confidence) {
            this.text = text;
            this.sentiment = sentiment;
            this.confidence = confidence;
            this.date = new Date();
        }

        public double getScore() {
            return (confidence/100) * text.length();
        }
    }

}
