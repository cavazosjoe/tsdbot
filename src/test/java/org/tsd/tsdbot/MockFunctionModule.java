package org.tsd.tsdbot;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.mockito.Mockito;
import org.reflections.Reflections;
import org.tsd.tsdbot.functions.MainFunction;
import org.tsd.tsdbot.module.Function;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Joe on 3/28/2015.
 */
public class MockFunctionModule extends AbstractModule {

    private HashSet<Class<? extends MainFunction>> ignoredFunctions;

    public MockFunctionModule(HashSet<Class<? extends MainFunction>> ignoredFunctions) {
        this.ignoredFunctions = ignoredFunctions;
    }

    @Override
    protected void configure() {

        Multibinder<MainFunction> functionBinder = Multibinder.newSetBinder(binder(), MainFunction.class);

        Reflections modelReflect = new Reflections("org.tsd.tsdbot.functions");
        Set<Class<?>> functions = modelReflect.getTypesAnnotatedWith(Function.class);
        for (Class clazz : functions) {
            if(!ignoredFunctions.contains(clazz)) {
                MainFunction mockedFunction = (MainFunction) Mockito.mock(clazz);
                String regex = ((Function)clazz.getAnnotation(Function.class)).initialRegex();
                Mockito.when(mockedFunction.getListeningRegex()).thenReturn(regex);
                functionBinder.addBinding().toInstance(mockedFunction);
            } else {
                functionBinder.addBinding().to(clazz);
            }
        }

    }
}
