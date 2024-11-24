import com.xmlcalabash.config.XmlCalabash;
import com.xmlcalabash.datamodel.DeclareStepInstruction;
import com.xmlcalabash.documents.XProcDocument;
import com.xmlcalabash.parsers.xpl.XplParser;
import com.xmlcalabash.runtime.XProcPipeline;
import com.xmlcalabash.util.BufferingReceiver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class SmokeTest {

    @Test
    public void testPipeline() {
        XmlCalabash calabash = XmlCalabash.Companion.newInstance();
        XplParser parser = calabash.newXProcParser();
        DeclareStepInstruction decl = parser.parse("src/test/resources/pipe.xpl");

        XProcPipeline exec = decl.getExecutable();
        BufferingReceiver receiver = new BufferingReceiver();
        exec.setReceiver(receiver);

        try {
            exec.run();
        } catch (Exception ex) {
            fail();
        }

        XProcDocument result = receiver.getOutputs().get("result").get(0);
        System.out.println(result.getValue());
    }
}
