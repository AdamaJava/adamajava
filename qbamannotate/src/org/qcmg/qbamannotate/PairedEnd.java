/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import htsjdk.samtools.SAMRecord;

import org.qcmg.ma.MADirection;
import org.qcmg.ma.MARecord;

public class PairedEnd extends AnnotatorType {
	final int isizeLowerLimit;
	final int isizeUpperLimit;
	private String zpAnnotation;
	private SAMRecord record;
	private MARecord maRecord;
	private int annotatedCount = 0;
	private final Map<String, Integer> zpToCount = new HashMap<String, Integer>();
	private String xmlReport;

	public PairedEnd(int isizeLowerLimit, int isizeUpperLimit) throws Exception {
		if (isizeLowerLimit >= isizeUpperLimit) {
			throw new Exception(
					"Lower interval size cannot be equal-to-or-greater-than upper interval size");
		}
		this.isizeLowerLimit = isizeLowerLimit;
		this.isizeUpperLimit = isizeUpperLimit;
	}

	public void resetCount() {
		zpToCount.clear();
		annotatedCount = 0;
	}

	@Override
	public boolean annotate(final SAMRecord record) throws Exception {
		this.record = record;
		boolean result = false;
		if (record.getReadPairedFlag()) {
			result = createZPAnnotation();
		} else {
			record.setAttribute("ZP", "Z**");
			annotatedCount++;
		}
		return result;
	}

	@Override
	public boolean annotate(SAMRecord record, MARecord maRecord)
			throws Exception {
		this.record = record;
		this.maRecord = maRecord;
		performZMAnnotation();
		return true;
	}

	public String generateReport() throws Exception {
		PairedEndReport report = new PairedEndReport();
		PairedEndReport.InsertRange range = new PairedEndReport.InsertRange();
		range.setLowerLimit(BigInteger.valueOf(this.isizeLowerLimit));
		range.setUpperLimit(BigInteger.valueOf(this.isizeUpperLimit));
		report.setInsertRange(range);
		for (String key : zpToCount.keySet()) {
			Integer count = zpToCount.get(key);
			PairedEndReport.UniquePairing pairing = new PairedEndReport.UniquePairing();
			pairing.setType(key);
			pairing.setCount(BigInteger.valueOf(count.longValue()));
			report.getUniquePairing().add(pairing);
		}
		StringWriter writer = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(PairedEndReport.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); // pretty print
																// XML
		m.marshal(report, writer);
		xmlReport = writer.toString();
		return xmlReport;
	}

	private void performZMAnnotation() {
		assert 0 == compareTriplet(record, maRecord);
		if (maRecord.getDefLine().getDirection() == MADirection.F3
				&& record.getReadPairedFlag() && record.getFirstOfPairFlag()) {
			setZMAttribute();
		} else if (maRecord.getDefLine().getDirection() == MADirection.F5
				&& record.getReadPairedFlag() && record.getSecondOfPairFlag()) {
			setZMAttribute();
		} else {
			markRecordUnmatched(record);
		}
	}

	private void setZMAttribute() {
		int n = maRecord.getDefLine().getNumberMappings();
		setZMAttribute(record, n);
	}

	private boolean isReadF3() {
		return record.getFirstOfPairFlag();
	}

	private boolean isReadF5() {
		return record.getSecondOfPairFlag();
	}

	private boolean isReadLeftOfMate() {
		return record.getAlignmentStart() < record.getMateAlignmentStart();
	}

	private boolean isReadRightOfMate() {
		return record.getAlignmentStart() > record.getMateAlignmentStart();
	}

	private boolean isReadForward() {
		return !record.getReadNegativeStrandFlag();
	}

	private boolean isReadReverse() {
		return !isReadForward();
	}

	private boolean isMateForward() {
		return !record.getMateNegativeStrandFlag();
	}

	private boolean isMateReverse() {
		return !isMateForward();
	}

	private boolean isSameStrand() {
		return record.getReadNegativeStrandFlag() == record
				.getMateNegativeStrandFlag();
	}

	private boolean isDifferentStrand() {
		return record.getReadNegativeStrandFlag() != record
				.getMateNegativeStrandFlag();
	}

	private boolean isF5toF3() {
		boolean result = false;
		if (isReadF5() && isReadLeftOfMate() && isReadForward()
				&& isMateForward()) {
			result = true;
		} else if (isReadF5() && isReadRightOfMate() && isReadReverse()
				&& isMateReverse()) {
			result = true;
		} else if (isReadF3() && isReadRightOfMate() && isReadForward()
				&& isMateForward()) {
			result = true;
		} else if (isReadF3() && isReadLeftOfMate() && isReadReverse()
				&& isMateReverse()) {
			result = true;
		}
		return result;
	}

	private boolean isF3toF5() {
		boolean result = false;
		if (isReadF3() && isReadLeftOfMate() && isReadForward()
				&& isMateForward()) {
			result = true;
		} else if (isReadF3() && isReadRightOfMate() && isReadReverse()
				&& isMateReverse()) {
			result = true;
		} else if (isReadF5() && isReadRightOfMate() && isReadForward()
				&& isMateForward()) {
			result = true;
		} else if (isReadF5() && isReadLeftOfMate() && isReadReverse()
				&& isMateReverse()) {
			result = true;
		}
		return result;
	}

