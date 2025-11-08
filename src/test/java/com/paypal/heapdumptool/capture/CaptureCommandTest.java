package com.paypal.heapdumptool.capture;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CaptureCommandTest {

    @Test
    public void testSanitizationText() {
        assertThat(escapedSanitizationText("\\0"))
                .isEqualTo("\0");
        assertThat(escapedSanitizationText("\0"))
                .isEqualTo("\0");

        assertThat(escapedSanitizationText("foobar"))
                .isEqualTo("foobar");
    }

    private String escapedSanitizationText(final String sanitizationText) {
        final CaptureCommand cmd = new CaptureCommand();
        cmd.setSanitizationText(sanitizationText);
        return cmd.getSanitizationText();
    }

}
