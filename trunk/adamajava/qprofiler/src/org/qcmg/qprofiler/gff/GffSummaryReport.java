/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.gff;

import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.gff.GFFRecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.w3c.dom.Element;

public class GffSummaryReport extends SummaryReport {
	
	private SummaryByCycle<Character> baseByCycle = new SummaryByCycle<Character>();
	private SummaryByCycle<Character> colorByCycle = new SummaryByCycle<Character>();
	private SummaryByCycle<Integer> qualityByCycle = new SummaryByCycle<Integer>();

	@Override
	public void toXml(Element parent) {
		Element element = init(parent, ProfileType.gff);
			
		colorByCycle.toXml( element, "ColourByCycle");
		qualityByCycle.toXml( element, "QualityByCycle");
		baseByCycle.toXml( element, "baseByCycle");
	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(GFFRecord gffRecord) {
		updateRecordsParsed();
//		recordsParsed++;
		
		//TODO - use the static methods in SummaryByCycleUtils
		parseBases(gffRecord.getAttribute("b"));
		parseColors(gffRecord.getAttribute("g"));
		parseQualities(gffRecord.getAttribute("q"));
	}
	
	
	/**
	 * Takes the bases and tallies them by cycle
	 * 
	 * @param string a string representing the bases for this read
	 */
	private void parseBases(String basesString) {
		if (null != basesString) {
			for (int i = 1; i <= basesString.length(); i++) {
				Character c = basesString.charAt(i-1);
				baseByCycle.increment(i, c);
			}
		}
	}
	
	private void parseColors(String colorsString) {
		if (null != colorsString) {
			// Need to skip first color because it is T or G
			for (int i = 1; i < colorsString.length(); i++) {
				Character c = colorsString.charAt(i);
				colorByCycle.increment(i, c);
			}
		}
	}

	private void parseQualities(String qualityString) {
		if (null != qualityString) {
			String[] quals = qualityString.split(",");
			for (int i = 1; i <= quals.length; i++) {
				Integer j = Integer.parseInt( quals[i-1] );
				qualityByCycle.increment(i, j);
			}
		}
	}

}
