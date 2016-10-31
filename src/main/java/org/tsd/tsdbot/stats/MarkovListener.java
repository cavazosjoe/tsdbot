package org.tsd.tsdbot.stats;

import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.markov.MarkovFileManager;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

public class MarkovListener implements Stats {

    private static final Logger log = LoggerFactory.getLogger(MarkovListener.class);

    private final MarkovFileManager markovFileManager;

    private boolean healthy = true;

    @Inject
    public MarkovListener(MarkovFileManager markovFileManager) {
        this.markovFileManager = markovFileManager;
    }

    @Override
    public LinkedHashMap<String, Object> getReport() {
        LinkedHashMap<String, Object> report = new LinkedHashMap<>();
        List<File> allMarkovFiles = markovFileManager.allFiles();
        report.put("Markov status", healthy ? "HEALTHY" : "ERROR");
        report.put("Markov files", allMarkovFiles.size());
        long bytes = allMarkovFiles.stream().mapToLong(File::length).sum();
        report.put("Markov size", FileUtils.byteCountToDisplaySize(bytes));
        return report;
    }

    @Override
    public void processMessage(String channel, String sender, String login, String hostname, String message) {
        try{
            markovFileManager.process(sender, message.split("\\s+"));
            healthy = true;
        } catch (Exception e) {
            log.error("Error during markov processing", e);
            healthy = false;
        }
    }

    @Override
    public void processAction(String sender, String login, String hostname, String target, String action) {

    }
}
