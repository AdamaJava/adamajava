/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.qsv.QSVCluster;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

public class QSVClusterReport extends QSVReport {
	
 
    protected boolean append = false;
	private String fileType;
	private List<QSVCluster> qsvRecords;
	private String tumourFindType;
	private String normalFindType;
	private boolean isQCMG;
	private boolean isSingleSided;
    private static String TAB = "\t";
    
    public QSVClusterReport(String base, String mutationType, String end, String fileType, List<QSVCluster> qsvRecords, String tumourFindType, String normalFindType, boolean isSingleSided) throws Exception {
        super(new File(base + "." + mutationType + end));
        this.fileType = fileType;
        this.qsvRecords = qsvRecords;
        this.isSingleSided = isSingleSided;
        this.tumourFindType = tumourFindType;
        this.normalFindType = normalFindType;
        if (normalFindType == null) {
        	normalFindType = "";
        }
        
        writeHeader();
        writeReport();
    }  
    public QSVClusterReport(String base, String mutationType, String end, String fileType, List<QSVCluster> qsvRecords, boolean isSingleSided) throws Exception {
        super(new File(base + "." + mutationType + end));
        this.fileType = fileType;
        this.qsvRecords = qsvRecords;
        this.isSingleSided = isSingleSided;
        if (normalFindType == null) {
        	normalFindType = "";
        }
        writeHeader();
        writeReport();
    }    
        
    public QSVClusterReport(String base, String mutationType, String end, String fileType, QSVCluster r, String tumourFindType, String normalFindType, boolean isQCMG, boolean isSingleSided)  throws Exception {    	
    	super(new File(base + "." + r.getReference() + "." + mutationType + end));
        this.fileType = fileType;
        this.qsvRecords = new ArrayList<QSVCluster>();
        this.tumourFindType = tumourFindType;
        this.normalFindType = normalFindType;
        this.isQCMG = isQCMG;
        this.isSingleSided = isSingleSided;
        qsvRecords.add(r);
        writeHeader();
        writeReport();
	}

	public void writeHeader() throws IOException {    	

        // print header for the file.
        if (!file.exists()) { 
        	BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            writer.write(getHeader());
            writer.close();
        }        
    }

    public synchronized void writeReport() throws Exception {
    	 BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
    	 
    	 for (QSVCluster r: qsvRecords) {   
    		
    		 if (r.printRecord(isSingleSided)) {
    			 
    			 String str = r.getDataString(fileType, tumourFindType, normalFindType, isQCMG, "");
    			 if (fileType.equals("softclip")) {   
    				 if (!str.equals("") && r.hasSoftClipEvidence()) {
    					 writer.write(str + QSVUtil.getNewLine());
    				 } 
    			 } else {    				 
    				 writer.write(str +  QSVUtil.getNewLine());
    			 }
    		 }
    	 }
         
         writer.close();
    }
    
	public String getHeader() {		
		
		
		if (fileType.equals("tab")) {
			return "#analysis_id" + TAB + "sample_id" + TAB + "sv_id" + TAB + "sv_type" + TAB + "chr1" + TAB + "pos1" + TAB  + "strand1" + TAB + "chr2" + TAB + "pos2" + TAB + "strand2" + TAB + QSVConstants.DISEASE_SAMPLE + "_discordant_pairs_count" + TAB + QSVConstants.CONTROL_SAMPLE + "_discordant_pairs_count" 
						+ TAB + QSVConstants.CONTROL_SAMPLE + "_low_qual_discordant_reads_count"
                        + TAB + QSVConstants.DISEASE_SAMPLE + "_clips_count_pos1" 
                        + TAB + QSVConstants.DISEASE_SAMPLE + "_clips_count_pos2" 
						+ TAB + QSVConstants.CONTROL_SAMPLE + "_clips_count_pos1" 
						+ TAB + QSVConstants.CONTROL_SAMPLE + "_clips_count_pos2" 
                       + TAB + "category"  
                        + TAB + "microhomology" + TAB + "non-template" +  TAB + "test_split_read_bp" + TAB  + "control_split_read_bp" + TAB + "event_notes" + TAB +  "contig" + QSVUtil.getNewLine();
		} else if (fileType.equals("softclip")){
			return "sv_id" + TAB + "chr1" + TAB + "bp1"  + TAB + "contig1" + 
					TAB + "read_strand_1" + TAB +  "blat_align_chr1" + TAB +  "blat_align_bp1" + TAB + 
					"blat_align_ref_start_pos_1" + TAB + "blat_align_ref_end_pos_1" + TAB + "blat_align_contig_start_1" + TAB + "blat_align_contig_end_1" + TAB +
					"blat_align_strand_1" + 
					TAB + "chr2"  + TAB + "bp2"  + TAB + "contig2" +
					TAB + "read_strand_2" + TAB +  "blat_align_chr2" + TAB +  "blat_align_bp2" + TAB + "blat_align_contig_start_1" + TAB + "blat_align_contig_end_1" + TAB +
					"blat_align_start_pos_2" + TAB + "blat_align_end_pos_2" + TAB + "blat_align_strand_2" + 
					QSVUtil.getNewLine();
		}
		return "";
		
	}

}
