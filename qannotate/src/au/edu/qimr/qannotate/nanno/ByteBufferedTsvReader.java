package au.edu.qimr.qannotate.nanno;

import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.qio.record.RecordSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * Forward-only UTF-8 TSV reader for files with chromosome and position in the first two columns.
 * Skipped rows are scanned as bytes; only headers and rows at requested positions become Strings.
 * Each instance is confined to one annotation-source worker at a time.
 */
final class ByteBufferedTsvReader implements RecordSource<String> {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final VarHandle LONG_VIEW = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final long BYTE_ONES = 0x0101010101010101L;
    private static final long BYTE_HIGH_BITS = 0x8080808080808080L;
    private static final long LF_BYTES = 0x0a0a0a0a0a0a0a0aL;
    private static final long CR_BYTES = 0x0d0d0d0d0d0d0d0dL;

    private final File file;
    private final InputStream input;
    private final boolean chrStartsWithChr;
    private final byte[] buffer;
    private final List<String> headers = new ArrayList<>();
    private final ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
    private byte[] prefix = new byte[64];
    private byte[] cachedContig = new byte[0];
    private int cachedContigNumber;
    private int cursor;
    private int limit;
    private int prefixStart = -1;
    private int prefixLength;
    private boolean positionReady;
    private long position;

    ByteBufferedTsvReader(File file, boolean chrStartsWithChr) throws IOException {
        this(file, chrStartsWithChr, BUFFER_SIZE);
    }

