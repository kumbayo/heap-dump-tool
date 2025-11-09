package com.paypal.heapdumptool.utils;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class InternalLogger {

    private static final int DEBUG_INT = 10;

    private static final int INFO_INT = 20;

    private static final int level = Integer.getInteger("heap-dump-tool.logLevel", INFO_INT);

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    private final PrintStream out = System.out;

    private final String clazz;

    public InternalLogger(final Class<?> clazz) {
        this.clazz = clazz.getSimpleName();
    }

    public static InternalLogger getLogger(final Class<?> clazz) {
        return new InternalLogger(clazz);
    }

    public void info(final String format, final Object... arguments) {
        if (level <= INFO_INT) {
            final String message = getMessage(format, arguments);
            out.printf("%s INFO  %s - %s%n", getTimestamp(), clazz, message);
        }
    }

    public void debug(final String format, final Object... arguments) {
        if (level <= DEBUG_INT) {
            final String message = getMessage(format, arguments);
            out.printf("%s DEBUG %s - %s%n", getTimestamp(), clazz, message);
        }
    }

    private static String getMessage(final String format, final Object[] arguments) {
        final String newFormat = format.replace("{}", "%s");
        return String.format(newFormat, arguments);
    }

    private static String getTimestamp() {
        return dateTimeFormatter.format(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
