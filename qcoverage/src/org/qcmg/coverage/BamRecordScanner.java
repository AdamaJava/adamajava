/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.util.BlockCompressedInputStream;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.qcmg.picard.SAMFileReaderFactory;

/**
 * Reads the records of a single reference sequence from a BAM file.
 * <p>
 * Only those fields of each record that are required to calculate the coverage are
 * decoded, which avoids the cost of creating a SAMRecord for every read. The records
 * that are returned are the same as those returned by htsjdk when the BAM file is
 * queried for the whole of a reference sequence.
 * <p>
 * The reads of a reference sequence are located using the file pointers that are
 * recorded in the BAM index, so the index does not have to be parsed by each of the
 * jobs that process the BAM file.
 */
final class BamRecordScanner implements Closeable {

	/** the number of bytes of the fixed length part of a BAM record */
	private static final int FIXED_BLOCK_SIZE = 32;

	/** the number of bytes used by each CIGAR operation */
	private static final int CIGAR_SIZE_MULTIPLIER = 4;

	/** the number of CIGAR operations of a record that uses a sentinel CIGAR */
	private static final int LONG_CIGAR_SENTINEL_LENGTH = 2;

	// the offsets of the fields of the fixed length part of a BAM record
	private static final int REFERENCE_INDEX_OFFSET = 0;
	private static final int POSITION_OFFSET = 4;
	private static final int READ_NAME_LENGTH_OFFSET = 8;
	private static final int CIGAR_OPERATION_COUNT_OFFSET = 12;
	private static final int FLAGS_OFFSET = 14;
	private static final int READ_LENGTH_OFFSET = 16;
	private static final int INSERT_SIZE_OFFSET = 28;

	// the CIGAR operation codes
	private static final int CIGAR_MATCH = 0;
	private static final int CIGAR_INSERT = 1;
	private static final int CIGAR_DELETE = 2;
	private static final int CIGAR_SKIP = 3;
	private static final int CIGAR_SOFT_CLIP = 4;
	private static final int CIGAR_HARD_CLIP = 5;
	private static final int CIGAR_PADDING = 6;
	private static final int CIGAR_MATCH_EQUAL = 7;
	private static final int CIGAR_MATCH_DIFFERENT = 8;

	// the SAM flags that are of interest
	private static final int FLAG_PAIRED = 0x1;
	private static final int FLAG_UNMAPPED = 0x4;

	/** the position of a read that is not placed on the reference sequence */
	private static final int NO_ALIGNMENT_START = 0;

	private final BlockCompressedInputStream stream;
	private final long[] filePointers;
	private final int refIndex;

	/** the index of the next file pointer to be used */
	private int filePointerIndex = 0;

	/** the virtual file pointer at which the current chunk of the BAM file ends */
	private long filePointerLimit = -1;

	/** the buffer into which the records are read */
	private byte[] recordBuffer = new byte[4096];

	/** the buffer into which the length of each record is read */
	private final byte[] lengthBuffer = new byte[4];

	private int alignmentStart;
	private int alignmentEnd;
	private int insertSize;
	private int readLength;
	private boolean paired;

	/**
	 * Creates a scanner for the reads of the given reference sequence.
	 *
	 * @param bamFile the BAM file to read
	 * @param filePointers the file pointers of the reads of the reference sequence, in the
	 *        format used by the BAM index: the start and the end of each chunk of the file
	 * @param refIndex the index of the reference sequence within the BAM header
	 * @throws IOException if the BAM file could not be opened
	 */
	BamRecordScanner(final File bamFile, final long[] filePointers, final int refIndex) throws IOException {
		this.stream = SAMFileReaderFactory.createBlockCompressedInputStream(bamFile);
		this.filePointers = filePointers;
		this.refIndex = refIndex;
	}

