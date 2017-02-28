package org.tsd.tsdbot.tsdtv.processor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.tsd.tsdbot.tsdtv.TSDTVFileProcessor;
import org.tsd.tsdbot.util.MiscUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class AnalysisCollection {
    private String id;
    private String folder;
    private String textOutput;
    private List<FileAnalysis> analyses = new LinkedList<>();

    public AnalysisCollection(String folder) {
        this.id = MiscUtils.getRandomString();
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }

    public String getId() {
        return id;
    }

    public List<FileAnalysis> getAnalyses() {
        return analyses;
    }

    public String getTextOutput() {
        return textOutput;
    }

    public void addAnalysis(FileAnalysis analysis) {
        analyses.add(analysis);
    }

    public void generateTextOutput() {
        boolean discrepancy = false;

        StringBuilder output = new StringBuilder();
        output.append("*** ANALYSIS FOR FILES IN ").append(folder).append(" ***\n\n");

        StringBuilder streamView = new StringBuilder();
        StringBuilder subConfig = new StringBuilder();
        StringBuilder dubConfig = new StringBuilder();
        StringBuilder detailView = new StringBuilder();

        FileAnalysis prevAnalysis = null;

        int vidNum = 1;
        for(FileAnalysis analysis : analyses) {

            if(prevAnalysis != null && !discrepancy) {
                discrepancy = !analysis.matchesStructure(prevAnalysis);
            }

            addSubDubStreams(subConfig, analysis, true);
            addSubDubStreams(dubConfig, analysis, false);

            StringBuilder columnFormat = new StringBuilder();
            columnFormat.append("%03d: ");
            for(int i=0 ; i < analysis.streamCount() ; i++) {
                columnFormat.append("%-16s ");
            }
            columnFormat.append("%n");

            Object[] columns = new Object[1+analysis.streamCount()];
            columns[0] = vidNum;
            int colIdx = 1;
            for(Integer streamNum : analysis.getStreamsByInteger().keySet()) {
                Stream s = analysis.getStreamsByInteger().get(streamNum);
                StringBuilder col = new StringBuilder();
                col.append(s.getStreamNumber()).append("(").append(s.getStreamType().toString().substring(0, 1)).append(")").append(": ");
                if(StringUtils.isEmpty(s.getLanguage()))
                    col.append("NULL");
                else
                    col.append(s.getLanguage());
                columns[colIdx] = col.toString();
                colIdx++;
            }
            streamView.append(String.format(columnFormat.toString(), columns));

            detailView.append("----------------------------------------------------------------------\n");
            detailView.append(analysis.getFile().getName()).append("\n");
            detailView.append("Bitrate: ").append(analysis.getBitrate()).append("\n");
            detailView.append("Duration: ").append(DurationFormatUtils.formatDuration(analysis.getDuration(), "HH:mm:ss")).append("\n");
            for(StreamType streamType : analysis.getStreamsByType().keySet()) {
                TreeSet<Stream> streams = analysis.getStreamsByType().get(streamType);
                detailView.append(streamType).append(" STREAMS: ").append(streams.size()).append("\n");
                for(Stream stream : streams) {
                    detailView.append("\t").append("#").append(stream.getStreamNumber());
                    if(stream.getLanguage() != null)
                        detailView.append(", ").append(stream.getLanguage());
                    detailView.append(": ").append(stream.getRawInfo()).append("\n");
                }
            }

            vidNum++;
            prevAnalysis = analysis;
        }

        if(discrepancy) {
            output.append("**** WARNING: STREAM DISCREPANCY DETECTED, SUGGEST MANUAL FILTERING ****\n\n");
        }

        output.append(streamView.toString()).append("\n\n")
                .append("-- sub stream config --").append("\n")
                .append(subConfig.toString()).append("\n")
                .append("-- dub stream config --").append("\n")
                .append(dubConfig.toString()).append("\n")
                .append("** VIDEO DETAILS **").append("\n\n")
                .append(detailView.toString());

        this.textOutput = output.toString();
    }

    private void addSubDubStreams(StringBuilder sb, FileAnalysis fileAnalysis, boolean sub) {
        try {
            Stream[] streams = (sub) ?
                    TSDTVFileProcessor.detectSubStreams(fileAnalysis) : TSDTVFileProcessor.detectDubStreams(fileAnalysis);
            for(int i=0 ; i < streams.length ; i++) {
                if(i != 0) {
                    sb.append(",");
                }
                sb.append(streams[i].getStreamNumber());
            }
            sb.append("\n");
        } catch (StreamDetectionException sde) {
            sb.append(sde.getMessage()).append("\n");
        }
    }
}
