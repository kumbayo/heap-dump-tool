package com.paypal.heapdumptool.sanitizer;

import com.paypal.heapdumptool.utils.InternalLogger;
import com.paypal.heapdumptool.utils.ProgressMonitor;
import org.apache.commons.lang3.function.Failable;
import org.apache.commons.lang3.mutable.MutableLong;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.paypal.heapdumptool.sanitizer.HeapRecord.HEAP_DUMP;
import static com.paypal.heapdumptool.sanitizer.HeapRecord.HEAP_DUMP_SEGMENT;
import static com.paypal.heapdumptool.sanitizer.HeapRecord.LOAD_CLASS;
import static com.paypal.heapdumptool.sanitizer.HeapRecord.STRING_IN_UTF8;

/**
 * Heavily based on: <br>
 *
 * <a href="https://html-preview.github.io/?url=https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/demo/jvmti/hprof/manual.html">
 * Heap Dump Binary Format Spec
 * </a> (highly recommended to make sense of the code in any meaningful way)
 * <br>
 *
 * <a href="https://github.com/openjdk/jdk/blob/a2bbf933d96dc4a911ac4b429519937d8dd83200/src/hotspot/share/services/heapDumper.cpp">
 * JDK heapDumper.cpp
 * </a>
 * <br>
 *
 * <a href="https://github.com/AdoptOpenJDK/jheappo">
 * JHeappo
 * </a> (clean modern code)
 * <br>
 *
 * <a href="https://github.com/apache/netbeans/tree/f2611e358c181935500ea4d9d9142fb850504a72/profiler/lib.profiler/src/org/netbeans/lib/profiler/heap">
 * NetBeans/VisualVM HeapDump code (old but reference)
 * </a>
 */
public class HeapDumpSanitizer {

    private static final InternalLogger LOGGER = InternalLogger.getLogger(HeapDumpSanitizer.class);

    private InputStream inputStream;
    private OutputStream outputStream;
    private ProgressMonitor progressMonitor;
    private SanitizeCommand sanitizeCommand;

    private final Map<Long, String> stringIdToStringMap = new HashMap<>();
    private final Map<Long, Long> classObjectIdToStringIdMap = new HashMap<>();
    private final Map<String, ClassObject> classNameToClassObjectsMap = new HashMap<>();

    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setProgressMonitor(final ProgressMonitor numBytesWrittenMonitor) {
        this.progressMonitor = numBytesWrittenMonitor;
    }

    public void setSanitizeCommand(final SanitizeCommand sanitizeCommand) {
        this.sanitizeCommand = sanitizeCommand;
    }

    public void sanitize() throws IOException {
        final Pipe pipe = new Pipe(inputStream, outputStream, progressMonitor);

        /*
         * The basic fields in the binary output are u1 (1 byte), u2 (2 byte), u4 (4 byte), and u8 (8 byte).
         *
         * The binary output begins with the information:
         * [u1]* An initial NULL terminated series of bytes representing the format name and version
         * u4 size of identifiers. Identifiers are used to represent UTF8 strings, objects, stack traces, etc.
         * u4 high word of number of milliseconds since 0:00 GMT, 1/1/70
         * u4 low word of number of milliseconds since 0:00 GMT, 1/1/70
         */
        final String version = pipe.pipeNullTerminatedString().trim();
        LOGGER.debug("Heap Dump Version: {}", version);

        pipe.setIdSize((int) pipe.pipeU4());
        LOGGER.debug("Id Size: {}", pipe.getIdSize());
        pipe.pipe(8);

        /*
         * Followed by a sequence of records that look like:
         * u1		TAG: denoting the type of the record
         * u4		TIME: number of microseconds since the time stamp in the header
         * u4		LENGTH: number of bytes that follow this u4 field and belong to this record
         * [u1]*	BODY: as many bytes as specified in the above u4 field
         */

        while (true) {
            final int tag = pipe.pipeU1IfPossible();
            if (tag == -1) {
                break;
            }
            final HeapRecord heapRecord = HeapRecord.findByTag(tag);

            pipe.pipeU4(); // timestamp
            final long length = pipe.pipeU4();
            LOGGER.debug("Tag: {}", tag);
            LOGGER.debug("Length: {}", length);

            if (heapRecord == HEAP_DUMP || heapRecord == HEAP_DUMP_SEGMENT) {
                final Pipe heapPipe = pipe.newInputBoundedPipe(length);
                copyHeapDumpRecord(heapPipe);

            } else if (heapRecord == STRING_IN_UTF8) {
                copyStringInUtf8Record(pipe, length);

            } else if (heapRecord == LOAD_CLASS) {
                copyLoadClassRecord(pipe);

            } else {
                pipe.pipe(length);
            }
        }
    }

