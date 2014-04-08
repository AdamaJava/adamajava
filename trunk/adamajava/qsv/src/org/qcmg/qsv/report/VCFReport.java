/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
//package org.qcmg.qsv.report;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import org.qcmg.qsv.QSVCluster;
//import org.qcmg.qsv.util.QSVUtil;
//
//public class VCFReport extends QSVReport {
//    
//    private List<QSVCluster> qsvRecords;
//    private String fileFormat = "VCF4.1";
//    private Date fileDate;
//    private String reference = "";
//    private String assembly = "";
//    private List<String> infos;
//    private List<String> alts;
//    private List<String> formats;
//    private String sampleId;
//	private boolean isSingleSided;
//	private String tumourFindType;
//	private String normalFindType;
//    
//    public VCFReport(String base, String type, String sampleId, Date analysisDate, String reference, String tType, String nType, List<QSVCluster> qsvRecords, boolean isSingleSided) throws Exception {
//        super(new File(base + "." + type + "vcf"));        
//        this.append = true;
//        this.qsvRecords = qsvRecords;
//        this.fileDate = analysisDate;
//        this.reference =reference;
//        this.isSingleSided = isSingleSided;
//        createInfos();
//        this.alts = new ArrayList<String>();
//        this.formats = new ArrayList<String>();
//        this.sampleId= sampleId;
//        this.normalFindType = nType;
//        this.tumourFindType = tType;
//        writeReport();
//    }
//    
//    private void createInfos() {
//        this.infos = new ArrayList<String>();
//        infos.add(createInfo("MUT",".","String", "mutation_type: somatic or germline"));
//        infos.add(createInfo("ANID",".","String", "analysis_id"));
//        infos.add(createInfo("TSAMPLE",".","String", "tumour_sample_id"));
//        infos.add(createInfo("SVTYPE",".","String", "variant_type"));
//        infos.add(createInfo("STRANDFROM",".","Integer", "strand)from"));
//        infos.add(createInfo("TCHROM",".","Integer", "chr_to"));
//        infos.add(createInfo("TPOS",".","Integer", "chr_to_bkpt"));
//        infos.add(createInfo("STRANDTO",".","Integer", "strand)from"));
//        infos.add(createInfo("NT",".","Integer", "disease_discordant_pairs_count" ));
//        infos.add(createInfo("NN",".","Integer", "control_discordant_pairs_count" ));
//        infos.add(createInfo("NNLOW",".","Integer", "control_low_qual_discordant_reads_count"));
//        infos.add(createInfo("NTCLIP",".","String", "disease_clip_count"));
//        infos.add(createInfo("NNCLIP",".","String",  "control_clip_count"));
//        infos.add(createInfo("POSFROM",".","String", "chr_from_region"));
//        infos.add(createInfo("CATEGORY",".","String", "category based on discordant pairs and clipping signature")); 
//        infos.add(createInfo("MICROHOMOLOGY",".","String", "microhomology"));
//        infos.add(createInfo("NONTEMPLATE",".","String", "nontemplate")); 
//        infos.add(createInfo("SPLITREAD",".","String", "split_read_bp")); 
//        infos.add(createInfo("CONTIG",".","String", "contig")); 
//    }
//  
//    public String createInfo(String id, String number, String type, String description) {
//        String info = "##INFO=<ID=";
//        info += id;
//        info += ",Number="+ number;
//        info += ",Type=" + type;
//        info += ",Description=\"" + description + "\"";
//        info += ">" + QSVUtil.getNewLine();
//        return info;
//    }
//    
//    public String getHeader() {
//        StringBuffer sb = new StringBuffer();
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
//        
//        sb.append("##fileformat=" + fileFormat + QSVUtil.getNewLine());
//        sb.append("##fileDate=" + formatter.format(fileDate) + QSVUtil.getNewLine());
//        sb.append("##reference=" + reference + QSVUtil.getNewLine());
//        sb.append("##assembly=" + assembly + QSVUtil.getNewLine());
//        for (String info : infos) {            
//            sb.append(info);
//        }
//
//        sb.append("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO" + QSVUtil.getNewLine());
//        return sb.toString();
//    }
//    
//    @Override 
//    public void writeReport() throws Exception {
//        
//        boolean append = false;
//        if (!file.exists()) {
//            append = false;
//        } else {
//            append = true;
//        }
//
//        BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
//
//        // print header for the file.
//        if (!append) {
//            writer.write(getHeader());
//        }      
//            
//        for (QSVCluster r : qsvRecords) {
//        	if (isSingleSided || (!isSingleSided && !r.singleSidedClip())) {
//        		writer.write(r.toVCFString() + QSVUtil.getNewLine());
//        	}
//        }
//        
//        writer.flush();
//        writer.close();
//    }
//
//	@Override
//	public void writeHeader() throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
