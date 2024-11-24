package com.xmlcalabash.ext.templatejava;

import com.xmlcalabash.api.XProcStep;
import com.xmlcalabash.runtime.parameters.StepParameters;
import com.xmlcalabash.spi.AtomicStepManager;
import kotlin.jvm.functions.Function0;
import net.sf.saxon.s9api.QName;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TemplateStepManager implements AtomicStepManager {
    private static final QName TEMPLATE_STEP_TYPE = new QName("cx", "http://xmlcalabash.com/ns/extensions", "template-java");

    @NotNull
    @Override
    public Set<QName> stepTypes() {
        return Set.of(TEMPLATE_STEP_TYPE);
    }

    @Override
    public boolean stepAvailable(@NotNull QName stepType) {
        return TEMPLATE_STEP_TYPE.equals(stepType);
    }

    @NotNull
    @Override
    public Function0<XProcStep> createStep(@NotNull StepParameters stepParameters) {
        return () -> new TemplateJava();
    }
}
