package org.tsd.tsdbot.module;

import com.google.inject.AbstractModule;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(HttpModule.class);

    private final ExecutorService executorService;

    public HttpModule(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    protected void configure() {
        final PoolingHttpClientConnectionManager poolingManager = new PoolingHttpClientConnectionManager();
        poolingManager.setMaxTotal(100);
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                // don't try more than 5 times
                return i < 5 && e instanceof NoHttpResponseException;
            }
        };
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(poolingManager)
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
                .setRetryHandler(retryHandler)
                .build();
        bind(PoolingHttpClientConnectionManager.class).toInstance(poolingManager);
        bind(HttpClient.class).toInstance(httpClient);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("Starting idle connection monitor...");
                    while (true) {
                        synchronized (this) {
                            wait(1000 * 30);
                            // Close expired connections
                            poolingManager.closeExpiredConnections();
                            // Optionally, close connections
                            // that have been idle longer than 30 sec
                            poolingManager.closeIdleConnections(30, TimeUnit.SECONDS);
                        }
                    }
                } catch (InterruptedException ex) {
                    // terminate
                    log.warn("Idle connection monitor terminated");
                }
            }
        });
    }
}