    private void copyLoadClassRecord(final Pipe pipe) throws IOException {
        pipe.pipeU4(); // class serial number
        final long classObjectId = pipe.pipeId();// class object ID
        pipe.pipeU4(); // stack trace serial number
        final long id = pipe.pipeId();// class name string ID
        classObjectIdToStringIdMap.put(classObjectId, id);
    }

    private void copyStringInUtf8Record(final Pipe pipe, final long length) throws IOException {
        final long id = pipe.pipeId();
        final Pipe dataPipe = pipe.newInputBoundedPipe(length - pipe.getIdSize());
        final String string = dataPipe.pipeString(length);
        stringIdToStringMap.put(id, string.replace("/", "."));
    }

    private void copyHeapDumpRecord(final Pipe pipe) throws IOException {
        while (true) {
            final int tag = pipe.pipeU1IfPossible();
            if (tag == -1) {
                break;
            }
            LOGGER.debug("Heap Dump Tag: {}", tag);

            final long id = pipe.pipeId();
            switch (tag) {
                case 0xFF: // GC_ROOT_UNKNOWN
                    break;

                case 0x01: // GC_ROOT_JNI_GLOBAL
                    pipe.pipeId();
                    break;

                case 0x02: // GC_ROOT_JNI_LOCAL
                case 0x03: // GC_ROOT_JAVA_FRAME
                    pipe.pipe(4 + 4);
                    break;

                case 0x04: // GC_ROOT_NATIVE_STACK
                    pipe.pipeU4();
                    break;

                case 0x05: // GC_ROOT_STICKY_CLASS
                    break;

                case 0x06: // GC_ROOT_THREAD_BLOCK
                    pipe.pipeU4();
                    break;

                case 0x07: // GC_ROOT_MONITOR_USED
                    break;

                case 0x08: // GC_ROOT_THREAD_OBJ
                    pipe.pipe(4 + 4);
                    break;

                case 0x20: // GC_CLASS_DUMP
                    copyHeapDumpClassDump(pipe, id);
                    break;

                case 0x21: // GC_INSTANCE_DUMP
                    copyHeapDumpInstanceDump(pipe, id);
                    break;

                case 0x22: // GC_OBJ_ARRAY_DUMP
                    copyHeapDumpObjectArrayDump(pipe);
                    break;

                case 0x23: // GC_PRIM_ARRAY_DUMP
                    copyHeapDumpPrimitiveArrayDump(pipe, id);
                    break;

                default:
                    throw new IllegalArgumentException("" + tag);
            }
        }
    }

    private void copyHeapDumpClassDump(final Pipe pipe, final long classObjectId) throws IOException {
        pipe.pipeU4(); // stacktrace
        final long superClassObjectId = pipe.pipeId();
        pipe.pipeId(); // class loader object id
        pipe.pipeId(); // signers object id
        pipe.pipeId(); // protection domain
        pipe.pipeId(); // reserved
        pipe.pipeId(); // reserved
        pipe.pipeU4(); // instance size

        final int numConstantPoolRecords = pipe.pipeU2();
        for (int i = 0; i < numConstantPoolRecords; i++) {
            pipe.pipeU2();
            final int entryType = pipe.pipeU1();
            pipeBasicType(pipe, entryType);
        }

        final String className = getClassName(classObjectId);
        // final String superClassName = getClassName(superClassObjectId);
        final ClassObject classObject = new ClassObject(classObjectId, superClassObjectId);
        classNameToClassObjectsMap.putIfAbsent(className, classObject);

        final Collection<String> excludeStaticFields = getFieldsToClearInClassHierarchy(className);

        final int numStaticFields = pipe.pipeU2();
        for (int i = 0; i < numStaticFields; i++) {
            final long fieldNameStringId = pipe.pipeId();
            final String fieldName = stringIdToStringMap.getOrDefault(fieldNameStringId, "");
            final int fieldType = pipe.pipeU1();

            if (excludeStaticFields.contains(fieldName)) {
                sanitizeCommand.getClearedInstanceFieldCounter(className, fieldName).incrementAndGet();
                LOGGER.debug("Removed value of static field '{}' from class '{}'", fieldName, className);
                final int valueSize = BasicType.findValueSize(fieldType, pipe.getIdSize());
                pipe.pipeReplaceByZero(valueSize);
            } else {
                pipeStaticField(pipe, fieldType);
            }
        }

        final int numInstanceFields = pipe.pipeU2();
        for (int i = 0; i < numInstanceFields; i++) {
            final long fieldNameStringId = pipe.pipeId();
            final int fieldType = pipe.pipeU1();
            final String fieldName = stringIdToStringMap.getOrDefault(fieldNameStringId, "");
            final BasicType basicType = BasicType.findByU1Code(fieldType).orElseThrow(IllegalStateException::new);
            classObject.fields.add(new Field(fieldName, basicType));
        }
    }

    private boolean isAssignableClassWithFieldToClear(final long classObjectId) {
        final String className = getClassName(classObjectId);
        return getClassNameHierarchy(className)
                .anyMatch(sanitizeCommand::isExactClassWithFieldToClear);
    }

