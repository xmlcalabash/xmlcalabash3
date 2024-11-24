package com.xmlcalabash.ext.templatejava;

import com.xmlcalabash.config.SaxonConfiguration;
import com.xmlcalabash.spi.AtomicStepManager;
import com.xmlcalabash.spi.AtomicStepProvider;
import org.jetbrains.annotations.NotNull;

public class TemplateStepProvider implements AtomicStepProvider {
    @NotNull
    @Override
    public AtomicStepManager create() {
        return new TemplateStepManager();
    }
}
