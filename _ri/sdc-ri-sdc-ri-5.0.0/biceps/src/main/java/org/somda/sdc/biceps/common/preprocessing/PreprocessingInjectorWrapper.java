package org.somda.sdc.biceps.common.preprocessing;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Wrapper class to pass the injector in PreprocessingUtil without the warning:
 * AssistedInject factory ...will be slow because class ... has assisted Provider dependencies or injects the Injector.
 */
public class PreprocessingInjectorWrapper {

    private final Injector injector;

    @Inject
    public PreprocessingInjectorWrapper(Injector injector) {
        this.injector = injector;
    }

    public Injector getInjector() {
        return injector;
    }
}
