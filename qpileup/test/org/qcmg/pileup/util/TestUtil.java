package org.qcmg.pileup.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.Options;

public class TestUtil {
	
	public static Options getValidOptions(TemporaryFolder testFolder, String mode, File reference, File hdf, File bam, String outputDir, String readRange, String mergeHdf) throws Exception {
		String iniFile =  setUpIniFile(testFolder, mode, reference.getAbsolutePath(), hdf.getAbsolutePath(), bam.getAbsolutePath(), outputDir, readRange, mergeHdf);
		String[] args = {"--ini", iniFile};
		Options options = new Options(args);
		return options;
	}
	

	public static Options getValidOptions(TemporaryFolder testFolder, String mode, String reference, String hdf, String bam, String outputDir, String readRange, String mergeHdf) throws Exception {
		String iniFile =  setUpIniFile(testFolder, mode, reference, hdf, bam, outputDir, readRange, mergeHdf);
		String[] args = {"--ini", iniFile, "--tmp" , testFolder.getRoot().toString()};
		Options options = new Options(args);
		return options;
	}
	
	public static String[] getArgs(TemporaryFolder testFolder, String mode, String reference, String hdf, String bam, String outputDir, String readRange, String mergeHdf) throws Exception {
		String iniFile =  setUpIniFile(testFolder, mode, reference, hdf, bam, outputDir, readRange, mergeHdf);
		String[] args = {"--ini", iniFile };
		return args;
	}

	public static String[] getViewArgs(TemporaryFolder testfolder, String hdf, String readRange, boolean getHeader) {
		if (getHeader) {
			String[] args = {"--view", "--hdf", hdf, "--range", readRange, "--H"};
			return args;
		} else {
			String[] args = {"--view", "--hdf", hdf, "--range", readRange};
			return args;
		}
	}
	
