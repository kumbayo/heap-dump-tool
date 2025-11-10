package com.paypal.heapdumptool;

import com.paypal.heapdumptool.sanitizer.DataSize;
import com.paypal.heapdumptool.sanitizer.SanitizeCommand;
import com.paypal.heapdumptool.utils.InternalLogger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IVersionProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

import static com.paypal.heapdumptool.Application.APP_ID;
import static org.apache.commons.io.IOUtils.resourceToByteArray;

@Command(name = APP_ID,
        description = "Tool for normalizing heap dumps",
        mixinStandardHelpOptions = true,
        versionProvider = Application.class,
        subcommands = {
                SanitizeCommand.class,
                HelpCommand.class,
        }
)
public class Application implements IVersionProvider {

    public static final String APP_ID = "heap-dump-tool";

    private final String versionResource = System.getProperty("heap-dump-tool.version-resource", "/git-heap-dump-tool.properties");

    // Stay with "String[] args". vararg "String... args" causes weird failure with mockito
    public static void main(final String[] args) throws Exception {
        final CommandLine commandLine = newCommandLine();

        final int exitCode = commandLine.execute(args);
        systemExit(exitCode);
    }

    static CommandLine newCommandLine() {
        final CommandLine commandLine = new CommandLine(new Application());
        commandLine.setUsageHelpWidth(120);
        commandLine.registerConverter(DataSize.class, DataSize::parse);
        commandLine.setAbbreviatedOptionsAllowed(true);
        return commandLine;
    }

    // for mocking
    static void systemExit(final int exitCode) {
        System.exit(exitCode);
    }

    public static void printVersion() throws IOException {
        final String[] versionInfo = new Application().getVersion();
        InternalLogger.getLogger(Application.class).info(versionInfo[0]);
    }

    @Override
    public String[] getVersion() throws IOException {
        final byte[] bytes = resourceToByteArray(versionResource);
        final Properties gitProperties = new Properties();
        gitProperties.load(new ByteArrayInputStream(bytes));

        final String versionInfo = MessageFormat.format(
                "{0} ({1} {2}, {3})",
                APP_ID,
                gitProperties.get("git.build.version"),
                gitProperties.get("git.commit.id.abbrev"),
                gitProperties.get("git.commit.time")
        );
        return new String[]{versionInfo};
    }

}