    private void pipeStaticField(final Pipe pipe, final int entryType) throws IOException {
        final int valueSize = BasicType.findValueSize(entryType, pipe.getIdSize());
        pipe.pipe(valueSize);
    }

    private void pipeBasicType(final Pipe pipe, final int entryType) throws IOException {
        final int valueSize = BasicType.findValueSize(entryType, pipe.getIdSize());
        pipe.pipe(valueSize);
    }

    /*
     *
     * INSTANCE DUMP    0x21
     *
     * ID  object ID
     * u4  stack trace serial number
     * ID  class object ID
     * u4  number of bytes that follow
     * [value]*  instance field values (this class, followed by super class, etc)
     */
    private void copyHeapDumpInstanceDump(final Pipe pipe, final long objectId) throws IOException {
        pipe.pipeU4();
        final long classObjectId = pipe.pipeId();
        final long numBytes = pipe.pipeU4();
        final String className = getClassName(classObjectId);

        if (isAssignableClassWithFieldToClear(classObjectId)) {
            copyInstanceWithFieldsCleared(pipe, className, numBytes);
        } else {
            pipe.pipe(numBytes);
        }
    }

    private Stream<ClassObject> getClassHierarchy(final String className) {
        ClassObject classObject = classNameToClassObjectsMap.get(className);
        Stream<ClassObject> stream = Stream.of();
        while (classObject != null) {
            stream = Stream.concat(stream, Stream.of(classObject));
            classObject = classNameToClassObjectsMap.get(getClassName(classObject.superClassObjectId));
        }
        return stream;
    }

    private Stream<String> getClassNameHierarchy(final String className) {
        return getClassHierarchy(className)
                .map(classObject -> classObject.id)
                .map(this::getClassName);
    }

    private Stream<Field> getAllFieldsInClassHierarchy(final String className) {
        return getClassHierarchy(className).flatMap(classObject -> classObject.fields.stream());
    }

    private Collection<String> getFieldsToClearInClassHierarchy(final String className) {
        return getClassNameHierarchy(className)
                .map(sanitizeCommand::getFieldsToClear)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<String> getClassFieldsToClear() {
        return sanitizeCommand.getClassFieldsToClearList();
    }

    public AtomicInteger getClearedInstanceFieldCounter(String className, String fieldName) {
        return sanitizeCommand.getClearedInstanceFieldCounter(className, fieldName);
    }

    private void copyInstanceWithFieldsCleared(final Pipe pipe, final String className, final long numBytes) throws IOException {
        final Collection<String> excludeStringFields = getFieldsToClearInClassHierarchy(className);
        final ClassObject classObject = classNameToClassObjectsMap.get(className);
        final MutableLong numBytesMutable = new MutableLong(numBytes);
        Objects.requireNonNull(classObject);
        getAllFieldsInClassHierarchy(className).forEach(field -> {
            final int fieldSize = field.type.getValueSize(pipe.getIdSize());

            if (excludeStringFields.contains(field.name)) {
                int valueSize = field.type.getValueSize(pipe.getIdSize());
                sanitizeCommand.getClearedInstanceFieldCounter(className, field.name).incrementAndGet();
                LOGGER.debug("Removed value of field '{}' from class instance '{}'", field.name, className);
                Failable.call(() -> {
                    pipe.pipeReplaceByZero(valueSize);
                    return null;
                });
            } else {
                Failable.run(() -> pipe.pipe(fieldSize));
            }
            numBytesMutable.subtract(fieldSize);
        });

        pipe.pipe(numBytesMutable.longValue());
    }

    private String getClassName(final long classObjectId) {
        final Long stringId = classObjectIdToStringIdMap.get(classObjectId);
        return stringIdToStringMap.getOrDefault(stringId, "");
    }

    private void copyHeapDumpObjectArrayDump(final Pipe pipe) throws IOException {
        pipe.pipeU4();
        final long numElements = pipe.pipeU4();
        pipe.pipeId();
        for (long i = 0; i < numElements; i++) {
            pipe.pipeId();
        }
    }

    /*
     * PRIMITIVE ARRAY DUMP	 * 	0x23
     * 	ID	array object ID
     * 	u4	stack trace serial number
     * 	u4	number of elements
     * 	u1	element type (See Basic Type)
     * 	[u1]*	elements (packed array)
     */
    private void copyHeapDumpPrimitiveArrayDump(final Pipe pipe, final long objectId) throws IOException {
        pipe.pipeU4();
        final long numElements = pipe.pipeU4();

        final int elementType = pipe.pipeU1();
        final long elementSize = BasicType.findValueSize(elementType, pipe.getIdSize());

        final long numBytes = Math.multiplyExact(numElements, elementSize);

        pipe.pipe(numBytes);
    }
}
