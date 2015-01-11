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
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ToolTipTagFragmentGenerator;
import org.jfree.chart.imagemap.URLTagFragmentGenerator;
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
import org.tsd.tsdbot.util.MiscUtils;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 1/3/2015.
 */
@Singleton
public class Hustle extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Hustle.class);

    private static final String fmt = "Current Hustle/Hate ratio for %s: %s -- %s";

    private HttpClient httpClient;
    private String apiKey;
    private String printoutDir;

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
    }

}
