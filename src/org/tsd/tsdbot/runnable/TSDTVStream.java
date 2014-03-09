package org.tsd.tsdbot.runnable;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTVStream implements Callable {
    @Override
    public Object call() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("tsdtv.sh", "sgc2c_s06e01_chambraigne.m4v");
        pb.directory(new File(System.getProperty("user.home")));
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        return null;
    }
}
