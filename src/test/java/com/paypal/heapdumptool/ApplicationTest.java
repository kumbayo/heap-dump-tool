package com.paypal.heapdumptool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static com.paypal.heapdumptool.ApplicationTestSupport.runApplication;
import static com.paypal.heapdumptool.fixture.ResourceTool.contentOf;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
public class ApplicationTest {

    @Test
    public void testVersionProvider() throws Exception {
        final String[] version = new Application().getVersion();
        assertThat(version[0]).contains("heap-dump-tool");
    }

    @Test
    public void testMainHelp(final CapturedOutput output) throws Exception {
        final int exitCode = runApplication("help");
        assertThat(exitCode).isEqualTo(0);

        final String expectedOutput = contentOf(getClass(), "help.txt");
        assertThat(output.getOut()).isEqualTo(expectedOutput);
    }
}