	/**
	 * Advances to the next read of the reference sequence being processed.
	 *
	 * @return true if a read is available, false when there are no more reads
	 * @throws IOException if the BAM file could not be read
	 * @throws UnsupportedBamFeatureException if the read contains a feature that this
	 *         scanner does not support
	 */
	boolean next() throws IOException {
		while (readRecord()) {
			final int recordRefIndex = getInt(REFERENCE_INDEX_OFFSET);
			if (recordRefIndex < refIndex) {
				// htsjdk ends the iteration when it encounters a read that belongs to an
				// earlier reference sequence
				return false;
			}
			if (recordRefIndex > refIndex) {
				// reads that belong to a later reference sequence are ignored
				continue;
			}

			final int flags = getUnsignedShort(FLAGS_OFFSET);
			final int recordReadLength = getInt(READ_LENGTH_OFFSET);
			final int cigarOperationCount = getUnsignedShort(CIGAR_OPERATION_COUNT_OFFSET);
			final int cigarOffset = FIXED_BLOCK_SIZE + getUnsignedByte(READ_NAME_LENGTH_OFFSET);

			int referenceLength = 0;
			for (int i = 0; i < cigarOperationCount; i++) {
				final int cigarElement = getInt(cigarOffset + i * CIGAR_SIZE_MULTIPLIER);
				final int operation = cigarElement & 0xf;
				switch (operation) {
					case CIGAR_MATCH:
					case CIGAR_DELETE:
					case CIGAR_SKIP:
					case CIGAR_MATCH_EQUAL:
					case CIGAR_MATCH_DIFFERENT:
						referenceLength += cigarElement >>> 4;
						break;
					case CIGAR_INSERT:
					case CIGAR_SOFT_CLIP:
					case CIGAR_HARD_CLIP:
					case CIGAR_PADDING:
						break;
					default:
						throw new UnsupportedBamFeatureException("Unsupported CIGAR operation: " + operation);
				}
			}
			if (isSentinelCigar(cigarOffset, cigarOperationCount, recordReadLength)) {
				throw new UnsupportedBamFeatureException("Read uses a sentinel CIGAR");
			}

			final int recordAlignmentStart = getInt(POSITION_OFFSET) + 1;
			final boolean unmapped = (flags & FLAG_UNMAPPED) != 0;

			this.alignmentStart = recordAlignmentStart;
			this.alignmentEnd = unmapped ? NO_ALIGNMENT_START : recordAlignmentStart + referenceLength - 1;
			this.insertSize = getInt(INSERT_SIZE_OFFSET);
			this.readLength = recordReadLength;
			this.paired = (flags & FLAG_PAIRED) != 0;

			// htsjdk ignores those reads that end before the start of the queried interval,
			// which for a whole reference sequence is the start of that sequence
			final int intervalEnd = (unmapped && recordAlignmentStart != NO_ALIGNMENT_START)
					? recordAlignmentStart
					: this.alignmentEnd;
			if (intervalEnd < 0) {
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * @return the first position of the current read on the reference sequence (1 based,
	 *         inclusive)
	 */
	int getAlignmentStart() {
		return alignmentStart;
	}

	/**
	 * @return the last position of the current read on the reference sequence (1 based,
	 *         inclusive). Zero if the read is unmapped.
	 */
	int getAlignmentEnd() {
		return alignmentEnd;
	}

	/**
	 * @return the size of the insert that the current read is part of
	 */
	int getInsertSize() {
		return insertSize;
	}

	/**
	 * @return the number of bases in the current read
	 */
	int getReadLength() {
		return readLength;
	}

	/**
	 * @return true if the current read is part of a pair
	 */
	boolean isPaired() {
		return paired;
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	/**
	 * Reads the next record of the BAM file.
	 * <p>
	 * The records are read from the chunks of the BAM file that contain the reads of the
	 * reference sequence being processed. When the current chunk has been exhausted the
	 * next one is opened.
	 *
	 * @return true if a record was read, false when the end of the file was reached
	 */
	private boolean readRecord() throws IOException {
		while (stream.getFilePointer() >= filePointerLimit) {
			if (filePointerIndex >= filePointers.length) {
				return false;
			}
			final long chunkStart = filePointers[filePointerIndex++];
			final long chunkEnd = filePointers[filePointerIndex++];
			stream.seek(chunkStart);
			filePointerLimit = chunkEnd;
		}

		if (readFully(lengthBuffer, 0, lengthBuffer.length) < lengthBuffer.length) {
			// the end of the file has been reached
			return false;
		}
		final int recordLength = getInt(lengthBuffer, 0);
		if (recordLength < FIXED_BLOCK_SIZE) {
			throw new SAMFormatException("Invalid record length: " + recordLength);
		}
		if (recordBuffer.length < recordLength) {
			recordBuffer = Arrays.copyOf(recordBuffer, recordLength);
		}
		if (readFully(recordBuffer, 0, recordLength) < recordLength) {
			throw new EOFException("Premature end of file");
		}
		return true;
	}

	/**
	 * Indicates whether or not the CIGAR of the current record is a sentinel CIGAR, which
	 * is used when the real CIGAR is too long to be stored in the BAM file.
	 *
	 * @param cigarOffset the offset of the CIGAR operations within the current record
	 * @param cigarOperationCount the number of CIGAR operations of the current record
	 * @param recordReadLength the number of bases in the current read
	 * @return true if the CIGAR is a sentinel CIGAR
	 */
	private boolean isSentinelCigar(final int cigarOffset, final int cigarOperationCount, final int recordReadLength) {
		if (cigarOperationCount != LONG_CIGAR_SENTINEL_LENGTH) {
			return false;
		}
		final int first = getInt(cigarOffset);
		final int second = getInt(cigarOffset + CIGAR_SIZE_MULTIPLIER);
		return (first & 0xf) == CIGAR_SOFT_CLIP
				&& (second & 0xf) == CIGAR_SKIP
				&& ((first >>> 4) == recordReadLength || recordReadLength == 0);
	}

	/**
	 * Reads the requested number of bytes from the BAM file.
	 *
	 * @return the number of bytes that were read
	 */
	private int readFully(final byte[] buffer, final int offset, final int length) throws IOException {
		int total = 0;
		while (total < length) {
			final int read = stream.read(buffer, offset + total, length - total);
			if (read < 0) {
				break;
			}
			total += read;
		}
		return total;
	}

	private int getInt(final int offset) {
		return getInt(recordBuffer, offset);
	}

	private int getUnsignedShort(final int offset) {
		return (recordBuffer[offset] & 0xff) | ((recordBuffer[offset + 1] & 0xff) << 8);
	}

	private int getUnsignedByte(final int offset) {
		return recordBuffer[offset] & 0xff;
	}

	/**
	 * Reads a little endian, four byte, signed integer.
	 */
	private static int getInt(final byte[] buffer, final int offset) {
		return (buffer[offset] & 0xff)
				| ((buffer[offset + 1] & 0xff) << 8)
				| ((buffer[offset + 2] & 0xff) << 16)
				| ((buffer[offset + 3] & 0xff) << 24);
	}
}
