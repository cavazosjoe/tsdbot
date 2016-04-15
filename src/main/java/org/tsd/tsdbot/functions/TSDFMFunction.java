package org.tsd.tsdbot.functions;

import com.google.inject.Singleton;
import org.tsd.tsdbot.module.Function;

@Singleton
@Function(initialRegex = "^\\.tsdfm.*")
public class TSDFMFunction extends MainFunctionImpl {

    @Override
    public void run(String channel, String sender, String ident, String text) {

    }


}
