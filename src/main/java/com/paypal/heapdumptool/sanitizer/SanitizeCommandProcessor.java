package com.paypal.heapdumptool.sanitizer;

import com.paypal.heapdumptool.cli.CliCommandProcessor;
import com.paypal.heapdumptool.utils.InternalLogger;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;

import static com.paypal.heapdumptool.utils.DateTimeTool.getFriendlyDuration;
import static com.paypal.heapdumptool.utils.ProgressMonitor.numBytesProcessedMonitor;

public class SanitizeCommandProcessor implements CliCommandProcessor {

    private static final InternalLogger LOGGER = InternalLogger.getLogger(SanitizeCommandProcessor.class);

    private final SanitizeCommand command;

    private final SanitizeStreamFactory streamFactory;

    // for mocking
    public static SanitizeCommandProcessor newInstance(final SanitizeCommand command, final SanitizeStreamFactory streamFactory) {
        return new SanitizeCommandProcessor(command, streamFactory);
    }

    public SanitizeCommandProcessor(final SanitizeCommand command) {
        this(command, new SanitizeStreamFactory(command));
    }

    public SanitizeCommandProcessor(final SanitizeCommand command, final SanitizeStreamFactory streamFactory) {
        Validate.isTrue(command.getBufferSize().toBytes() >= 0, "Invalid buffer size");

        this.command = command;
        this.streamFactory = streamFactory;
    }

    @Override
    public void process() throws Exception {
        final Instant now = Instant.now();

        final HeapDumpSanitizer sanitizer = new HeapDumpSanitizer();
        LOGGER.info("Starting heap dump normalization ...");
        LOGGER.info("Class fields to clear: {}", String.join(",", command.getClassFieldsToClearList()));
        LOGGER.info("Input File: {}", command.getInputFile());
        LOGGER.info("Output File: {}", command.getOutputFile());

        try (final InputStream inputStream = streamFactory.newInputStream();
             final OutputStream outputStream = streamFactory.newOutputStream()) {

            sanitize(sanitizer, inputStream, outputStream);
        }
        LOGGER.info("Finished heap dump normalization in {}", getFriendlyDuration(now));
    }

    private void sanitize(final HeapDumpSanitizer sanitizer,
                          final InputStream inputStream,
                          final OutputStream outputStream) throws IOException {
        sanitizer.setInputStream(inputStream);
        sanitizer.setOutputStream(outputStream);
        sanitizer.setProgressMonitor(numBytesProcessedMonitor(command.getBufferSize(), LOGGER));
        sanitizer.setSanitizeCommand(command);

        sanitizer.sanitize();
    }

}