	private boolean isInward() {
		boolean result = false;
		if (isReadLeftOfMate() && isReadForward() && isMateReverse()) {
			result = true;
		} else if (isReadRightOfMate() && isReadReverse() && isMateForward()) {
			result = true;
		}
		return result;
	}

	private boolean isOutward() {
		boolean result = false;
		if (isReadRightOfMate() && isReadForward() && isMateReverse()) {
			result = true;
		} else if (isReadLeftOfMate() && isReadReverse() && isMateForward()) {
			result = true;
		}
		return result;
	}

	private boolean isDistanceTooSmall() {
		int absoluteISize = Math.abs(record.getInferredInsertSize());
		return 0 <= absoluteISize && absoluteISize < isizeLowerLimit;
	}

	private boolean isDistanceNormal() {
		int absoluteISize = Math.abs(record.getInferredInsertSize());
		return isizeLowerLimit <= absoluteISize
				&& absoluteISize <= isizeUpperLimit;
	}

	private boolean isDistanceTooLarge() {
		int absoluteISize = Math.abs(record.getInferredInsertSize());
		return absoluteISize > isizeUpperLimit;
	}

	private boolean createZPAnnotation() {
		boolean result = false;
		Integer nh = record.getIntegerAttribute("NH");
		if (!record.getDuplicateReadFlag()) {
			if (null != nh && 1 == nh && record.getMateUnmappedFlag()) {
				zpAnnotation = "D**";
				record.setAttribute("ZP", zpAnnotation);
				countZp();
				result = true;
				annotatedCount++;
			} else if (null != nh
					&& 1 == nh
					&& !record.getReferenceName().equals(
							record.getMateReferenceName())
					&& !record.getMateReferenceName().equals("=")) {
				zpAnnotation = "C**";
				record.setAttribute("ZP", zpAnnotation);
				if (record.getFirstOfPairFlag()) {
					countZp();
					result = true;
				}
				annotatedCount++;
			} else if (null != nh && 1 == nh
					&& record.getReadFailsVendorQualityCheckFlag()) {
				zpAnnotation = "E**";
				record.setAttribute("ZP", zpAnnotation);
				if (record.getFirstOfPairFlag()) {
					countZp();
					result = true;
				}
				annotatedCount++;
			} else if (null != nh && 1 == nh && isDifferentStrand()) {
				zpAnnotation = "A";
				handleOrientation();
				handleIntervalSize();
				record.setAttribute("ZP", zpAnnotation);
				if (record.getFirstOfPairFlag()) {
					countZp();
					result = true;
				}
				annotatedCount++;
			} else if (null != nh && 1 != nh && isDifferentStrand()) {
				zpAnnotation = "A";
				handleOrientation();
				handleIntervalSize();
				if (zpAnnotation.equals("AAA")) {
					record.setAttribute("ZP", zpAnnotation);
					if (record.getFirstOfPairFlag()) {
						countZp();
						result = true;
					}
				} else {
					record.setAttribute("ZP", "Z**");
				}
				annotatedCount++;
			} else if (null == nh && isDifferentStrand()) {
				zpAnnotation = "A";
				handleOrientation();
				handleIntervalSize();
				if (zpAnnotation.equals("AAA")) {
					record.setAttribute("ZP", zpAnnotation);
					if (record.getFirstOfPairFlag()) {
						countZp();
						result = true;
					}
				} else {
					record.setAttribute("ZP", "Z**");
				}
				annotatedCount++;
			} else if (null != nh && 1 == nh && isSameStrand()) {
				zpAnnotation = "B";
				handleOrientation();
				handleIntervalSize();
				record.setAttribute("ZP", zpAnnotation);
				if (record.getFirstOfPairFlag()) {
					countZp();
					result = true;
				}
				annotatedCount++;
			} else {
				record.setAttribute("ZP", "Z**");
				annotatedCount++;
			}
		} else {
			record.setAttribute("ZP", "Z**");
			annotatedCount++;
		}
		return result;
	}

	private void countZp() {
		if (null == zpToCount.get(zpAnnotation)) {
			zpToCount.put(zpAnnotation, 1);
		} else {
			Integer count = zpToCount.get(zpAnnotation);
			count++;
			zpToCount.put(zpAnnotation, count);
		}
	}

	private void handleOrientation() {
		if ('A' == zpAnnotation.charAt(0)) {
			if (isOutward()) {
				zpAnnotation += 'B';
			} else if (isInward()) {
				zpAnnotation += 'A';
			} else {
				zpAnnotation += 'X';
			}
		} else if ('B' == zpAnnotation.charAt(0)) {
			if (isF5toF3()) {
				zpAnnotation += 'B';
			} else if (isF3toF5()) {
				zpAnnotation += 'A';
			} else {
				zpAnnotation += 'X';
			}
		}
	}

	private void handleIntervalSize() {
		if (isDistanceNormal()) {
			zpAnnotation += 'A';
		} else if (isDistanceTooSmall()) {
			zpAnnotation += 'B';
		} else if (isDistanceTooLarge()) {
			zpAnnotation += 'C';
		}
	}

}
