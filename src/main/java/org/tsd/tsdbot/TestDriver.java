package org.tsd.tsdbot;

import com.esotericsoftware.yamlbeans.YamlReader;
import org.tsd.tsdbot.config.TSDBotConfiguration;

import java.io.FileReader;

/**
 * Created by Joe on 2/19/14.
 */
public class TestDriver {

    public static void main(String[] args) throws Exception {

        YamlReader yamlReader = new YamlReader(new FileReader("C:\\Users\\Joe\\code\\TSDBot\\src\\main\\resources\\tsdbot.yml"));
        TSDBotConfiguration configuration = yamlReader.read(TSDBotConfiguration.class);
        int i=0;

    }
}
