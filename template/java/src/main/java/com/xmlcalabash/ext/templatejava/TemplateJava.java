package com.xmlcalabash.ext.templatejava;

import com.xmlcalabash.io.MediaType;
import com.xmlcalabash.documents.XProcBinaryDocument;
import com.xmlcalabash.documents.XProcDocument;
import com.xmlcalabash.runtime.XProcStepConfiguration;
import com.xmlcalabash.steps.AbstractAtomicStep;
import com.xmlcalabash.util.MediaClassification;
import com.xmlcalabash.util.SaxonTreeBuilder;
import com.xmlcalabash.util.XAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

public class TemplateJava extends AbstractAtomicStep {
    @Override
    public void run() {
        super.run();

        int binary = 0;
        int byteCount = 0;
        int markup = 0;
        int json = 0;
        int text = 0;
        int lineCount = 0;
        int unknown = 0;

        for (XProcDocument doc : getQueues().get("source")) {
            if (doc instanceof XProcBinaryDocument) {
                binary++;
                byteCount += ((XProcBinaryDocument) doc).getBinaryValue().length;
            } else {
                MediaClassification ctc = doc.getContentClassification();
                switch (ctc) {
                    case XML:
                    case XHTML:
                    case HTML:
                        markup++;
                        break;
                    case JSON:
                    case YAML:
                    case TOML:
                        json++;
                        break;
                    case TEXT:
                        text++;
                        break;
                    case BINARY:
                        unknown++;
                        break;
                }
            }
        }

        XProcStepConfiguration stepConfig = getStepConfig();
        SaxonTreeBuilder builder = new SaxonTreeBuilder(stepConfig);
        builder.startDocument(stepConfig.getBaseUri());
        builder.addStartElement(new QName("result"));

        XAttributeMap amap = new XAttributeMap();
        if (byteCount > 0) {
            amap.set(new QName("bytes"), "" + byteCount);
        }
        builder.addStartElement(new QName("", "binary"), amap.getAttributes());
        builder.addText("" + binary);
        builder.addEndElement();

        builder.addStartElement(new QName("", "markup"));
        builder.addText("" + markup);
        builder.addEndElement();

        amap = new XAttributeMap();
        if (lineCount > 0) {
            amap.set(new QName("", "lines"), "" + lineCount);
        }
        builder.addStartElement(new QName("", "text"), amap.getAttributes());
        builder.addText("" + text);
        builder.addEndElement();

        builder.addStartElement(new QName("", "json"));
        builder.addText("" + json);
        builder.addEndElement();

        builder.addEndElement();
        builder.endDocument();
        getReceiver().output("result", XProcDocument.Companion.ofXml(builder.getResult(), stepConfig));
    }

    @Override
    public String toString() {
        return "cx:template-java";
    }
}
