package com.xmlcalabash.test;

import com.xmlcalabash.XmlCalabash;
import com.xmlcalabash.XmlCalabashBuilder;
import com.xmlcalabash.api.MessageReporter;
import com.xmlcalabash.config.ConfigurationLoader;
import com.xmlcalabash.datamodel.DeclareStepInstruction;
import com.xmlcalabash.documents.XProcDocument;
import com.xmlcalabash.io.MediaType;
import com.xmlcalabash.io.MessagePrinter;
import com.xmlcalabash.parsers.xpl.XplParser;
import com.xmlcalabash.runtime.XProcPipeline;
import com.xmlcalabash.runtime.XProcRuntime;
import com.xmlcalabash.util.BufferingReceiver;
import com.xmlcalabash.util.Report;
import com.xmlcalabash.util.UriUtils;
import com.xmlcalabash.util.Verbosity;
import kotlin.jvm.functions.Function0;
import net.sf.saxon.s9api.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

public class JavaApiTest {
    Processor processor;

    private XdmNode parseString(String xml) {
        return parseStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private XdmNode parseStream(InputStream stream) {
        DocumentBuilder builder = processor.newDocumentBuilder();
        InputSource input = new InputSource(stream);
        input.setSystemId("http://example.com/");
        try {
            return builder.build(new SAXSource(input));
        } catch (SaxonApiException ex) {
            throw new RuntimeException(ex);
        }
    }

    private XdmNode anIdentityPipeline() {
        try {
            return parseStream(new FileInputStream(new File("src/test/resources/identity.xpl")));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private XdmNode anXsltPipeline() {
        try {
            return parseStream(new FileInputStream(new File("src/test/resources/xslt.xpl")));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private XmlCalabash setupXmlCalabash() {
        XmlCalabashBuilder builder = new XmlCalabashBuilder();
        XmlCalabash xmlCalabash = builder.build();
        processor = xmlCalabash.getSaxonConfiguration().getProcessor();
        return xmlCalabash;
    }

    @Test
    public void runIdentity() {
        XmlCalabash xmlCalabash = setupXmlCalabash();

        XplParser parser = xmlCalabash.newXProcParser();
        DeclareStepInstruction declareStep = parser.parse(anIdentityPipeline());
        XProcRuntime runtime = declareStep.runtime();
        XProcPipeline pipeline = runtime.executable();

        BufferingReceiver receiver = new BufferingReceiver();
        pipeline.setReceiver(receiver);

        pipeline.input("source", XProcDocument.Companion.ofXml(parseString("<doc/>"), pipeline.getConfig(), MediaType.Companion.getXML()));
        pipeline.run();

        XdmValue result = receiver.getOutputs().get("result").get(0).getValue();
        Assertions.assertEquals("<doc/>", result.toString());
    }

    @Test
    public void runXslt() {
        XmlCalabash xmlCalabash = setupXmlCalabash();

        XplParser parser = xmlCalabash.newXProcParser();
        DeclareStepInstruction declareStep = parser.parse(anXsltPipeline());
        XProcRuntime runtime = declareStep.runtime();
        XProcPipeline pipeline = runtime.executable();

        BufferingReceiver receiver = new BufferingReceiver();
        pipeline.setReceiver(receiver);

        pipeline.input("source", XProcDocument.Companion.ofXml(parseString("<doc/>"), pipeline.getConfig(), MediaType.Companion.getXML()));
        pipeline.run();

        XdmValue result = receiver.getOutputs().get("result").get(0).getValue();
        Assertions.assertEquals("<doc>You got some output</doc>", result.toString());
    }

    @Test
    public void runXsltWithAMessage() {
        XmlCalabash xmlCalabash = setupXmlCalabash();

        XplParser parser = xmlCalabash.newXProcParser();
        DeclareStepInstruction declareStep = parser.parse(anXsltPipeline());
        XProcRuntime runtime = declareStep.runtime();
        XProcPipeline pipeline = runtime.executable();

        BufferingReceiver receiver = new BufferingReceiver();
        pipeline.setReceiver(receiver);

        pipeline.input("source", XProcDocument.Companion.ofXml(parseString("<doc/>"), pipeline.getConfig(), MediaType.Companion.getXML()));
        pipeline.option(new QName("message"), new XdmAtomicValue("Hello, world."));
        pipeline.run();

        XdmValue result = receiver.getOutputs().get("result").get(0).getValue();
        Assertions.assertEquals("<doc>You got some output</doc>", result.toString());
    }

    @Test
    public void runXsltCaptureMessage() {
        MessagePrinter printer = new MyMessagePrinter();
        MessageReporter reporter = new MyMessageReporter(printer);

        XmlCalabashBuilder builder = new XmlCalabashBuilder();
        builder.setMessagePrinter(printer);
        builder.setMessageReporter(reporter);

        XmlCalabash xmlCalabash = builder.build();
        processor = xmlCalabash.getSaxonConfiguration().getProcessor();

        XplParser parser = xmlCalabash.newXProcParser();
        DeclareStepInstruction declareStep = parser.parse(anXsltPipeline());
        XProcRuntime runtime = declareStep.runtime();
        XProcPipeline pipeline = runtime.executable();

        BufferingReceiver receiver = new BufferingReceiver();
        pipeline.setReceiver(receiver);

        pipeline.input("source", XProcDocument.Companion.ofXml(parseString("<doc/>"), pipeline.getConfig(), MediaType.Companion.getXML()));
        pipeline.option(new QName("message"), new XdmAtomicValue("Hello, world."));
        pipeline.run();

        XdmValue result = receiver.getOutputs().get("result").get(0).getValue();
        Assertions.assertEquals("<doc>You got some output</doc>", result.toString());
    }

    @Test
    public void runXsltCaptureError() {
        MessagePrinter printer = new MyMessagePrinter();
        MessageReporter reporter = new MyMessageReporter(printer);

        XmlCalabashBuilder builder = new XmlCalabashBuilder();
        builder.setMessagePrinter(printer);
        builder.setMessageReporter(reporter);

        XmlCalabash xmlCalabash = builder.build();
        processor = xmlCalabash.getSaxonConfiguration().getProcessor();

        XplParser parser = xmlCalabash.newXProcParser();
        DeclareStepInstruction declareStep = parser.parse(anXsltPipeline());
        XProcRuntime runtime = declareStep.runtime();
        XProcPipeline pipeline = runtime.executable();

        BufferingReceiver receiver = new BufferingReceiver();
        pipeline.setReceiver(receiver);

        pipeline.input("source", XProcDocument.Companion.ofXml(parseString("<doc/>"), pipeline.getConfig(), MediaType.Companion.getXML()));
        pipeline.option(new QName("message"), new XdmAtomicValue("Ruh, roh."));
        pipeline.option(new QName("fail"), new XdmAtomicValue(true));

        try {
            pipeline.run();
            fail();
        } catch (Exception ex) {
            Assertions.assertNotNull(ex);
        }
    }

    @Test
    public void loadConfiguration() {
        XmlCalabashBuilder builder = new XmlCalabashBuilder();
        ConfigurationLoader loader = new ConfigurationLoader(builder);
        loader.load(UriUtils.Companion.cwdAsUri().resolve("src/test/resources/configfile.xml"));

        XmlCalabash xmlCalabash = builder.build();
        processor = xmlCalabash.getSaxonConfiguration().getProcessor();

        XplParser parser = xmlCalabash.newXProcParser();
        DeclareStepInstruction declareStep = parser.parse(anIdentityPipeline());
        XProcRuntime runtime = declareStep.runtime();
        XProcPipeline pipeline = runtime.executable();

        BufferingReceiver receiver = new BufferingReceiver();
        pipeline.setReceiver(receiver);

        pipeline.input("source", XProcDocument.Companion.ofXml(parseString("<doc/>"), pipeline.getConfig(), MediaType.Companion.getXML()));
        pipeline.run();

        XdmValue result = receiver.getOutputs().get("result").get(0).getValue();
        Assertions.assertEquals("<doc/>", result.toString());
    }


    private static class MyMessagePrinter implements MessagePrinter {
        private String _encoding = "UTF-8";

        @Override
        public @NotNull String getEncoding() {
            return _encoding;
        }

        @Override
        public void setEncoding(@NotNull String encoding) {
            _encoding = encoding;
        }

        @Override
        public void setPrintStream(@NotNull PrintStream stream) {
            // nop
        }

        @Override
        public void print(@NotNull String message) {
            // nop
        }

        @Override
        public void println(@NotNull String message) {
            // nop
        }
    }

    private static class MyMessageReporter implements MessageReporter {
        private static Map<Verbosity, Integer> levels = new HashMap<>();
        private MessagePrinter printer;
        private Verbosity threshold = Verbosity.INFO;

        static {
            levels.put(Verbosity.ERROR, 5);
            levels.put(Verbosity.WARN, 4);
            levels.put(Verbosity.INFO, 3);
            levels.put(Verbosity.DEBUG, 2);
            levels.put(Verbosity.TRACE, 1);
        }

        public MyMessageReporter(MessagePrinter printer) {
            this.printer = printer;
        }

        @Override
        public @NotNull MessagePrinter getMessagePrinter() {
            return printer;
        }

        @Override
        public @NotNull Verbosity getThreshold() {
            return threshold;
        }

        @Override
        public void setThreshold(@NotNull Verbosity verbosity) {
            threshold = verbosity;
        }

        @Override
        public void setMessagePrinter(@NotNull MessagePrinter messagePrinter) {
            printer = messagePrinter;
        }

        @Override
        public void error(@NotNull Function0<? extends @NotNull Report> report) {
            report(Verbosity.ERROR, report);
        }

        @Override
        public void warn(@NotNull Function0<? extends @NotNull Report> report) {
            report(Verbosity.WARN, report);
        }

        @Override
        public void info(@NotNull Function0<? extends @NotNull Report> report) {
            report(Verbosity.INFO, report);
        }

        @Override
        public void debug(@NotNull Function0<? extends @NotNull Report> report) {
            report(Verbosity.DEBUG, report);
        }

        @Override
        public void trace(@NotNull Function0<? extends @NotNull Report> report) {
            report(Verbosity.TRACE, report);
        }

        @Override
        public void report(@NotNull Verbosity severity, @NotNull Function0<? extends @NotNull Report> report) {
            if (levels.get(severity) >= levels.get(threshold)) {
                printer.println(report.invoke().getMessage());
            }
        }
    }
}