	public static String setUpIniFile(TemporaryFolder testFolder, String mode,
			String reference, String hdf, String bam, String outputDir, String readRange, String mergeHdf) throws IOException {
		File iniFile = testFolder.newFile("test.ini");
		
		if (iniFile.exists()) {
			iniFile.delete();
		}		
		
		BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));
		out.write("[general]\n");
		out.write("log="+testFolder.newFile("log").getAbsolutePath()+"\n");
		out.write("loglevel=INFO\n");
		out.write("hdf="+hdf+"\n");
		out.write("mode="+mode+"\n");
		out.write("bam_override=true\n");
		out.write("range="+readRange+"\n");
		out.write("output_dir="+outputDir+"\n");		
		out.write("thread_no=1\n");
		out.write("[add_remove]\n");		
		out.write("name="+bam+"\n");		
		out.write("[merge]\n");		
		out.write("input_hdf="+mergeHdf+"\n");
		out.write("input_hdf="+mergeHdf+"\n");
    	out.write("[view]\n");   			
    	out.write("[bootstrap]\n");    	
    	out.write("reference="+reference+"\n");
    	out.write("low_read_count="+20+"\n");
    	out.write("nonref_percent="+30+"\n");
    	if (mode.equals("metrics")) {
    		out.write("[metrics]\n");
			out.write("min_bases=3\n");
			out.write("bigwig_path=wiggle\n");
			out.write("chrom_sizes=grc37.chromosomes.txt\n");
    		out.write("[metrics/clip]\n");
    		out.write("position_value=5\n");
    		out.write("window_count=5\n");
    		out.write("[metrics/nonreference_base]\n");
    		out.write("position_value=1\n");
    		out.write("window_count=1\n");
    		out.write("[metrics/mapping_qual]\n");
    		out.write("position_value=5\n");
    		out.write("window_count=5\n");
    		out.write("[metrics/indel]\n");
    		out.write("position_value=5\n");
    		out.write("window_count=1\n");
    		out.write("[metrics/snp]\n");
    		out.write("window_count=1\n");
    		out.write("dbSNP="+testFolder.newFile("dbSNP.vcf")+ "\n");
    		out.write("germlineDB="+testFolder.newFile("germline.vcf")+ "\n");
    		out.write("snp_file_format=vcf\n");
    		out.write("snp_file="+testFolder.newFile("other.vcf")+ "\n");
    		out.write("snp_file_annotation=TEST\n");
    		out.write("nonref_percent=1\n");
    		out.write("nonref_count=1\n");
    		out.write("high_nonref_count=1\n");
    		out.write("[metrics/strand_bias]\n");
    		out.write("min_percent_diff=0\n");
    		out.write("min_nonreference_bases=1\n");
    	}
    	out.close();
		return iniFile.getAbsolutePath();
	}

	public static void createBam(String fileName){
		
        List<String> data = new ArrayList<String>();
        data.addAll(createSamHeader());
        data.addAll(createSamBody());

       BufferedWriter out;
       try {
          out = new BufferedWriter(new FileWriter(fileName));
          for (String line : data) {
                  out.write(line + "\n");
          }
          out.close();
       } catch (IOException e) {
           System.err.println("IOException caught whilst attempting to write to SAM test file: "
                                              + fileName + e);
       }
 }

  private static List<String> createSamHeader(){
      List<String> data = new ArrayList<String>();
      data.add("@HD	VN:1.0	SO:coordinate");
      data.add("@RG	ID:20110501134458953	SM:eBeads_20091110_CD	DS:rl=50");
      data.add("@RG	ID:JV5US	SM:eBeads_20091110_CD	DS:rl=50");
      data.add("@PG	ID:qbamfilter::Test	VN:0.2pre");
      data.add("@SQ	SN:chr2	LN:249250621");
      data.add("@SQ	SN:GL000194.1	LN:243199373");
      data.add("@CO	create by qcmg.qpileup.test");
      return data;
  }

  private static List<String> createSamBody(){
      List<String> data = new ArrayList<String>();      
     
      data.add("JV5US:2415:2293	0	chr2	11190680	84	121M1S25H	*	0	0	GTGGTGGCAGTGGCGGCCGTGGTGGCGGCAGTGGTGGCGTTGGTGATGTTGGCCCCGCTGGCATGACGCAGTTTCTTCTTCTCATCGCGGGCTTGGTTCTGATGTTTGTAGTGTAGCACAGA	@DD@DE@DDECC?CC=B=BCE=EE@BB=ABBBD6<;?CCE@E?CCEC;;6;;?BC/AA@C=DDCEEABACCCE<CC?CD?ABACC<CCDE2CD>C>C>EDDDD???7?BC??>BCCCCCC@?	XA:Z:map3-1	MD:Z:121	ZE:i:11190851	PG:Z:tmap	RG:Z:JV5US	NM:i:0	AS:i:121	XS:i:20" +
      		"	ZS:i:11190630	XT:i:34	FZ:B:S,100,0,99,0,0,93,0,200,106,0,0,193,0,0,0,0,103,8,195,1,7,92,0,105,4,96,95,0,2,0,210,110,3,0,2,186,0,0,182,84" +
      		",101,0,9,201,0,0,0,0,104,0,197,7,0,90,182,0,10,0,11,1,106,108,83,9,106,4,2,192,128,0,0,207,1,114,6,98,0,0,0,0,201,1,201,5,112,0,100,101,98,90,230,6,0,0,215,393,0,0,0,84,5,0,81,19,116,0,0,192,0,2,102,106,103,3,99,99,9,97,119,0,5,0,2,7,104,101,89,0,305,0,108,0,186,0,92,0,208,87,83,4,11,0,99,112,104,89,110,0,3,106,297,0,10,20,9,0,100,0,0,0,193,0,6,186,196,0,100" +
      		",0,101,1,5,107,98,0,0,6,106,0,96,0,276,6,83,0,94,0,0,97,14,2,101,0,123,7,8,115,107,109,11,89,3,94,0,1,98,0,89,89,11,0,104,120,126,94,12,91,11,1,11,0,216,0,112,7,11,107,100,15,97,0,3,89,0,303,0,0,85,0,0,0,114,7,3,106,24,4,88,93,5,98,10,92,28,0,179,96,104,0,0,91");
      data.add("889_275_916	83	GL000194.1	112840	0	11M2135N39M	=	69095	-45929	TGGAAACATAGCCACAGGGACATAGAACCAAGCCCCAGGGCTGCTCAGCT	@IIEI''<7@FIIII%%IIG%%III" + 
    		 "5GIIIIIIIIIIIIIBIIIIIIIII	ZC:i:10	MD:Z:50	RG:Z:20110501134458953	NH:i:2	CM:i:3	NM:i:0	SM:i:17	CQ:Z:BBAB?AAA@30@B=.<BB<.@A@>*,AA;%)?:?%>>7?25,,1'>42<@" +	
    				  "CS:Z:T32321223123002100032010102233012001111032331300201=");
      data.add("JV5US:1796:1738	0	chr2	25829140	3	1M2I18M107S15H	*	0	0	TGCTTGTTTATTACGTTTCCTGTTTGTCTACCCAATGACACTGTAACACACAAGAGATAAGTCGTTAATGAATTAAAAACAAAATGCTTTTTATTTCTTAGAA" +
      		"TACTTAGAAATGTTAATAAAAATAA	+./8269<.00*+**-0(0(**--%**0-*05)4(*-2**-***-(0*-**-+.-**1/(--0,,(0(-**%--1235)+-42(0,-----%*--%*-(**--0//303049/21305(**111%-09	XA:Z:map3-1	MD:Z:19	" +
      		"ZE:i:25829314	PG:Z:tmap	RG:Z:JV5US	NM:i:2	AS:i:10	XS:i:-2147483647	ZS:i:25829089	XT:i:1	FZ:B:S,121,1,104,0,0,101,0,115,51,0,0,51,5,0,100,1,201,0,94,0,293,0,0,100,249," +
      		"13,0,87,67,0,51,0,268,0,219,0,51,7,0,51,251,0,0,51,13,0,0,0,98,67,0,0,119,0,12,56,0,0,1,0,270,222,1,0,51,7,0,97,38,117,109,0,0,1,2,1,51,2,114,41,58,9,51,0,51,1,1,164,0,0,2,0,131," +
      		"110,4,142,0,127,51,0,0,229,0,77,0,3,4,2,79,51,4,75,124,5,0,174,0,3,52,0,93,0,0,0,98,47,139,4,175,163,5,0,108,0,10,54,6,6,5,0,159,6,4,0,221,3,4,515,0,51,7,407,69,61,7,0,77,0,6,6," +
      		"547,71,4,5,251,2,51,4,249,7,1,11,67,51,11,194,122,6,6,72,5,69,9,0,194,4,7,114,3,7,69,8,0,305,0,0,101,0,3,75,188,5,4,0,190,4,4,1,140,1,0,473,60,0,3,414,0,2,2,0,51,0,1,0,195,251," +
      		"51,0,0,222,4,0,68,107,0,0,98,0,124,0,0,0,14,49,0,49,0,0,41,30,0,0,49,0,0,0,49,48,0,49");
      data.add("JV5US:232:554	0	chr2	115252276	66	74M2D30M1D16M1D10M1S17H	*	0	0	TTGGCAAATCACACTTGTTTCCCACTAGCACCATAGGTACATCATCCGAGTCTTTTACTCGCTTAATCTGCTCCTAAAACGGGAATATATTATCA" +
      		"GAACATAAGAAAACAAGATTAGGCTGGTACAGTGGA	C=D=CCC=CCCCEEE@CCC?CE?CCCCCDDD?CBBB@DEDDDDE@@;@?DB@CCC8CCCDDDD@C@CFAAA22*.>=9,668+3177<,,',8:88:386983..22&,94==@5@83800(+;885;859	XA:Z:map" +
      		"2-1	MD:Z:74^CT0A29^A16^G10	XE:i:0	XF:i:1	PG:Z:tmap	RG:Z:JV5US	NM:i:5	AS:i:103	XS:i:18	XT:i:23	FZ:B:S,103,1,100,0,0,84,0,84,208,0,0,198,0,0,111,312,97,103,0,86,3,105," +
      		"1,105,0,6,11,0,97,0,1,5,199,0,0,106,291,1,305,0,0,6,1,2,105,6,109,0,92,10,0,90,0,7,96,0,3,6,4,0,93,94,0,206,2,87,9,0,106,83,0,207,102,0,0,1,98,9,96,108,94,105,0,101,97,180,121," +
      		"107,3,95,108,0,87,6,0,5,415,102,111,0,96,0,94,99,3,106,207,3,191,0,10,0,101,97,3,0,97,3,115,0,0,0,2,0,90,0,0,0,100,0,246,2,84,408,117,262,0,7,21,2,186,0,4,10,82,9,0,82,88,9,5,1" +
      		"05,157,0,3,82,8,0,30,0,89,10,95,0,24,98,17,120,2,0,24,16,173,0,86,69,95,11,0,183,12,6,106,449,4,14,4,39,103,189,104,0,19,102,22,0,195,121,0,216,0,79,103,249,24,5,0,0,95,0,0,109," +
      		"4,83,9,100,26,89,75,4,21,5,185,0,21,95,0,24,115,13,79,27,2,11,0,26,100,16,163,8,15,2,88,118,18,83,10,31,80,101,10,9,259,96,30,17,100,111,23,100");
      
      return data;

  }



  

}
