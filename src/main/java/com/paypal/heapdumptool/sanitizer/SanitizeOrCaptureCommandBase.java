package com.paypal.heapdumptool.sanitizer;

import com.paypal.heapdumptool.cli.CliCommand;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.paypal.heapdumptool.sanitizer.DataSize.ofMegabytes;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;
import static picocli.CommandLine.Help.Visibility.ALWAYS;

public abstract class SanitizeOrCaptureCommandBase implements CliCommand {

    // to allow field injection from picocli, these variables can't be final

    @Option(names = {"-c", "--fields-to-clear"},
            description = "Class fields to clear during processing. Value in com.example.MyClass#fieldName format",
            defaultValue = "com.sun.javafx.css.StyleManager#cacheContainerMap"
                    + "," + "com.sun.javafx.css.StyleManager$ImageCache#imageCache"
                    + "," + "javafx.scene.CssStyleHelper#cacheContainer"
                    + "," + "javafx.scene.CssStyleHelper#firstStyleableAncestor"
                    + "," + "com.sun.javafx.text.PrismTextLayout#stringCache"
                    + "," + "com.sun.javafx.text.PrismTextLayout#layoutCache"
                    + "," + "com.sun.javafx.sg.prism.NGRegion#imageCacheMap"
                    + "," + "com.sun.javafx.fxml.BeanAdapter#globalMethodCache"
                    + "," + "sun.security.util.MemoryCache#cacheMap" // Used from sun.security.provider.X509Factory
                    + "," + "java.util.jar.JarFile#manRef"
                    + "," + "java.lang.invoke.DirectMethodHandle#type"
                    + "," + "java.lang.invoke.DirectMethodHandle#form"
                    + "," + "java.lang.invoke.DirectMethodHandle#member"
                    + "," + "java.lang.invoke.DirectMethodHandle$Constructor#initMethod"
                    + "," + "java.lang.invoke.DirectMethodHandle$Constructor#instanceClass"
                    + "," + "java.lang.invoke.DirectMethodHandle$Accessor#fieldType"
                    + "," + "java.lang.invoke.DirectMethodHandle$Accessor#asTypeCache"
                    + "," + "java.lang.invoke.DirectMethodHandle$Accessor#asTypeSoftCache"
                    + "," + "java.lang.invoke.DirectMethodHandle$Special#caller"
                    + "," + "jdk.internal.loader.ClassLoaders$PlatformClassLoader#nameToModule"
                    + "," + "java.util.ResourceBundle#cacheList"
                    + "," + "java.util.Locale$Cache#LOCALECACHE"
                    + "," + "sun.util.locale.BaseLocale$Cache#CACHE",
            showDefaultValue = ALWAYS)
    private List<String> classFieldsToClearList;

    private StringFieldMap classFieldsToClearMap;

    @Option(names = {"-b", "--buffer-size"}, description = "Buffer size for reading and writing", defaultValue = "100MB", showDefaultValue = ALWAYS)
    private DataSize bufferSize = ofMegabytes(100);

    public DataSize getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(final DataSize bufferSize) {
        this.bufferSize = bufferSize;
    }

    public List<String> getClassFieldsToClearList() {
        final List<String> list = classFieldsToClearList == null ? Collections.emptyList() : classFieldsToClearList;
        return list.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .filter(field -> field.contains("#"))
                .map(field -> field.split(","))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    public void setClassFieldsToClearList(final List<String> list) {
        this.classFieldsToClearList = list;
    }

    private StringFieldMap getClassFieldsToClearMap() {
        if (classFieldsToClearMap != null) {
            return classFieldsToClearMap;
        }
        classFieldsToClearMap = new StringFieldMap();
        for (String excluded : getClassFieldsToClearList()) {
            final String className = StringUtils.substringBefore(excluded, "#");
            final String fieldName = StringUtils.substringAfter(excluded, "#");
            classFieldsToClearMap.add(className, fieldName);
        }
        return classFieldsToClearMap;
    }

    public boolean isExactClassWithFieldToClear(final String className) {
        return getClassFieldsToClearMap().map.containsKey(className);
    }

    public List<String> getFieldsToClear(final String className) {
        return getClassFieldsToClearMap().map.getOrDefault(className, Collections.emptyList());
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    private static class StringFieldMap {
        private final Map<String, List<String>> map = new HashMap<>();

        public void add(final String className, final String fieldName) {
            map.computeIfAbsent(className, key -> new ArrayList<>());
            map.get(className).add(fieldName);
        }

        @Override
        public String toString() {
            return reflectionToString(this, MULTI_LINE_STYLE);
        }

    }
}