    // Smaller buffers allow tests to exercise boundaries inside fields, UTF-8 characters and CRLF.
    ByteBufferedTsvReader(File file, boolean chrStartsWithChr, int bufferSize) throws IOException {
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size must be positive");
        this.file = file;
        this.chrStartsWithChr = chrStartsWithChr;
        this.buffer = new byte[bufferSize];
        boolean gzip = FileUtils.isInputGZip(file);
        InputStream stream = new FileInputStream(file);
        try {
            this.input = gzip ? new GZIPInputStream(stream, BUFFER_SIZE) : stream;
        } catch (IOException | RuntimeException e) {
            try {
                stream.close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
        try {
            while (available() && buffer[cursor] == '#') {
                headers.add(readLine());
            }
        } catch (IOException | RuntimeException e) {
            try {
                input.close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public List<String> getHeader() {
        return headers;
    }

    /** Retains all rows at a position so the existing matcher can resolve REF/ALT and duplicates. */
    List<String> readRecordsAt(long requestedPosition) {
        try {
            if (requestedPosition == -1) {
                while (readPosition()) consumeLine(false);
                return List.of();
            }

            // Most source rows are skipped. Keep collection and its allocation out of this loop.
            while (readPosition() && position < requestedPosition) consumeLine(false);
            if (!positionReady || position != requestedPosition) return List.of();

            List<String> records = new ArrayList<>(4);
            do {
                records.add(readLine());
            } while (readPosition() && position == requestedPosition);
            return records;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    long nextPosition() {
        try {
            return readPosition() ? position : Long.MAX_VALUE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean available() throws IOException {
        while (cursor == limit) {
            int count = input.read(buffer);
            if (count < 0) return false;
            cursor = 0;
            limit = count;
        }
        return true;
    }

    /** Reads only the first two tab-delimited fields, without allocating per-row objects. */
    private boolean readPosition() throws IOException {
        if (positionReady) return true;
        if (!available()) return false;

        // Normally both fields are already buffered. Parse them in place and leave their
        // bytes here until readLine needs them; skipped rows never need a prefix copy.
        int firstTab = -1;
        for (int end = cursor; end < limit; end++) {
            byte value = buffer[end];
            if (value == '\n' || value == '\r') break; // The boundary path reports malformed rows.
            if (value == '\t') {
                if (firstTab < 0) {
                    firstTab = end;
                } else {
                    prefixStart = cursor;
                    prefixLength = end + 1 - cursor;
                    cursor = end + 1;
                    setPosition(buffer, prefixStart, firstTab, end);
                    return true;
                }
            }
        }

        // A prefix crossing a buffer boundary must be saved before refilling the buffer.
        prefixStart = -1;
        prefixLength = 0;
        firstTab = -1;
        while (available()) {
            byte value = buffer[cursor++];
            if (value == '\n' || value == '\r') {
                throw new IllegalArgumentException("Missing chromosome/position columns in " + file);
            }
            if (prefixLength == prefix.length) prefix = Arrays.copyOf(prefix, prefix.length * 2);
            prefix[prefixLength++] = value;
            if (value == '\t') {
                if (firstTab < 0) {
                    firstTab = prefixLength - 1;
                } else {
                    setPosition(prefix, 0, firstTab, prefixLength - 1);
                    return true;
                }
            }
        }
        throw new IllegalArgumentException("Missing chromosome/position columns in " + file);
    }

    private void setPosition(byte[] bytes, int start, int firstTab, int secondTab) {
        int contigStart = start + (chrStartsWithChr ? 3 : 0);
        int contigLength = firstTab - contigStart;
        if (contigLength <= 0) {
            throw new IllegalArgumentException("Missing chromosome in " + file);
        }
        if (cachedContig.length != contigLength
                || !Arrays.equals(bytes, contigStart, firstTab, cachedContig, 0, cachedContig.length)) {
            cachedContig = Arrays.copyOfRange(bytes, contigStart, firstTab);
            cachedContigNumber = ChrPositionUtils.convertContigNameToInt(
                    new String(cachedContig, StandardCharsets.UTF_8));
        }
        int startPosition = parsePosition(bytes, firstTab + 1, secondTab);
        position = ((long) cachedContigNumber << 32) + startPosition;
        positionReady = true;
    }

    private int parsePosition(byte[] bytes, int start, int end) {
        boolean negative = start < end && bytes[start] == '-';
        if (start < end && (negative || bytes[start] == '+')) start++;
        if (start == end) throw new NumberFormatException("Missing position in " + file);
        // Accumulate negatively to support the same integer range as Integer.parseInt.
        int lowerBound = negative ? Integer.MIN_VALUE : -Integer.MAX_VALUE;
        int value = 0;
        for (int i = start; i < end; i++) {
            int digit = bytes[i] - '0';
            if (digit < 0 || digit > 9 || value < lowerBound / 10) {
                throw new NumberFormatException("Invalid or overflowing position in " + file);
            }
            value *= 10;
            if (value < lowerBound + digit) {
                throw new NumberFormatException("Overflowing position in " + file);
            }
            value -= digit;
        }
        return negative ? value : -value;
    }

    private String readLine() throws IOException {
        lineBytes.reset();
        if (positionReady) {
            if (prefixStart >= 0) lineBytes.write(buffer, prefixStart, prefixLength);
            else lineBytes.write(prefix, 0, prefixLength);
        }
        consumeLine(true);
        return lineBytes.toString(StandardCharsets.UTF_8);
    }

    /** Consumes the remaining row in buffer-sized spans, optionally retaining its bytes. */
    private void consumeLine(boolean retain) throws IOException {
        while (available()) {
            int start = cursor;
            cursor = findLineEnd(buffer, start, limit);
            if (retain) lineBytes.write(buffer, start, cursor - start);
            if (cursor < limit) {
                byte terminator = buffer[cursor++];
                if (terminator == '\r' && available() && buffer[cursor] == '\n') cursor++;
                break;
            }
        }
        positionReady = false;
        prefixStart = -1;
        prefixLength = 0;
    }

    /** Finds the first CR or LF in [start, end), testing eight bytes per iteration. */
    static int findLineEnd(byte[] bytes, int start, int end) {
        int index = start;
        for (int lastWord = end - Long.BYTES; index <= lastWord; index += Long.BYTES) {
            long word = (long) LONG_VIEW.get(bytes, index);
            long lf = word ^ LF_BYTES;
            long cr = word ^ CR_BYTES;
            long matches = (((lf - BYTE_ONES) & ~lf) | ((cr - BYTE_ONES) & ~cr)) & BYTE_HIGH_BITS;
            if (matches != 0) {
                // Subtraction can flag bytes after a match. With little-endian loads, the
                // least-significant flagged byte is always the first actual terminator.
                return index + (Long.numberOfTrailingZeros(matches) >>> 3);
            }
        }
        while (index < end && bytes[index] != '\n' && bytes[index] != '\r') index++;
        return index;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return nextPosition() != Long.MAX_VALUE;
            }

            @Override
            public String next() {
                if (!hasNext()) throw new NoSuchElementException();
                try {
                    return readLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
