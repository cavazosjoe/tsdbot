package org.tsd.tsdbot;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.tsd.tsdbot.functions.MainFunction;

/**
 * Created by Joe on 3/29/2015.
 */
public class IntegTestUtils {

    @SafeVarargs
    public static void loadFunctions(Binder binder, Class<? extends MainFunction>... functions) {
        Multibinder<MainFunction> functionBinder = Multibinder.newSetBinder(binder, MainFunction.class);
        for(Class<? extends MainFunction> function : functions)
            functionBinder.addBinding().to(function);
    }
}
