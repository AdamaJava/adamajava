package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.ContentType;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.MafElement;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2mafTest {
    static String inputName = DbsnpModeTest.inputName;        
    
    @org.junit.Rule
    public  TemporaryFolder testFolder = new TemporaryFolder();
    
     @Test
     public void isHC() {
         assertEquals(false, Vcf2maf.isHighConfidence(null));
         assertEquals(false, Vcf2maf.isHighConfidence(new SnpEffMafRecord()));
         SnpEffMafRecord maf = new SnpEffMafRecord();
         maf.setColumnValue(MafElement.Confidence, null);
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         
         maf.setColumnValue(MafElement.Confidence, "");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "blah");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "high");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH_1");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH_1,HIGH");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH_1,HIGH_1");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH_2,HIGH_1");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH_2,HIGH_2");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "HIGH,HIGH");
         assertEquals(false, Vcf2maf.isHighConfidence(maf));
         maf.setColumnValue(MafElement.Confidence, "PASS");
         assertEquals(true, Vcf2maf.isHighConfidence(maf));
     }
     
     @Test
     public void getDetailsFromVcfHeader() {
         VcfHeader h = new VcfHeader();
         assertEquals(false, getBamid("blah", h).isPresent());
         h = createMergedVcfHeader();
         assertEquals(false, getBamid("##qDonorId", h).isPresent());
         assertEquals(true, getBamid("##1:qDonorId", h).isPresent());
     }
     
     
     private Optional<String> getBamid(String key, VcfHeader header){
        for (final VcfHeaderRecord hr : header.getAllMetaRecords()) { 
            if( hr.toString().indexOf(key) != -1) {
                return Optional.ofNullable(StringUtils.getValueFromKey(hr.toString(), key));
            }
        }
        return Optional.empty(); 
    }
     
     private VcfHeader createMergedVcfHeader() {
         VcfHeader h = new VcfHeader();
         
//have to remove empty line "##"         
         Arrays.asList("##fileformat=VCFv4.2",
"##fileDate=20160523",
"##qUUID=209dec81-a127-4aa3-92b4-2c15c21b75c7",
"##qSource=qannotate-2.0 (1170)",
"##1:qUUID=7554fdcc-7230-400e-aefe-5c9a4c79907b",
"##1:qSource=qSNP v2.0 (1170)",
"##1:qDonorId=my_donor",
"##1:qControlSample=my_control_sample",
"##1:qTestSample=my_test_sample",
"##1:qControlBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9031/aligned_read_group_sets/dna_primarytumour_externpsar20150414090_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##1:qControlBamUUID=null",
"##1:qTestBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9014/aligned_read_group_sets/dna_primarytumour_externpsar20150414076_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##1:qTestBamUUID=null",
"##1:qAnalysisId=e3afda85-469f-412b-8919-10cd31d2ca52",
"##2:qUUID=aa7d805f-2ec8-4aea-b1e6-7bc410a41c4b",
"##2:qSource=qSNP v2.0 (1170)",
"##2:qDonorId=my_donor",
"##2:qControlSample=my_control_sample",
"##2:qTestSample=my_test_sample",
"##2:qControlBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9031/aligned_read_group_sets/dna_primarytumour_externpsar20150414090_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##2:qControlBamUUID=null",
"##2:qTestBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9014/aligned_read_group_sets/dna_primarytumour_externpsar20150414076_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##2:qTestBamUUID=null",
"##2:qAnalysisId=3334e934-cb45-4215-9eb5-84b63d96a502",
"##2:qControlVcf=/mnt/lustre/home/oliverH/q3testing/analysis/9/7/97b3715c-0a80-4115-844e-cc877b2cf409/controlGatkHCCV.vcf",
"##2:qControlVcfUUID=null",
"##2:qControlVcfGATKVersion=3.4-46-gbc02625",
"##2:qTestVcf=/mnt/lustre/home/oliverH/q3testing/analysis/c/f/cfccdb1c-6c26-48e9-bd73-ad4ebd806aa6/testGatkHCCV.vcf",
"##2:qTestVcfUUID=null",
"##2:qTestVcfGATKVersion=3.4-46-gbc02625",
"##INPUT=1,FILE=/mnt/lustre/home/oliverH/q3testing/analysis/e/3/e3afda85-469f-412b-8919-10cd31d2ca52/e3afda85-469f-412b-8919-10cd31d2ca52.vcf",
//"##INPUT=2,FILE=/mnt/lustre/home/oliverH/q3testing/analysis/3/3/3334e934-cb45-4215-9eb5-84b63d96a502/3334e934-cb45-4215-9eb5-84b63d96a502.vcf").stream().forEach(h::parseHeaderLine);
"##INPUT=2,FILE=/mnt/lustre/home/oliverH/q3testing/analysis/3/3/3334e934-cb45-4215-9eb5-84b63d96a502/3334e934-cb45-4215-9eb5-84b63d96a502.vcf").stream().forEach(h::addOrReplace);

         return h;
     }
     
     
     
     @Test
     public void compoundSNPTest() {
         /*
          * chr1    11836441        rs71492357      TG      CA      .       .       IN=1,2;DB;EFF=upstream_gene_variant(MODIFIER||2547||615|C1orf167|protein_coding|CODING|ENST00000444493||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||3433||531|C1orf167|protein_coding|CODING|ENST00000449278||1|WARNING_TRANSCRIPT_NO_START_CODON),downstream_gene_variant(MODIFIER||693|||RP11-56N19.5|antisense|NON_CODING|ENST00000376620||1),intron_variant(MODIFIER|||c.314-80TG>CA|827|C1orf167|protein_coding|CODING|ENST00000312793|2|1|WARNING_TRANSCRIPT_NO_START_CODON),intron_variant(MODIFIER|||c.2237-80TG>CA|1473|C1orf167|protein_coding|CODING|ENST00000433342|9|1),intron_variant(MODIFIER|||n.1210-80TG>CA||C1orf167|processed_transcript|CODING|ENST00000484153|7|1)       GT:DP:FT:MR:NNS:OABS:INF        1/1:22:.:22:22:CA14[]8[];C_1[]0[];_A1[]0[]:.;CONF=HIGH  1/1:19:.:19:16:CA18[]1[]:.;.    1/1:22:.:22:22:CA14[]8[];C_1[]0[];_A1[]0[]:.;CONF=HIGH  1/1:19:.:19:16:CA18[]1[]:.;.
          */
        String[] array = {
                "chr1","11836441","rs71492357","TG","CA",".",".","IN=1,2;DB;EFF=upstream_gene_variant(MODIFIER||2547||615|C1orf167|protein_coding|CODING|ENST00000444493||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||3433||531|C1orf167|protein_coding|CODING|ENST00000449278||1|WARNING_TRANSCRIPT_NO_START_CODON),downstream_gene_variant(MODIFIER||693|||RP11-56N19.5|antisense|NON_CODING|ENST00000376620||1),intron_variant(MODIFIER|||c.314-80TG>CA|827|C1orf167|protein_coding|CODING|ENST00000312793|2|1|WARNING_TRANSCRIPT_NO_START_CODON),intron_variant(MODIFIER|||c.2237-80TG>CA|1473|C1orf167|protein_coding|CODING|ENST00000433342|9|1),intron_variant(MODIFIER|||n.1210-80TG>CA||C1orf167|processed_transcript|CODING|ENST00000484153|7|1)"
                ,"GT:DP:FT:MR:NNS:OABS:INF"
                ,"1/1:22:.:22:22:CA14[]8[];C_1[]0[];_A1[]0[]:.;CONF=HIGH"
                ,"1/1:19:.:19:16:CA18[]1[]:.;."
                ,"1/1:22:.:22:22:CA14[]8[];C_1[]0[];_A1[]0[]:.;CONF=HIGH"
                ,"1/1:19:.:19:16:CA18[]1[]:.;."         
        };
        
        final VcfRecord vcf = new VcfRecord(array);
        final Vcf2maf mode = new Vcf2maf(2, 1, "TEST", "CONTROL", ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);
        SnpEffMafRecord maf = mode.converter(vcf);

        assertFalse(maf == null);        
        assertEquals("TG", maf.getColumnValue(MafElement.Reference_Allele));    
        assertEquals("CA", maf.getColumnValue(MafElement.Tumor_Seq_Allele1));
        assertEquals("CA", maf.getColumnValue(MafElement.Tumor_Seq_Allele2));
        assertEquals("CA", maf.getColumnValue(MafElement.Match_Norm_Seq_Allele1));
        assertEquals("CA", maf.getColumnValue(MafElement.Match_Norm_Seq_Allele2));
        assertTrue(maf.getColumnValue(14).equals("rs71492357"));     
         assertEquals("CA14[]8[];C_1[]0[];_A1[]0[]", maf.getColumnValue(MafElement.ND));            //ND
         assertEquals("CA18[]1[]", maf.getColumnValue(MafElement.TD));
         assertTrue(maf.getColumnValue(33).equals("TEST"));   //tumour sample
         assertTrue(maf.getColumnValue(34).equals("CONTROL"));   //normal sample
         
         assertEquals("22:16", maf.getColumnValue(MafElement.Novel_Starts));                 
         assertTrue(maf.getColumnValue(MafElement.T_Depth).equals("19"));
         assertTrue(maf.getColumnValue(MafElement.T_Ref_Count).equals("0"));
         assertTrue(maf.getColumnValue(MafElement.T_Alt_Count).equals("19"));    
         assertTrue(maf.getColumnValue(MafElement.N_Depth).equals("22"));
         assertTrue(maf.getColumnValue(MafElement.N_Ref_Count).equals("0"));
         assertTrue(maf.getColumnValue(MafElement.N_Alt_Count).equals("22"));
         
         //we get consequence with high rank, then long length, should be second annotation
         assertEquals("null", maf.getColumnValue(MafElement.Amino_Acid_Change));
         assertEquals("ENST00000433342", maf.getColumnValue(MafElement.Transcript_ID));
         assertEquals("c.2237-80TG>CA", maf.getColumnValue(MafElement.CDS_Change));
     }
    
     //do it tomorrow
     @Test
     public void confidenceTest() {
         
         final Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
         final String[] parms = {"chr1","16534646","rs221058","C","G",".",".","FLANK=CTGGCGAGGCT;BaseQRankSum=0.337;ClippingRankSum=-0.625;DP=17;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=-0.818;QD=15.22;ReadPosRankSum=-1.491;SOR=0.836;IN=1,2;DB;VLD;VAF=0.2062;EFF=missense_variant(MODERATE|MISSENSE|Ggc/Cgc|p.Gly163Arg/c.487G>C|802|ARHGEF19|protein_coding|CODING|ENST00000270747|3|1),upstream_gene_variant(MODIFIER||1625||167|ARHGEF19|protein_coding|CODING|ENST00000441785||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1625|||ARHGEF19|processed_transcript|CODING|ENST00000478210||1),upstream_gene_variant(MODIFIER||1608||305|ARHGEF19|protein_coding|CODING|ENST00000449495||1|WARNING_TRANSCRIPT_INCOMPLETE),upstream_gene_variant(MODIFIER||1169|||ARHGEF19|processed_transcript|CODING|ENST00000471928||1),upstream_gene_variant(MODIFIER||1140|||ARHGEF19|processed_transcript|CODING|ENST00000478117||1)"
                 ,"GT:DP:FT:MR:NNS:OABS:INF:AD:GQ","0/1:17:.:9:9:C2[33]6[31.33];G2[33]7[37.29]:PASS:.:.","0/0:14:.:.:.:C3[35]9[35.44];G0[0]2[40]:PASS.:.:.","0/1:17:.:9:9:C2[33]6[31.33];G2[33]7[37.29]:PASS:8,9:99",".:.:SAT3:.:.:C3[35]9[35.44];G0[0]2[40]:.;.:.:."};
         
         final VcfRecord vcf = new VcfRecord(parms);
         final SnpEffMafRecord maf = v2m.converter(vcf);
         assertEquals("FAIL", maf.getColumnValue(MafElement.Confidence));
     }
     
     @Test
     public void filterField() {
    	 Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
    	 String[] parms = {"chr1","16534646","rs221058","C","G",".",".","FLANK=CTGGCGAGGCT;BaseQRankSum=0.337;ClippingRankSum=-0.625;DP=17;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=-0.818;QD=15.22;ReadPosRankSum=-1.491;SOR=0.836;IN=1,2;DB;VLD;VAF=0.2062;EFF=missense_variant(MODERATE|MISSENSE|Ggc/Cgc|p.Gly163Arg/c.487G>C|802|ARHGEF19|protein_coding|CODING|ENST00000270747|3|1),upstream_gene_variant(MODIFIER||1625||167|ARHGEF19|protein_coding|CODING|ENST00000441785||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1625|||ARHGEF19|processed_transcript|CODING|ENST00000478210||1),upstream_gene_variant(MODIFIER||1608||305|ARHGEF19|protein_coding|CODING|ENST00000449495||1|WARNING_TRANSCRIPT_INCOMPLETE),upstream_gene_variant(MODIFIER||1169|||ARHGEF19|processed_transcript|CODING|ENST00000471928||1),upstream_gene_variant(MODIFIER||1140|||ARHGEF19|processed_transcript|CODING|ENST00000478117||1)"
    			 ,"GT:DP:FT:MR:NNS:OABS:INF:AD:GQ","0/1:17:.:9:9:C2[33]6[31.33];G2[33]7[37.29]:PASS:.:.","0/0:14:.:.:.:C3[35]9[35.44];G0[0]2[40]:PASS.:.:.","0/1:17:.:9:9:C2[33]6[31.33];G2[33]7[37.29]:PASS:8,9:99",".:.:SAT3:.:.:C3[35]9[35.44];G0[0]2[40]:.;.:.:."};
    	 
    	 VcfRecord vcf = new VcfRecord(parms);
    	 SnpEffMafRecord maf = v2m.converter(vcf);
    	 assertEquals(".,.,.,SAT3", maf.getColumnValue(MafElement.QFlag));
    	 
    	 String[] params = {"chr1","16534646","rs221058","C","G",".","PASS","FLANK=CTGGCGAGGCT;BaseQRankSum=0.337;ClippingRankSum=-0.625;DP=17;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=-0.818;QD=15.22;ReadPosRankSum=-1.491;SOR=0.836;IN=1,2;DB;VLD;VAF=0.2062;EFF=missense_variant(MODERATE|MISSENSE|Ggc/Cgc|p.Gly163Arg/c.487G>C|802|ARHGEF19|protein_coding|CODING|ENST00000270747|3|1),upstream_gene_variant(MODIFIER||1625||167|ARHGEF19|protein_coding|CODING|ENST00000441785||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1625|||ARHGEF19|processed_transcript|CODING|ENST00000478210||1),upstream_gene_variant(MODIFIER||1608||305|ARHGEF19|protein_coding|CODING|ENST00000449495||1|WARNING_TRANSCRIPT_INCOMPLETE),upstream_gene_variant(MODIFIER||1169|||ARHGEF19|processed_transcript|CODING|ENST00000471928||1),upstream_gene_variant(MODIFIER||1140|||ARHGEF19|processed_transcript|CODING|ENST00000478117||1)"
    			 ,"GT:DP:FT:MR:NNS:OABS:INF:AD:GQ","0/1:17:.:9:9:C2[33]6[31.33];G2[33]7[37.29]:PASS:.:.","0/0:14:.:.:.:C3[35]9[35.44];G0[0]2[40]:PASS.:.:.","0/1:17:.:9:9:C2[33]6[31.33];G2[33]7[37.29]:PASS:8,9:99",".:.:SAT3:.:.:C3[35]9[35.44];G0[0]2[40]:.;.:.:."};
    	 
    	 vcf = new VcfRecord(params);
    	 maf = v2m.converter(vcf);
    	 assertEquals("PASS", maf.getColumnValue(MafElement.QFlag));
     }
     
     @Test
     public void confidenceRealLife() {
         
         Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
         VcfRecord rec = new VcfRecord( new String[] {"chr1","13302","rs180734498","C","T",".",".","FLANK=GGACATGCTGT;IN=1,2;DB;VAF=0.1143",
                    "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                    "0/1:.:Germline:23:34:PASS:.:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                    "0/1:.:Germline:23:80:PASS:.:.:9:8:C35[40.11]36[39.19];T8[38.88]1[42]",
                    "0/1:26,10:Germline:22:36:PASS:99:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                    "0/0:.:LOH:22:80:PASS:.:.:.:.:C35[40.11]36[39.19];T8[38.88]1[42]"});
         
         SnpEffMafRecord maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
         
         rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                    "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                    "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                    "0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                    "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
            "0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
         
         maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
     }
     
     @Test
     public void somaticAndGermlinePASSBecomesGermline() {
         
         Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
         VcfRecord rec = new VcfRecord( new String[] {"chr1","13302","rs180734498","C","T",".",".","FLANK=GGACATGCTGT;IN=1,2;DB;VAF=0.1143",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
                 "0/1:24,10:Germline:23:34:PASS:.:.:9:C13[39.69]11[39.73];T9[37]1[42]",
                 "0/1:71,9:Germline:23:80:PASS:.:SOMATIC:8:C35[40.11]36[39.19];T8[38.88]1[42]",
                 "0/1:26,10:Germline:22:36:PASS:99:.:.:.",
         "0/0:.:LOH:22:80:PASS:.:.:.:."});
         
         SnpEffMafRecord maf = v2m.converter(rec);
         assertEquals("GERM", maf.getColumnValue(MafElement.Mutation_Status));
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
     }
     
     @Test
     public void ifGermlineOnlyLookAtControl() {
         
         Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
         VcfRecord rec = new VcfRecord( new String[] {"chr1","13302","rs180734498","C","T",".",".","FLANK=GGACATGCTGT;IN=1,2",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                 "0/1:.:Germline:23:34:PASS:.:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                 "0/1:.:Germline:23:80:PASS:.:.:9:8:C35[40.11]36[39.19];T8[38.88]1[42]",
                 "0/1:26,10:Germline:22:36:PASS:99:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
                  "0/1:.:Germline:22:80:PASS:.:.:.:.:C35[40.11]36[39.19];T8[38.88]1[42]"});
         
         SnpEffMafRecord maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
         
         rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                 "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                 "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                  "0/1:.:Germline:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
         
         maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
         rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                 "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                 "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                  "0/1:.:Germline:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
         
         maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
         rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                 "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                 "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
         
         maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
         
         rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                 "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                 "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "./.:.:.:.:.:.:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
         
         maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
         
         rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
                 "GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
                 "0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
                 "0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
                 "0/1:.:Germline:22:5:.:.:.:.:.:A1[39.5]1[42];G1[38.6]2[37.33]"});
         
         maf = v2m.converter(rec);
         assertEquals("PASS", maf.getColumnValue(MafElement.Confidence));
     }
     
     @Test
     public void isConsequence() {
         SnpEffMafRecord maf = new SnpEffMafRecord();         
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
         maf.setColumnValue(MafElement.Transcript_BioType, "protein_coding");
         assertEquals(true, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
         assertEquals(true, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
         
         maf.setColumnValue(MafElement.Transcript_BioType, "");
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
         
         maf.setColumnValue(MafElement.Transcript_BioType, "processed_transcript");
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
         
         maf.setColumnValue(MafElement.Transcript_BioType, null);
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
         assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
         
     }
     
     @Test
     public void flankNoteTest(){
        VcfRecord vcf = new VcfRecord.Builder("chrY",22012840,"C").allele("A").build();
        final Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
        
        //get flank first
        vcf.setInfo("HOM=28,CTTTTCTTTCaTTTTTTTTTT;FLANK=CTTTCATTTTT");                
        SnpEffMafRecord maf = v2m.converter(vcf);
        assertEquals("CTTTCATTTTT", maf.getColumnValue(MafElement.Var_Plus_Flank));
        assertEquals("HOM=28", maf.getColumnValue(MafElement.Notes));
        
        //get flank from reference
        vcf.setInfo("HOM=28,CTTTTCTTTCaTTTTTTTTTT;TRF=10_6,20_3");                
        maf = v2m.converter(vcf);
        assertEquals("CTTTTCTTTCaTTTTTTTTTT", maf.getColumnValue(MafElement.Var_Plus_Flank));
        assertEquals("TRF=10_6,20_3;HOM=28", maf.getColumnValue(MafElement.Notes));
        
     }
     
     @Test
     public void getFilterDetails() {
    	 assertEquals(".", Vcf2maf.getFilterDetails(null));
    	 assertEquals(".", Vcf2maf.getFilterDetails(new String[]{}));
    	 assertEquals(".", Vcf2maf.getFilterDetails(new String[]{"."}));
    	 assertEquals(".,.", Vcf2maf.getFilterDetails(new String[]{".","."}));
    	 assertEquals(".,.,.,.", Vcf2maf.getFilterDetails(new String[]{".",".",".","."}));
    	 assertEquals(".,PASS", Vcf2maf.getFilterDetails(new String[]{".","PASS"}));
    	 assertEquals(".,PASS,.", Vcf2maf.getFilterDetails(new String[]{".","PASS","."}));
    	 assertEquals("h,e,l,l,o", Vcf2maf.getFilterDetails(new String[]{"h","e","l","l","o"}));
     }
     
     @Test 
     public void converterTest() {
         
             final SnpEffMafRecord snpEffREc = new SnpEffMafRecord();            
            final Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
            final String[] parms = {"chr1","19595137","rs2235795","C","T",".",".","FLANK=ATACGTGGCCT;DP=9;FS=0.000;MQ=60.00;MQ0=0;QD=28.75;SOR=1.402;IN=1,2;DB;VLD;VAF=0.6584;EFF=missense_variant(MODERATE|MISSENSE|Gcg/Acg|p.Ala77Thr/c.229G>A|153|AKR7L|protein_coding|CODING|ENST00000420396|4|1),downstream_gene_variant(MODIFIER||1890|||AKR7L|retained_intron|CODING|ENST00000493176||1),non_coding_exon_variant(MODIFIER|||n.431G>A||AKR7L|polymorphic_pseudogene|CODING|ENST00000457194|4|1),non_coding_exon_variant(MODIFIER|||n.763G>A||AKR7L|polymorphic_pseudogene|CODING|ENST00000429712|6|1)"
                    ,"GT:DP:FT:MR:NNS:OABS:INF:AD:GQ"
                    ,"1/1:9:COVN12:9:9:T6[36.67]3[37.67]:.:.:."
                    ,"1/1:10:PASS:10:10:T8[36.12]2[30]:.:.:."
                    ,"1/1:9:COVN12:9:9:T6[36.67]3[37.67]:.:0,9:27"
                    ,"1/1:10:PASS:10:10:T8[36.12]2[30]:.:0,10:30"};
            
             final VcfRecord vcf = new VcfRecord(parms);
             final SnpEffMafRecord maf = v2m.converter(vcf);
                      
             assertEquals("FAIL", maf.getColumnValue(MafElement.Confidence));
             assertEquals(false, Vcf2maf.isHighConfidence(maf));
             
             assertEquals("MODERATE", maf.getColumnValue(MafElement.Eff_Impact));
             assertEquals("p.Ala77Thr", maf.getColumnValue(MafElement.Amino_Acid_Change));
             assertEquals("c.229G>A", maf.getColumnValue(MafElement.CDS_Change));
             assertEquals("Gcg/Acg", maf.getColumnValue(MafElement.Codon_Change));
             assertEquals("AKR7L", maf.getColumnValue(MafElement.Hugo_Symbol));
             assertEquals("protein_coding", maf.getColumnValue(55));
             assertEquals("CODING", maf.getColumnValue(56));
             assertEquals("ENST00000420396", maf.getColumnValue(51));
             assertEquals("4", maf.getColumnValue(MafElement.Exon_Intron_Rank));
             assertEquals("1",maf.getColumnValue(58));
             String ontology = "missense_variant";
             assertEquals(ontology, maf.getColumnValue(59));
             assertEquals(SnpEffConsequence.getClassicName(ontology), maf.getColumnValue(60));             
             assertEquals(SnpEffConsequence.getConsequenceRank(ontology)+"", maf.getColumnValue(40));             
             assertEquals(SnpEffConsequence.getMafClassification(ontology) , maf.getColumnValue(9));
                          
             //for other columns after A.M confirmation
             assertEquals(snpEffREc.getColumnValue(2), maf.getColumnValue(2));        
             assertEquals(SnpEffMafRecord.center , maf.getColumnValue(3));        
             assertEquals(snpEffREc.getColumnValue(4) , maf.getColumnValue(4));        
             assertEquals("1", maf.getColumnValue(5));        
             assertEquals(parms[1], maf.getColumnValue(6));        
             assertEquals(parms[1] , maf.getColumnValue(7));        
             assertEquals(snpEffREc.getColumnValue(8) , maf.getColumnValue(8));        
             assertEquals(IndelUtils.SVTYPE.SNP.name(), maf.getColumnValue(10));    
             assertEquals(parms[3] , maf.getColumnValue(11));        
             
             //check format field
             assertEquals("T", maf.getColumnValue(12));            
             assertEquals("T", maf.getColumnValue(13));             
             assertEquals("rs2235795", maf.getColumnValue(MafElement.DbSNP_RS));        
             assertEquals("VLD", maf.getColumnValue(15));    //dbSNP validation
                          
             assertEquals("null", maf.getColumnValue(MafElement.Tumor_Sample_Barcode));    //tumour sample    
             assertEquals("null", maf.getColumnValue(MafElement.Matched_Norm_Sample_Barcode));    //normal sample
             
             assertEquals("T", maf.getColumnValue(MafElement.Match_Norm_Seq_Allele1));    
             assertEquals("T", maf.getColumnValue(MafElement.Match_Norm_Seq_Allele2));        
             assertEquals("COVN12,PASS,COVN12,PASS", maf.getColumnValue(MafElement.QFlag));        //QFlag is filter column    
//             assertEquals(parms[6], maf.getColumnValue(MafElement.QFlag));        //QFlag is filter column    
             assertEquals("T6[36.67]3[37.67]", maf.getColumnValue(MafElement.ND));
             assertEquals("T8[36.12]2[30]", maf.getColumnValue(MafElement.TD));        
             assertEquals("9:10", maf.getColumnValue(41)); //NNS unkown
             assertEquals("ATACGTGGCCT", maf.getColumnValue(42)); //Var_Plus_Flank     
             assertEquals("0.6584", maf.getColumnValue(43)); //Var_Plus_Flank     
             assertEquals(snpEffREc.getColumnValue(44), maf.getColumnValue(44)); //Germ=0,185 
             
             //"chrY","22012840",".","C","A",
             //"GT:GD:AC","0/0:T/C:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]","0/0:A/C:C8[7.62],2[2],A2[8],28[31.18]"};
             assertEquals("10", maf.getColumnValue(MafElement.T_Depth));  // t_deep column2
             assertEquals("0", maf.getColumnValue(MafElement.T_Ref_Count));  // t_ref C8[7.62],2[2]
             assertEquals("10", maf.getColumnValue(MafElement.T_Alt_Count));  // t_allel A2[8],28[31.18]
             
             assertEquals("9", maf.getColumnValue(MafElement.N_Depth));  // n_deep column1
             assertEquals("0", maf.getColumnValue(MafElement.N_Ref_Count));
             assertEquals("9",maf.getColumnValue(MafElement.N_Alt_Count));   // A1[5],0[0]
             
             assertEquals("1,2", maf.getColumnValue(MafElement.Input));   // IN=1,2
             
             //other column
             assertEquals(VcfHeaderUtils.INFO_GERMLINE, maf.getColumnValue(26));  //somatic
     }
     
     
     @Ignore
     public void converterMergedRecHC() {
         
         final SnpEffMafRecord Dmaf = new SnpEffMafRecord();            
         final Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1            
         final String[] parms = {"chr1","11938210",".","GC","AA",".",".","IN=1,2;EFF=non_coding_exon_variant(MODIFIER|||n.384GC>AA||RP5-934G17.6|processed_pseudogene|NON_CODING|ENST00000438808|1|1)","GT:DP:FT:MR:NNS:OABS:INF","1/1:17:.:17:16:AA9[]8[];_A2[]1[]:.;CONF=HIGH","1/1:12:.:12:12:AA9[]3[]:.;.","1/1:17:.:17:16:AA9[]8[];_A2[]1[]:.;CONF=HIGH","1/1:12:.:12:12:AA9[]3[]:.;."};
         
         final VcfRecord vcf = new VcfRecord(parms);
         final SnpEffMafRecord maf = v2m.converter(vcf);
         
//         assertTrue(maf.getColumnValue(MafElement.Confidence).equals("HIGH,HIGH" ));
         assertEquals(true, Vcf2maf.isHighConfidence(maf));
         
         assertEquals("MODIFIER", maf.getColumnValue(MafElement.Eff_Impact));
         String ontology = "upstream_gene_variant";
         assertTrue(maf.getColumnValue(59).equals(ontology ));
         assertTrue(maf.getColumnValue(60).equals( SnpEffConsequence.getClassicName(ontology) ));             
         assertTrue(maf.getColumnValue(40).equals(SnpEffConsequence.getConsequenceRank(ontology)+""));             
         assertTrue(maf.getColumnValue(9).equals(SnpEffConsequence.getMafClassification(ontology) ));
         
         //for other columns after A.M confirmation
         assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));        
         assertTrue(maf.getColumnValue(3).equals(SnpEffMafRecord.center));        
         assertTrue(maf.getColumnValue(4).equals(Dmaf.getColumnValue(4) ));        
         assertEquals("1", maf.getColumnValue(MafElement.Chromosome));
         assertTrue(maf.getColumnValue(6).equals(parms[1] ));        
         assertTrue(maf.getColumnValue(7).equals(parms[1] ));        
         assertTrue(maf.getColumnValue(8).equals(Dmaf.getColumnValue(8) ));        
         assertTrue(maf.getColumnValue(10).equals(IndelUtils.SVTYPE.SNP.name()));    
         assertTrue(maf.getColumnValue(11).equals(parms[3] ));        
         
         //check format field    
         assertTrue(maf.getColumnValue(12).equals("A" ));            
         assertTrue(maf.getColumnValue(13).equals("C" ));             
         assertTrue(maf.getColumnValue(14).equals(SnpEffMafRecord.novel ));    //dbsnp        
         assertEquals(SnpEffMafRecord.Null, maf.getColumnValue(MafElement.DbSNP_Val_Status ));    //dbSNP validation
         
         assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Null ));    //tumour sample    
         assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Null ));    //normal sample
         
         assertTrue(maf.getColumnValue(18).equals("A" ));        //normal allel1    
         assertTrue(maf.getColumnValue(19).equals("C" ));         //normal allel2        
         assertTrue(maf.getColumnValue(35).equals(parms[6] ));        //QFlag is filter column    
         assertEquals("A5[41],13[39.31],C1[42],4[40.75]", maf.getColumnValue(MafElement.ND));    //ND
         assertTrue(maf.getColumnValue(MafElement.TD).equals("A20[41],23[39.78],C2[42],15[40.67]" ));        
         
         assertEquals("1,2", maf.getColumnValue(MafElement.Input));   // IN=1,2
     }
     
     @Test     
     public void cdsChange() {
         
         String[] str = {"chr1","240975611","rs7537530","C","G",".","PASS_1;PASS_2","FLANK=GATAGGCACTA;AC=2;AF=1.00;AN=2;DP=15;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=28.59;SOR=4.047;IN=1,2;DB;VLD;VAF=0.6979;HOM=2,TATGAGATAGgCACTATTAAT;CONF=HIGH_1,HIGH_2;EFF=intron_variant(MODIFIER|||c.879-268G>C|451|RGS7|protein_coding|CODING|ENST00000331110|13|1),intron_variant(MODIFIER|||c.798-268G>C|424|RGS7|protein_coding|CODING|ENST00000348120|10|1),intron_variant(MODIFIER|||c.957-268G>C|469|RGS7|protein_coding|CODING|ENST00000366562|12|1),intron_variant(MODIFIER|||c.957-268G>C|477|RGS7|protein_coding|CODING|ENST00000366563|13|1) intron_variant(MODIFIER|||c.957-268G>C|469|RGS7|protein_coding|CODING|ENST00000366564|13|1),intron_variant(MODIFIER|||c.957-268G>C|487|RGS7|protein_coding|CODING|ENST00000366565|13|1),intron_variant(MODIFIER|||c.798-268G>C|424|RGS7|protein_coding|CODING|ENST00000401882|10|1),intron_variant(MODIFIER|||c.957-268G>C|495|RGS7|protein_coding|CODING|ENST00000407727|12|1),intron_variant(MODIFIER|||c.450-268G>C|326|RGS7|protein_coding|CODING|ENST00000440928|6|1|WARNING_TRANSCRIPT_NO_START_CODON),intron_variant(MODIFIER|||c.705-268G>C|393|RGS7|protein_coding|CODING|ENST00000446183|13|1)","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","1/1&1/1:G/G&G/G:G1[33],14[32.93]&G1[33],14[32.93]:15&15:14&14:0,15:15:45:628,45,0","1/1&1/1:G/G&G/G:G1[32],7[33.43]&G1[32],7[33.43]:8&8:6&6:0,8:8:27:374,27,0"};
         
         Vcf2maf v2m = new Vcf2maf(1,2, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    
         SnpEffMafRecord maf  = v2m.converter(new VcfRecord(str));    
         assertEquals("ENST00000407727", maf.getColumnValue(MafElement.Transcript_ID));
         assertEquals("c.957-268G>C", maf.getColumnValue(MafElement.CDS_Change));
         assertEquals("null", maf.getColumnValue(MafElement.Amino_Acid_Change));
         assertEquals("RGS7", maf.getColumnValue(MafElement.Hugo_Symbol));
         assertEquals("protein_coding", maf.getColumnValue(MafElement.Transcript_BioType));
         assertEquals("CODING", maf.getColumnValue(MafElement.Gene_Coding));
         assertEquals("12", maf.getColumnValue(MafElement.Exon_Intron_Rank));
         assertEquals("1", maf.getColumnValue(MafElement.Genotype_Number));
     }
     
     /**
      * 
      * @throws Exception missing one sample column
      */
     @Test    (expected=Exception.class)
     public void indexTest() throws Exception{
         String[] str = {"chr1","204429212","rs71495004","AT","TG",".","SAT3;5BP4","DB;CONF=ZERO;"
                 + "EFF=upstream_gene_variant(MODIFIER||3793|||PIK3C2B|processed_transcript|CODING|ENST00000496872||1),"
                 + "downstream_gene_variant(MODIFIER||4368||172|PIK3C2B|protein_coding|CODING|ENST00000367184||1|WARNING_TRANSCRIPT_INCOMPLETE),"
                 + "intron_variant(MODIFIER|||c.1503-143AT>CA|1634|PIK3C2B|protein_coding|CODING|ENST00000367187|8|1),"
                 + "intron_variant(MODIFIER|||c.1503-143AT>CA|1606|PIK3C2B|protein_coding|CODING|ENST00000424712|8|1);"
                 + "END=204429213","ACCS","TG,1,3,_T,0,1"};
         Vcf2maf v2m = new Vcf2maf(1,2, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    
         v2m.converter(new VcfRecord(str));
         
     }
          
     @Test
     public void consequenceTest() {
                  
         String[] str = {"chr7","140453136","rs121913227","AC","TT",".","PASS","SOMATIC;DB;CONF=HIGH;"
                 + "EFF=missense_variant(MODERATE||gtg/AAg|p.Val207Lys/c.619GT>AA|374|BRAF|protein_coding|CODING|ENST00000496384||1|WARNING_TRANSCRIPT_NO_START_CODON),"
                 + "missense_variant(MODERATE||gtg/AAg|p.Val600Lys/c.1798GT>AA|766|BRAF|protein_coding|CODING|ENST00000288602||1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|16|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|12|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|14|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|18|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|15|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|11|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|17|1),"
                 + "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|13|1),"
                 + "sequence_feature[beta_strand](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|15|1),"
                 + "3_prime_UTR_variant(MODIFIER||54958|n.*1248GT>AA||BRAF|nonsense_mediated_decay|CODING|ENST00000497784|16|1),"
                 + "non_coding_exon_variant(MODIFIER|||n.82GT>AA||BRAF|nonsense_mediated_decay|CODING|ENST00000479537|2|1);END=140453137    "
                 + "ACCS    AC,14,20,A_,1,0    AC,33,36,A_,1,0,TC,1,0,TT,8,10,_C,0,2"};
                 
         Vcf2maf v2m = new Vcf2maf(1,2, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    
         SnpEffMafRecord maf  = v2m.converter( new VcfRecord(str));
         assertTrue(maf.getColumnValue(51+1).equals("p.Val600Lys"));
          assertTrue(maf.getColumnValue(50+1).equals("ENST00000288602"));
          assertTrue(maf.getColumnValue(52+1).equals("c.1798GT>AA"));
         
     }
          
     @Ignore
     public void defaultValueTest() {
             final SnpEffMafRecord Dmaf = new SnpEffMafRecord();            
            final Vcf2maf v2m = new Vcf2maf(2,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    //test column2; normal column 1
            final String[] parms = {"chrY","22012840",".","CT","AT","."  ,  "."  ,  "."  ,  "."  ,  "." ,  "."};

             final VcfRecord vcf = new VcfRecord(parms);
             final SnpEffMafRecord maf = v2m.converter(vcf);
             
             for(int i = 1 ; i < 63; i++) {
                 int ii = i;
                 if( i >= 5 && i < 8) ii = 100; //ignore. do nothing
                                  
                 switch(ii){
                     case 100 : break;  
                     case 10: assertTrue(maf.getColumnValue(10).equals( IndelUtils.SVTYPE.DNP.name()) ); break; 
                     case 11: assertTrue(maf.getColumnValue(11).equals("CT"));  break; 
                     case 26: assertTrue(maf.getColumnValue(26).equals(VcfHeaderUtils.INFO_GERMLINE )); break; 
                     case 31: assertTrue(maf.getColumnValue(31).equals(Dmaf.getColumnValue(i) + ":" + Dmaf.getColumnValue(i))); break;
                     case 35: assertTrue(maf.getColumnValue(35).equals(Constants.MISSING_DATA_STRING )); break;
                     case 41: assertTrue(maf.getColumnValue(41).equals("AT:ND0:TD0" ));  break;
                     default : assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i) ));      
                 }
                 
             } 
             
     }
    
     @Test
     public void bamIdTest()throws IOException, Exception{
         File log = testFolder.newFile();
         File input = testFolder.newFile();
         File out = testFolder.newFile();
            String[] str = {                
                    VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",    
                    VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
                    VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",    
                    VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",                
                    VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",                
                    VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_bamID\tTEST_bamID",
                    "chr1\t7722099\trs6698334\tC\tT\t.\t.\tBaseQRankSum=-0.736;ClippingRankSum=0.736;DP=3;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.736;QD=14.92;ReadPosRankSum=0.736;SOR=0.223;IN=2;DB;VLD;VAF=0.06887;EFF=intron_variant(MODIFIER|||c.805+173C>T|1673|CAMTA1|protein_coding|CODING|ENST00000303635|8|1),intron_variant(MODIFIER|||c.805+173C>T|1659|CAMTA1|protein_coding|CODING|ENST00000439411|8|1)\tGT:AD:DP:GQ:FT:MR:NNS:OABS:INF\t.:.:.:.:.:.:.:.:CONF=ZERO\t.:.:.:.:.:.:.:.:.\t0/1:1,2:3:35:COVN8:2:2:C0[0]1[39];T1[35]1[37]:CONF=ZERO\t.:.:.:.:.:.:.:.:."};            
                createVcf(input, str); 
                try {
                    Vcf2mafTest.createVcf(input, str);
                    final String[] command = {"--mode", "vcf2maf",  "--log", log.getAbsolutePath(),  "-i", input.getAbsolutePath() , "-o" , out.getAbsolutePath()};
                    au.edu.qimr.qannotate.Main.main(command);
                } catch ( Exception e) {
                    e.printStackTrace(); 
                    fail(); 
                }                
                try(BufferedReader br = new BufferedReader(new FileReader(out));) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                            if(line.startsWith("#") || line.startsWith(MafElement.Hugo_Symbol.name())) continue; //skip vcf header
                            
                        SnpEffMafRecord maf =  Vcf2mafIndelTest.toMafRecord(line);        
                         assertTrue(maf.getColumnValue(16).equals("TEST_bamID"));
                         assertEquals("CONTROL_bamID", maf.getColumnValue(MafElement.Matched_Norm_Sample_Barcode));     
                         assertTrue(maf.getColumnValue(33).equals("TEST_sample"));
                         assertEquals("TEST_bamID:CONTROL_bamID", maf.getColumnValue(MafElement.BAM_File));
                    }    
                }
         
     }
     
     @Ignore
     public  void singleSampleTest() throws IOException, Exception{
          
        String[] str = {
            "GL000236.1","7127",".","T","C",".","MR;MIUN","SOMATIC;MR=4;NNS=4;FS=CCAGCCTATTT;EFF=non_coding_exon_variant(MODIFIER|||n.1313T>C||CU179654.1|processed_pseudogene|NON_CODING|ENST00000400789|1|1);CONF=ZERO","GT:GD:AC:MR:NNS","0/0:T/T:T9[37.11],18[38.33]:.:4","0/1:C/T:C1[12],3[41],T19[35.58],30[33.63]:.:5"};            
            
         final Vcf2maf v2m = new Vcf2maf(1,1, null, null, ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES);    
        SnpEffMafRecord maf  = v2m.converter(new VcfRecord(str));
        assertTrue( maf.getColumnValue(36).equals(maf.getColumnValue(37)) );
        assertTrue( maf.getColumnValue(16).equals(SnpEffMafRecord.Null));
        assertTrue( maf.getColumnValue(33).equals(maf.getColumnValue(34)) );
        //T\tC    GT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4            
        assertTrue(maf.getColumnValue(41).equals("C:ND4:TD4"));   //NNS field is not existed at format                 
        assertTrue(maf.getColumnValue(45).equals("27"));  //t_deep column2
        assertTrue(maf.getColumnValue(46).equals("27"));  //t_ref T9[37.11],18[38.33]
        assertTrue(maf.getColumnValue(47).equals("0"));  //t_allel  
        
        assertTrue(maf.getColumnValue(48).equals("27"));  //n_deep column1
        assertTrue(maf.getColumnValue(49).equals("27")); //T9[37.11],18[38.33]
        assertTrue(maf.getColumnValue(50).equals("0"));
        
        
        assertEquals(null, maf.getColumnValue(MafElement.ND));
        assertEquals(null, maf.getColumnValue(MafElement.N_Depth));
        assertEquals(null, maf.getColumnValue(MafElement.N_Ref_Count));
        assertEquals(null, maf.getColumnValue(MafElement.N_Alt_Count));
        assertEquals(null, maf.getColumnValue(MafElement.Match_Norm_Seq_Allele1));
        assertEquals(null, maf.getColumnValue(MafElement.Match_Norm_Seq_Allele2));
        assertEquals(null, maf.getColumnValue(MafElement.Novel_Starts));
        
        //check BAM_FILE columne
        assertTrue(maf.getColumnValue(MafElement.BAM_File).equals(MafElement.Tumor_Sample_Barcode.getDefaultValue() + ":" + MafElement.Matched_Norm_Sample_Barcode.getDefaultValue()));
        assertTrue(maf.getColumnValue(MafElement.Tumor_Sample_Barcode).equals(MafElement.Tumor_Sample_Barcode.getDefaultValue()));
        assertTrue(maf.getColumnValue(MafElement.Matched_Norm_Sample_Barcode).equals(MafElement.Matched_Norm_Sample_Barcode.getDefaultValue()));
       
     }     
     
    
    public static void createVcf(String[] str) throws IOException{
        createVcf(new File(inputName), str);
    }
    public static void createVcf(File outputFile, String[] str) throws IOException{
        try(PrintWriter out = new PrintWriter(new FileWriter(outputFile));) {
            out.println(Arrays.stream(str).collect(Collectors.joining(Constants.NL_STRING)));
        }          
    }
     

    @Test
    public void fileNameTest() throws IOException {
         File log = testFolder.newFile();
         File input = testFolder.newFile();
         File out = testFolder.newFolder();
        String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",            
                VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
                VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_UUID",
                VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_UUID",                
                VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
                VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_UUID\tTEST_UUID"    
        };
                
        try{
            createVcf(input, str);       
               try {                     
                final String[] args = {"--mode", "vcf2maf",  "--log", log.getAbsolutePath(),  "-i", input.getAbsolutePath() , "--outdir" , out.getAbsolutePath()};
                au.edu.qimr.qannotate.Main.main(args);
            } catch ( Exception e) {
                e.printStackTrace(); 
                fail(); 
            }                
            
            assertTrue(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL_sample.TEST_sample.maf").exists());
            //below empty files will be deleted at last stage
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.Consequence.maf").exists());
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.maf").exists());
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.maf").exists());        
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.Consequence.maf").exists());            
        }catch(Exception e){
                fail(e.getMessage()); 
        }
    }
    
    @Ignore
    public void areVcfFilesCreated() throws Exception {
        String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",            
                VcfHeaderUtils.STANDARD_DONOR_ID + "=ABCD_1234",
                VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_uuid",
                VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_uuid",                
                VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",                
                VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_uuid\tTEST_uuid"    ,
                "chr10\t87489317\trs386746181\tTG\tCC\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
                         + "EFF=start_lost(HIGH||atg/GGtg|p.Met1?|580|GRID1|protein_coding|CODING|ENST00000536331||1);"
                         + "LOF=(GRID1|ENSG00000182771|4|0.25);END=87489318\tACCS\tTG,29,36,_G,0,1\tCC,4,12,TG,15,12"
        };
        
            
        File vcf = testFolder.newFile();
        File output = testFolder.newFile();
        createVcf(vcf, str);
            
        String [] command = {"--mode", "vcf2maf", "--log" , output.getParent() + "/output.log",  "-i" , vcf.getAbsolutePath() , "-o" , output.getAbsolutePath()};            
        Options options = new Options(command);
        options.parseArgs(command);
        new Vcf2maf(options );
            
        String SHCC  = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.Consequence.maf") ;
        String SHC = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.maf") ;
        String GHCC  = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.Consequence.maf") ;
        String GHC = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.maf") ;
        String SHCCVcf  = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.Consequence.vcf") ;
        String SHCVcf = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.vcf") ;
        String GHCCVcf  = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.Consequence.vcf") ;
        String GHCVcf = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.vcf") ;
        
        
        assertEquals(true, output.exists());
        assertEquals(true, new File(output.getAbsolutePath().replaceAll("maf", ".vcf")).exists());
        assertEquals(true, new File(SHCC).exists());
        assertEquals(true, new File(SHC).exists());
        assertEquals(true, new File(GHCC).exists());
        assertEquals(true, new File(GHC).exists());
        assertEquals(true, new File(SHCCVcf).exists());
        assertEquals(true, new File(SHCVcf).exists());
        assertEquals(true, new File(GHCCVcf).exists());
        assertEquals(true, new File(GHCVcf).exists());
    }

    @Test
    public void fileNameWithNODonorTest() throws IOException{
         File log = testFolder.newFile();
         File input = testFolder.newFile();
         File out = testFolder.newFile();
        String[] str = {"##fileformat=VCFv4.0",            
                "##qControlSample=CONTROL",
                "##qTestSample=TEST",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST" };

        createVcf(input, str);
        
        try{            
            final String command = "--mode vcf2maf --log " + log.getAbsolutePath() + " -i " + input.getAbsolutePath() + " --outdir " + out.getAbsolutePath();            
            final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");            
            assertEquals(1, exec.getErrCode());            
            
        }catch(Exception e){
             fail(e.getMessage());
        }    
    }
        
    @Test
    public void fileNameWithNoSampleidTest() throws IOException{
         File log = testFolder.newFile();
         File input = testFolder.newFile();
         File out = testFolder.newFolder();
        String[] str = {"##fileformat=VCFv4.0",            
                VcfHeaderUtils.STANDARD_DONOR_ID +"=MELA_0264",
                VcfHeaderUtils.STANDARD_TEST_BAMID +"=TEST_uuid",                
                VcfHeaderUtils.STANDARD_CONTROL_BAMID +"=CONTROL_uuid",                
                VcfHeaderUtils.STANDARD_TEST_SAMPLE +"=TEST_sample",                
                VcfHeaderUtils.STANDARD_CONTROL_SAMPLE +"=CONTROL_sample",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_uuid\tTEST_uuid"};

        createVcf(input, str);
        
        try{
            final String command = "-mode vcf2maf   --log " + log.getAbsolutePath() + " -i " + input.getAbsolutePath() + " --outdir " + out.getAbsolutePath();    
            final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");            
            assertEquals(0, exec.getErrCode());    
        } catch (Exception e){
             fail(e.getMessage());
        }
        
    }

}

