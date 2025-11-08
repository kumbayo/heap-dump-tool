package com.paypal.heapdumptool.sanitizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipOutputStream;

import static com.paypal.heapdumptool.sanitizer.DataSize.ofBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class SanitizeStreamFactoryTest {

    @TempDir
    Path tempDir;

    private SanitizeStreamFactory streamFactory;

    @Test
    public void testStdinInputStream() throws IOException {
        final SanitizeCommand cmd = newCommand();
        cmd.setInputFile(Paths.get("-"));
        cmd.setBufferSize(ofBytes(0));

        streamFactory = new SanitizeStreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isEqualTo(System.in);
    }

    @Test
    public void testBufferedInputStream() throws IOException {
        final SanitizeCommand cmd = newCommand();
        cmd.setInputFile(Paths.get("-"));

        streamFactory = new SanitizeStreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isInstanceOf(BufferedInputStream.class)
                .isNotSameAs(System.in);
    }

    @Test
    public void testBufferedOutputStream() throws IOException {
        final SanitizeCommand cmd = newCommand();
        cmd.setOutputFile(tempDir.resolve("testBufferedOutputStream"));

        streamFactory = new SanitizeStreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isInstanceOf(BufferedOutputStream.class);
    }

    @Test
    public void testFileInputStream() throws IOException {
        final Path inputFile = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
        final SanitizeCommand cmd = newCommand();
        cmd.setInputFile(inputFile);
        cmd.setBufferSize(ofBytes(0));

        streamFactory = new SanitizeStreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isNotInstanceOf(PrintStream.class);
    }

    @Test
    public void testFileOutputStream() throws IOException {
        final Path file = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
        final SanitizeCommand cmd = newCommand();
        cmd.setInputFile(Paths.get("foo"));
        cmd.setOutputFile(file);
        cmd.setBufferSize(ofBytes(0));

        streamFactory = new SanitizeStreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isNotInstanceOf(PrintStream.class);
    }

    @Test
    public void testZipOutputStream() throws IOException {
        final Path outputFile = Files.createTempFile(tempDir, getClass().getSimpleName(), ".zip");

        final SanitizeCommand cmd = newCommand();
        cmd.setInputFile(Paths.get("foo"));
        cmd.setOutputFile(outputFile);
        cmd.setBufferSize(ofBytes(0));
        cmd.setZipOutput(true);

        streamFactory = new SanitizeStreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isInstanceOf(ZipOutputStream.class);
    }

    @Test
    public void testSameInputOutput() {
        final SanitizeCommand cmd = newCommand();
        cmd.setInputFile(Paths.get("foo"));
        cmd.setOutputFile(Paths.get("foo"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SanitizeStreamFactory(cmd));
    }

    private SanitizeCommand newCommand() {
        final SanitizeCommand cmd = new SanitizeCommand();
        cmd.setInputFile(Paths.get("input.txt"));
        cmd.setOutputFile(Paths.get("output.txt"));
        return cmd;
    }
}
