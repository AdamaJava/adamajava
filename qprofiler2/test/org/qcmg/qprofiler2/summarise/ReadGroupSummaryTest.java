package org.qcmg.qprofiler2.summarise;
 
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.bam.BamSummarizer;
import org.qcmg.qprofiler2.bam.BamSummaryReport;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.XmlElementUtils;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class ReadGroupSummaryTest {
	
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	static File input;
	static File longReadInput;

	@BeforeClass
	public static void setup() throws IOException {
		input = testFolder.newFile("testInputFile.sam");
		createInputFile (input);

		longReadInput = testFolder.newFile("testLongReadInputFile.sam");
		createLongReadInputFile(longReadInput);
	}	
	 
	public static void createInputFile(File input) throws IOException {
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@RG	ID:1959N	SM:eBeads_20091110_ND	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");

		// unmapped first of pair
		data.add("243_146_1	101	chr1	10075	0	*	=	10167	0	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
 
		// duplicated
		data.add("243_146_2	1171	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		data.add("243_146_4	1121	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
		
		// secondary = not primary
		data.add("243_146_2	353	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		// vendorcheck failed
		data.add("243_146_3	609	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
				
		// supplementary
		data.add("243_146_5	2147	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		// count overlap assume both same length so min_end is below read end
		// hard clip both forward but proper pair
		// f3f5: 10075 ------- -> 10111(firstofPair)   10200 --------- -> 
		data.add("243_146_5	67	chr1	10075	6	3H37M	=	10200	93	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		// overlap outward
		// forward ??? why -59, it should be reverse.mate_end -forward.read_start
		data.add("970_1290_1068	163	chr1	10176	3	22M50D10M8H	=	10167	26	" +
				"AACCTAACCCTAACCCTAACCCTAACCCTAAC	I&&HII%%IIII4CII=4?IIF0B((!!7F@+	RG:Z:1959T	" +
				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=");		
		// reverse
		data.add("970_1290_1068	83	chr1	10167	1	5H35M	=	10176	-26	" +
				"CCTAACNCTAACCTAACCCTAACCCTAACCCTAAC	.(01(\"!\"&####07=?$$246/##<>,($3HC3+	RG:Z:1959T	" +
				"CS:Z:T11032031032301032201032311322310320133320110020210	CQ:Z:#)+90$*(%:##').',$,4*.####$#*##&,%$+$,&&)##$#'#$$)");
		 			
		// below overlap  62  groupRG:Z:1959N. make sure overlap only calculate once.
		// 62 = min_end - max_start +1 = 10161 - 10075 + 1 = 62
		// trimmed, deletion forward   10075 (start~end) 10161
		data.add("243_145_5	99	chr1	10075	6	15M50N22M	=	10100	175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		// mate reverse   soft clip ???seqbase maybe wrong 10100 (start~end) 10249
		data.add("243_145_5	147	chr1	10100	6	25M100D20M5S	=	10075	-175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAACACCCTAACCCTAA	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		// noRG, unpaired read should be counted but not belong to Pair and no Tlen
		data.add("NS500239:99	16	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");
		
		try(BufferedWriter out = new BufferedWriter(new FileWriter(input))) {	    
			for (String line : data)  out.write(line + "\n");	               
		}		
	}

    public static void createLongReadInputFile(File input) throws IOException {
		   List<String> data = new ArrayList<String>();
		   data.add("@HD\tVN:1.6\tSO:coordinate");
		   data.add("@SQ\tSN:chr22\tLN:50818468");
		   data.add("@RG\tID:COLO829_BL.28464\tPL:ONT\tSM:COLO829_BL");

				   //Read length 301 - Supplementary alignment
						   data.add("12b013c5-a784-4448-9eca-140d6db89e1c\t2048\tchr22\t20164189\t1\t11M3D72M6D11M3I35M1I1M2I37M3D20M6I46M3D9M4I2M2I26M2D13M3713H\t*\t0\t0\tCCACCACCATCACCACCACCACCACCAGCAGCATCACCACCAATACCACCACCACCACCAGCATCACCATCAACACCACCATCACCACCACCATCACCACCAGCACCACCAGCACCACCACCATCACCACCAGCATCACCACCAACACCACCATCACCACCAGCATCAGCACCACCACCACCACCACCACCACTACCGTCACCACCACCACCACCACCACCACCATCACCACCACCACCACCAGCAGCATCACCACCAATACCACCACCACCACCAGCATCACCATCAACACCACCATCAC\t/6:<BEGGKJFOHKHIHOHLLKHGDCBEHGDJIF?>>7201FEGHIIHD=>CCCBCLKHSDDFHGKSJJQJPKJSMSMKNNIJSHFRGGIIFGGFILHJHHIGHFFCIHHHHHFFFPLKQMSMSLHQHHISODECDBIIKJPMIKOJJILKLJKGRJFOGHIGIIFLFHEMHLLIIGISSJJJILIGGGFHHHGHJH<;;;<CGHFNJKLINRSQMJJMKJPIKKIGHFFJGJSJHLSLJKJNJKMJILJHSKIPLFPIHHJNJIOOLKLHOLLHSCBBB?EFEHGIJJIMILSISSHOLG\ts1:i:40\ts2:i:0\tSA:Z:chr1,110120463,+,2357S1655M2I,60,48;chr21,43335930,+,202S1716M2I2094S,1,305;chr21,43335930,+,724S1700M16D1590S,60,299;\tMD:Z:11^ACT19C2C5T2C8T8C0A1C8C10^ATCAGA16T3T4C26T2C2T3T10C5C2T1^GTT4T5T5T17T5T3T13T5T1^GTT1C14T5T2C11^CC5T7\tRG:Z:COLO829_BL.28464\tML:B:C,3,16,0,2,5,18,6,28,3,4,4,1,1,12,5,8,12,2,11,3,18,3,33,17,1,17,2,1,2,5,2,1,3,4,5,36,5,2,38,9,10,125,14,15,8,13,1,14,5,9,8,10,252,239,255,253,250,237,248,227,252,3,2,254,254,243,251,247,243,253,244,252,237,252,222,238,3,238,1,254,4,250,253,254,252,11,7,43,7,5,30,16,21,52,241,240,19,17,254,241,250,15,17,20\tMM:Z:C+h?,108,444,3,1,3,5,3,11,3,5,32,10,167,1,14,3,10,31,37,244,11,17,7,20,19,17,78,15,44,13,9,16,0,96,35,46,22,15,25,4,1,11,2,0,7,9,47,1,32,38,34,5;C+m?,108,444,3,1,3,5,3,11,3,5,32,10,167,1,14,3,10,31,37,244,11,17,7,20,19,17,78,15,44,13,9,16,0,96,35,46,22,15,25,4,1,11,2,0,7,9,47,1,32,38,34,5;\tNM:i:67\tAS:i:260\tde:f:0.1463\trl:i:1308\tcm:i:3\tnn:i:0\ttp:A:P\tms:i:287\tqs:i:22");

				   //Read length 1258
						   data.add("e0ea8507-83a9-4e00-bb0c-b0a27f9ed0a4\t0\tchr22\t20178190\t60\t138M1I201M2D67M2D14M1D6M1I5M1D25M1D163M1I426M1D77M1I132M\t*\t0\t0\tGGGCACGTCTCTTCCCAGTGAACCTCAGATCCCTCGGGCGCCCACGCTCCCCACTGAGGGCCTGGGGCCACAGCACTCTGCCTGTGAGGGGTGGGCCCTTGGAGGCTTGGGAAGGCGCGCGCCCACAGGCACGTGGGGTAGAGCGCGCTGAGCGCTGTCTCAGTGATGGTTCCCCACACTGGCCCCTGACATGGGGCTTGGTGTGGCAGCTTGTTGGGGAGGTGGTGCGTGGAGCAGGATGGGGACCGGGAGGTGGTTGCTGTGGGCTCTGCGGGGCGTGAGGGCCGGTGTTTGCCCCAAGCTCAGTGCCTGCCTGTGACCACACCAGCTCATAAGCGACCGGGGTGGCCCAGGGGTGAGCGGAGAGGGGACCTGAGCAGGCAACACGGGCCTGGAGAGGAGCTTTGTCTCTAGGAGAAGGAGCCTGCTGCTGTACACCTGGTATCACGCCACCCCAAGCAAGCCCCGCAGAGCCCTCGGGAGCTCCACTCGGAGATGGAGACGCGCTTCCCTGTGTCGGGGCTTCGCTCAGAACATTTTTATGGGCTGTTAATTGTTATGCTTTCATTTAATTCTGGGTGTCATTCCATGTCAGCCAAGCCCGACAGGCTGGGATTTATGTCTTCCAAATTATGGGGCTCGGGCTGTTCAGAGGTAAAACAATTACCCATCAATCAGCCACCAACCGGCTTGTGCTGGGGCTGGCAGCCCCATTAGCGGGCGTGGGCAGCCATGGCCCAGCAAGACCCCTTGAGGGACAGGACTGCCTTCGAGGGCAAGGCAGTCTGGGTGAAGTCTCGCCCCAGGTTGGCCCTGTGCTGTGGCAGGGCGCAGCTCTCTGGGGAGGCGGCCGCTCAGGGAAGGACCCCAGGGTCCACATTCACACCCAACTGGCTCCAGTGCTGCAGCCCTCGGCCTCCCATATGATGGTGTGACCCTAGGTGGGCTGAGGGTGACCCGCCGGAAGACGCTGCAGCATCAAGGTGGGGGGACCATGGGTCCCATATGCTGGGGCCTGGGGAGCCTTGGCCAGCCATGGTCACTCAGTGGAGGGGCAGGGTCACCTGCCTCCTGAGCACCCCATCCCCAGGAGGGAGGAACCAGGTGTGACCCACTTCACAGGGAGGGGAAGCCTTGGCTGTCAGAGGGGACACTGCGTCTCTCACTGGGAGGACCTCCCGGCCTGGGCTTGAGGCCCACCTGGATCTGTGCAAACGTAGATGTGGGTAGGGTGGGGCAGGGGAGGGGAGGTGGGCAG\tFDDDEEFHHHJSHIKILJIGGGIHI5CJSFGC:ACFFGGIHHEEEFJOJEA6556AGHHQHNINLKMJNIMOKKNJMLMLLLK///-0E@FKMNMKQLIHLISIFG6()%%&&(--CHHJIKSPHJGHFJMHB2100..0)(&&()016@FHFMGHGFGDDFFFEECCEPPAA@MKLKNHNNKSKSLKSMINPQJSSOPMLSKJQLPLSIKMSQ>GJKSSQMKJHSLIABSNMQOJSJKIJLHFL>=CSHHKFEKMILSNLKLMILOSIOHD?>???HLKPJIHAABCBSJJSPFEDGKSPLOMSSMKRNMMSOLNJKHSLNMQLKHOJKC<-----.--/1BAHFQJLHDGOSF<<<CFFC;;<E>SKILB@AJSRKQMLSLISIKLQSSIKKMNLSKIKSOMMLGE*),8;99;<@AEA>=766662)))))3028-,,,-33365566--*('&%&()*..679EHSJSFLJGG?AA?JLIFA@?655<BSK<;=AHHFJHGLMHKMNSJNKKHPOSPMSKSPSKSOKLSKOISSSMLPLMMSRMQSOLSMOSIOSJSSOSDKSSSPSSRSO.:ISKSSMSOIOIHIOJHGGEHF3,,,,-D@>>210000,01*((()'(*1576699<9<HFFHGFKJNSQNNMOHPMOSOSKMNSKSOMSSMIPLLKSSNSSNOSNMKRGRJPEEBNHSNHFKMJROMSSRJOMIPKJILKPJMSPPKKFGDHJKKOPLSOKJNOMHKLSOLHA@?8/;??IKNJMHILSSBACCDMKOKKSKKHCKKLKOMKGGGIGKKKSKGJHSIMSNMMNJJEDEFEMKSSSGMKLLHHHFGGJ=BCGE6?NQGJLLNPOMGGGGFLFHFGFMNMPSQSLSLNSNMKONPJMOMMPIOFFGKQKI@<?:91****,<175538HGGSNKIKPKKNPHLOSONSSMMHIFSKH7<99899EGJMLMKSMMKQSSQOSNMMLSKOOMSSKLQNKRKQPRJKIQSLLQKHRJSGJEGKNIHONQS?>?>,2/02889998=??AE888888DALNLJJJ.,<=.=4A@JLLPLJLSNSJISOKLLKPOLPONOKDKSSLRKNNI=11()((*???JHHGOHNLSSNSNLLNSO:<<?.<88:>@GIMILN?>=>>>OOPMKNSSNNNSSPSKMKKHHGF9GLKSSKKSSSSSGOJPPNSQKSMJLSMLLSOLM77FAGJIFGFHFHHHHQIJHHIJ1@=;<===41235;0))***<ACCIIHFFJLGS9E\ts1:i:1139\ts2:i:0\tMD:Z:111G1C0A0T27C0T190T3^CT1T65^TC14^C7C3^C0C1G22^G3G585^G50G158\tRG:Z:COLO829_BL.28464\tML:B:C,2,5,1,1,1,2,10,12,1,6,6,1,4,15,29,194,5,2,9,0,3,3,0,16,1,6,7,2,15,8,12,3,6,1,2,13,22,6,2,0,77,49,67,28,9,3,7,254,254,254,253,13,243,254,14,14,254,9,36,31,60,7,5,20,2,252,251,254,19,254,13,248,240,31,16,27,11,249,252,253,235,220,12,253,255,145,39,56,33,13\tMM:Z:C+h?,26,11,5,69,2,0,16,14,1,7,12,11,15,2,11,7,10,14,3,2,0,5,3,2,2,20,10,11,0,11,4,3,12,9,28,2,0,6,2,0,0,28,0,2,3;C+m?,26,11,5,69,2,0,16,14,1,7,12,11,15,2,11,7,10,14,3,2,0,5,3,2,2,20,10,11,0,11,4,3,12,9,28,2,0,6,2,0,0,28,0,2,3;\tNM:i:25\tAS:i:2366\tde:f:0.0182\trl:i:0\tcm:i:204\tnn:i:0\ttp:A:P\tms:i:2368\tqs:i:20");

				   //Read length 212
						  data.add("ee9f0c60-5af7-495b-9c3b-d4b30971c3d2\t0\tchr22\t20173734\t60\t212M\t*\t0\t0\tCAAGGACTCCCCGCAGCCCAGTGTCTGTGGAGTGGGGCGTGAGGTGCTGCCTCCCATGTTGCTGCTTAGAAGGACGCAGCCCTGGAAACCCTCACTGTGGAGTCTCTGAGCCCCTCATCCGCAGAGCAGCAGTTGCTGCTGTTTGCACAGACAGCAAATCCGGGGCATGTTTGTTTAGTAAGATTACCTCTGCTGGGGCCTGGCCCAAGGGA\t9EISNSRSMHDEJGJJSLJSSNLSSPLPNMSJNNSSMKSMNSQSSJSNSSKSHSKJKNOSJSJSSMRMSSSLPNMSMLECCB?::>??B988;HKD@=4444:767;?IFHHEEFDQKISSMNPLSSPSKLLSSLSKSSSSKPLJMNSKKOQSJOKSLSJKIB;FDMOQISMSAA@AAJJSNNORPSMJMKNLKSSJHNMMOJJGHKPSLOJ\ts1:i:211\ts2:i:0\tMD:Z:212\tRG:Z:COLO829_BL.28464\tML:B:C,2,27,23,3,3,9,27,17,7,252\tMM:Z:C+h?,17,10,9,11,11;C+m?,17,10,9,11,11;\tNM:i:0\tAS:i:424\tde:f:0.0\trl:i:0\tcm:i:41\tnn:i:0\ttp:A:P\tms:i:424\tqs:i:21");

				  data.add("10786c49-0a30-44b8-8abd-4c75b693c1a4\t0\tchr22\t20166193\t60\t16S56M9D152M1D34M4I52M12D73M\t*\t0\t0\tTGGCACATATAGAAATACACACGGGGTGAGGACCCTGCAAGCACATGCAAAGGCAGGGGTCTCTCTTAGTATTGGGCTGAACCTGTCTCTCTTCCTGCCTCACTGGGATTGTGGATGACCATGGGGTTGGCTCAGGAGTCTGCCCTGTGCCTGCAGATGTTCTGGATATCTGGGCTAGTGTTGGGGGGTATTATCTGACCCATGGACCAGTCTCTGGTTCTTCTCTGCCCCAGCCACTCCAGGCCTTTGGTGAACCCCAGTTTTCTACCATTCAACTTGACCTGCCATGTTCCTGAGATCTAGCCCAGGGCTACTTCTCACTAAAGTGTGAGTGGTTGCTTGGCACTACCCTAATCTTTGCCGCCTCATGTGTTTATTGGTGTGAAG\t027<31('&&$$$$%$$$$%%()346>>==<>AABBABBC<????DDGFGHHDBBBBAA?>??@AD@<998988799=:9979:;A>??DBCB@@BCBCAAACGHDDDCDEIGCCCDDECEDCA====>DDEAA@@?AB@==<?AAACCBAAABDDCCCCEDDEDCBAACDE@@@?>????BA?????>>?ACDAA???AAA@ACDCBAABBDDDEECDCBDCC32222BBACEDDCDBBAB@?>AAFBBDA@>>9690)((((()+=>AABBABCBDCCCCABABBA>=;731.-,''((''(.//111-.0/)))).47:>AADCDCCCDCB???????>>@DBA?>==>>@BEHHIFC75555;CEDEFGGGHHGFFFFDE?;;\ts1:i:303\ts2:i:133\tMD:Z:23C32^TGGGGCAGG5A146^A0T33C51^TCCATCCACCAA73\tRG:Z:COLO829_BL.28464\tML:B:C,216,4,22,8\tMM:Z:C+h?,4,91;C+m?,4,91;\tNM:i:30\tAS:i:642\tde:f:0.0216\trl:i:0\tcm:i:45\tnn:i:0\ttp:A:P\tms:i:673\tqs:i:17");
		   try(BufferedWriter out = new BufferedWriter(new FileWriter(input))) {
					   for (String line : data)  out.write(line + "\n");
			   }
	}

	private ReadGroupSummary createLongReadRGElement(String rgid) throws IOException, ParserConfigurationException {

		ReadGroupSummary rgSumm = new ReadGroupSummary(rgid, true);
		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(longReadInput)){
			for (SAMRecord record : reader) {
				if (rgid == null)
					rgSumm.parseRecord(record);
				else if (rgid.equals(XmlUtils.UNKNOWN_READGROUP) && record.getReadGroup() == null)
					rgSumm.parseRecord(record);
				else if (record.getReadGroup() != null && record.getReadGroup().getId().equals(rgid))
					rgSumm.parseRecord(record);
			}
		}

		return rgSumm;
	}

	@Test
	public void rgLongReadTest() throws Exception {
		String rgid = "COLO829_BL.28464";  // here only test the pair from "1959N"
		ReadGroupSummary rgSumm = createLongReadRGElement(rgid);
		final Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);

		// must be after readSummary2Xml(root)
		//Total 3 - 1 is supplementary, so two counted
		assertTrue(rgSumm.getReadCount() == 3);

		// <sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("basesLost")).findFirst().get() ;
		checkBadReadStats(root1, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root1, "unmappedReads", 0, 0, "0.00");
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00");
		checkBadReadStats(root1, "trimmedBases", 0, 0, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {1, 16, 16, 16, 16, 16,16}, "0.42");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP  ,new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {0,0,0,0,0,0,0},"0.00");

		// <sequenceMetrics name="reads" readCount="2">
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("reads")).findFirst().get() ;
		assertTrue(root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("4"));
		checkDiscardReads(root1, 1,0,0);

		// check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MAX, "1258"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEAN, "619"));

		//check tlen
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.NODE_PAIR_TLEN)).findFirst().get() ;
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MAX, "0"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEAN, "0"));

		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("countedReads")).findFirst().get() ;
		assertTrue(checkChildValue(groupE, ReadGroupSummary.UNPAIRED_READ, "3"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.READ_COUNT, "3"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_COUNT, "16"));
		// here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_PERCENT, "0.42"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_COUNT, "3774"));

	}


	/**
	 * 
	 * @param ele:<sequenceMetrics name="reads"..>
	 */
	private void checktLen(Element parent, int pairCount, int max, int mean, int mode, int median) {
		Element ele = XmlElementUtils.getChildElementByTagName(parent, XmlUtils.VARIABLE_GROUP)
		.stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals("tLen")).findFirst().get() ;	
		assertTrue(checkChildValue(ele, ReadGroupSummary.MAX, max+"")); 	
		assertTrue(checkChildValue(ele, ReadGroupSummary.MEAN, mean +"")); 	
		assertTrue(checkChildValue(ele, ReadGroupSummary.MODE, mode+"")); 	
		assertTrue(checkChildValue(ele, ReadGroupSummary.MEDIAN, median +"")); 	
		assertTrue(checkChildValue(ele, ReadGroupSummary.PAIR_COUNT, pairCount+"")); 		
	}
	
	/**
	 * 
	 * @param parent: <sequenceMetrics Name="reads" count="9">
	 * @param counts: array of {totalReads, supplementaryReads, secondaryReads, failedReads}
	 */
	private void checkDiscardReads(Element parent, int supplementary, int secondary, int failedVendor) {		
		Element ele1 = XmlElementUtils.getChildElementByTagName(parent, XmlUtils.VARIABLE_GROUP)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("discardedReads")).findFirst().get() ;				
		assertTrue(checkChildValue(ele1,"supplementaryAlignmentCount", String.valueOf(supplementary)));
		assertTrue(checkChildValue(ele1,"secondaryAlignmentCount", String.valueOf(secondary)));
		assertTrue(checkChildValue(ele1,"failedVendorQualityCount", String.valueOf(failedVendor)));
	}
	
	/**
	 * 
	 * @param parent: eg. <sequenceMetrics Name="reads" count="9">
	 * @param name: variableGroup name
	 * @param counts: read number for duplicate, unmapped or non-canonical
	 * @param totalReads : total counted reads
	 * @return
	 */
	private Element checkBadReadStats(Element parent, String name, int reads, int base, String percent) {		    
		   Element groupE =  XmlElementUtils.getChildElementByTagName(parent, XmlUtils.VARIABLE_GROUP)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(name)).findFirst().get() ;		   
			assertTrue(checkChildValue(groupE,"readCount", String.valueOf(reads)));
			assertTrue(checkChildValue(groupE,"basesLostCount", String.valueOf(base)));			 		
			assertTrue(checkChildValue(groupE,"basesLostPercent",percent));
			return groupE;
	}
 
	/**
	 * 
	 * @param parent
	 * @param nodeName
	 * @param counts new int[] {reads, min, max, mean, mode, median, lostBase}
	 * @param percent: basePercent
	 */
	private void checkCountedReadStats(Element parent, String nodeName, int[] counts, String percent) {
		// check readCount		 
		Element groupE =  XmlElementUtils.getChildElementByTagName(parent, XmlUtils.VARIABLE_GROUP)
			.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(nodeName)).findFirst().get() ;		   
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MIN, String.valueOf(counts[1])));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MAX, String.valueOf(counts[2])));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEAN, String.valueOf(counts[3])));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MODE, String.valueOf(counts[4])));	
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEDIAN, String.valueOf(counts[5])));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.READ_COUNT, String.valueOf(counts[0])));		
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_COUNT, String.valueOf(counts[6])));		
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_PERCENT, percent));		
	}
			
	public static boolean checkChildValue(Element parent,String name, String value) {
		 List<Element> eles = XmlElementUtils.getChildElementByTagName(parent, XmlUtils.VALUE);	
		 Element ele = eles.stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(name)).findFirst().get() ;		 
		 return ele.getTextContent().equals(value);		
	}

	/**
	 * parse read with specified rgid; it will parse read without readgroupid if the input rgid=QprofilerXmlUtils.UNKNOWN_READGROUP
	 * it will parse every reads if the input rgid is null
	 * @param rgid : readgroup id, allow QprofilerXmlUtils.UNKNOWN_READGROUP and null
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private ReadGroupSummary createRGElement(String rgid) throws IOException, ParserConfigurationException {
		
		ReadGroupSummary rgSumm = new ReadGroupSummary(rgid, false);
		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(input);){
			for (SAMRecord record : reader) {	
				if (rgid == null)
					rgSumm.parseRecord(record);
				else if (rgid.equals(XmlUtils.UNKNOWN_READGROUP) && record.getReadGroup() == null)
					rgSumm.parseRecord(record);	
				else if (record.getReadGroup() != null && record.getReadGroup().getId().equals(rgid))
					rgSumm.parseRecord(record);						 	 
			}						
		}
		
		return rgSumm;
	}
	
	@Test
	public void rgSmallTest() throws Exception {		
		String rgid = "1959N";  // here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid);
		final Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		
						
		// must be after readSummary2Xml(root)
		assertTrue(rgSumm.getReadCount() == 2);
		
		// <sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("basesLost")).findFirst().get() ;	
		checkBadReadStats(root1, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root1, "unmappedReads", 0, 0, "0.00");
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00");
		checkBadReadStats(root1, "trimmedBases", 1, 13, "13.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {1, 5, 5, 5, 5, 5,5}, "5.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP  ,new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {1,62,62,62,62,62,62},"62.00");
		
		
		// <sequenceMetrics name="reads" readCount="2">					
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("reads")).findFirst().get() ;		
		assertTrue(root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("2"));
		checkDiscardReads(root1, 0,0,0);
		
		// check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;		   					
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MAX, "50")); 	
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEAN, "43")); 
		
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("countedReads")).findFirst().get() ;	
		assertTrue(checkChildValue(groupE, ReadGroupSummary.UNPAIRED_READ, "0"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.READ_COUNT, "2")); 
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_COUNT, "80"));
		// here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_PERCENT, "80.00"));   
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_COUNT, "100"));
		
	}
	
	@Test 
	public void rgUnkownTest()throws Exception {
		String rgid = XmlUtils.UNKNOWN_READGROUP;  // here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid);
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		// must be after readSummary2Xml(root)
		assertTrue(rgSumm.getReadCount() == 1);  // counted reads is  1	
		
		// <sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("basesLost")).findFirst().get() ;	
		checkBadReadStats(root1, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root1, "unmappedReads", 0, 0, "0.00");
		checkBadReadStats(root1, "trimmedBases", 0, 0, "0.00");
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP  ,new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
				
		// <sequenceMetrics name="reads" readCount="2">					
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("reads")).findFirst().get() ;	
		checktLen(root1, 0, 0,  0, 0,0);
		assertTrue(root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("1"));
		checkDiscardReads(root1, 0,0,0);
		
		// check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;		  		
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MAX, "75")); 	
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEAN, "75")); 	
		
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("countedReads")).findFirst().get() ;			
		assertTrue(checkChildValue(groupE, ReadGroupSummary.READ_COUNT, "1")); 
		assertTrue(checkChildValue(groupE, ReadGroupSummary.UNPAIRED_READ, "1"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_COUNT, "0"));
		// here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_PERCENT, "0.00"));   
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_COUNT, "75"));		
		
		
	}
		
	@Test
	public void rgBigTest() throws Exception {
		
		String rgid = "1959T";  
		ReadGroupSummary rgSumm = createRGElement(rgid);
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);				
		assertTrue(rgSumm.getReadCount() == 6);  // counted reads is 9-1-1-1 =6			
				
		// <sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("basesLost")).findFirst().get() ;	
		checkBadReadStats(root1, "duplicateReads", 2, 80, "33.33"); 
		checkBadReadStats(root1, "unmappedReads", 1, 40, "16.67");
		checkBadReadStats(root1, "trimmedBases", 0, 0, "0.00");
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {1 ,26,26,26,26,26,26},"10.83");	
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP, new int[] {3,3,8 ,5 ,3,5,16}, "6.67");			
				
		// <sequenceMetrics name="reads" readCount="2">					
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("reads")).findFirst().get() ;	
		checktLen(root1,2, 93,  59, 26,93);
		assertTrue(root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("9"));
		checkDiscardReads(root1, 1,1,1);
		
		// check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;		   					
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MAX, "40")); 	
		assertTrue(checkChildValue(groupE, ReadGroupSummary.MEAN, "39")); 
		
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("countedReads")).findFirst().get() ;		
		assertTrue(checkChildValue(groupE, ReadGroupSummary.READ_COUNT, "6")); 
		assertTrue(checkChildValue(groupE, ReadGroupSummary.UNPAIRED_READ, "0"));
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_COUNT, "162"));
		// here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_LOST_PERCENT, "67.50"));   
		assertTrue(checkChildValue(groupE, ReadGroupSummary.BASE_COUNT, "240"));				
		
	}
		
	@Test
	public void overallTest() throws Exception {
		// overall readgroup should manually  setMaxBases(long);
		Element root = XmlElementUtils.createRootElement("root",null);
		BamSummarizer bs = new BamSummarizer();
		// BamSummarizer2 bs = new BamSummarizer2(200, null, true);
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(input.getAbsolutePath()); 
		sr.toXml(root);	
		
		root = XmlElementUtils.getOffspringElementByTagName(root, "bamSummary").get(0);
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
		.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(XmlUtils.ALL_BASE_LOST)).findFirst().get() ;	
		assertTrue(checkChildValue(root1, ReadGroupSummary.READ_COUNT, "9"));
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_COUNT , "415"));	
		// duplicate 80/415=19.28
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_DUPLICATE,   ReadGroupSummary.BASE_LOST_PERCENT, "_"), "19.28"));  // 80/415
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_UNMAPPED, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "9.64"));   // 40/415
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_NOT_PROPER_PAIR, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "0.00"));   // 0/415
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_TRIM , ReadGroupSummary.BASE_LOST_PERCENT, "_"), "3.13"));   // 13/415
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_SOFTCLIP, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "1.20"));   // 5/415	 
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_HARDCLIP, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "3.86"));   // 16/415
		assertTrue(checkChildValue(root1, StringUtils.getJoinedString(ReadGroupSummary.NODE_OVERLAP , ReadGroupSummary.BASE_LOST_PERCENT, "_"), "21.20"));   // 88/415	   		
			
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals(XmlUtils.OVERALL)).findFirst().get() ;
		assertTrue(checkChildValue(root1, "Number of cycles with greater than 1% mismatches", "0"));  
		assertTrue(checkChildValue(root1, "Average length of first-of-pair reads", "36")); //(37+35+37)/3
		assertTrue(checkChildValue(root1, "Average length of second-of-pair reads", "41")); //(32+50)/2
		assertTrue(checkChildValue(root1, "Discarded reads (FailedVendorQuality, secondary, supplementary)", "3"));  	
		assertTrue(checkChildValue(root1, "Total reads including discarded reads", "12"));  // 
	}				

	@Test
	/**
	 * test some invalid reads, such as 
	 * ST-E00110:380:H3NCKCCXY:3:2220:10084:38684	117	chrY	239007	0	*	=	239007	0	*	*	PG:Z:MarkDuplicates	RG:Z:c9516885-22af-4fbc-8acb-1dafeca5925d	AS:i:0	XS:i:0
 	 * ST-E00110:380:H3NCKCCXY:3:2120:3752:45329	69	chrY	239631	0	*	=	239631	0	*	*	PG:Z:MarkDuplicates	RG:Z:c9516885-22af-4fbc-8acb-1dafeca5925d	AS:i:0	XS:i:0
	 */
	public  void unMappedReadTest() throws Exception {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(239007);
		record.setReferenceName("chrY");
				
		ReadGroupSummary rgSumm = new ReadGroupSummary(null, false);
		for (int flag : new int[] {117, 69, 181}) {
			record.setFlags(flag);
			rgSumm.parseRecord(record);
		}
		// add one more read with seq to avoid max lenght is zero
		record.setReadBases(new byte[] {1,2,3,4,5,6,7});
		rgSumm.parseRecord(record);
		
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		
		// <sequenceMetrics name="basesLost">
		Element	root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
						.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("basesLost")).findFirst().get() ;	
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_TRIM , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		
		// <sequenceMetrics name="reads"
		root1  = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("reads")).findFirst().get() ;	
		assertTrue(root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("4"));
		checktLen(root1,0, 0,  0, 0,0);		
		checkDiscardReads(root1, 0,0,0);
		
		// <variableGroup name="countedReads">
		root1 = XmlElementUtils.getChildElementByTagName(root1, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("countedReads")).findFirst().get() ;	
		assertTrue(checkChildValue(root1, ReadGroupSummary.READ_COUNT, "4")); 
		assertTrue(checkChildValue(root1, ReadGroupSummary.UNPAIRED_READ, "0"));
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_LOST_PERCENT, "100.00"));   
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_COUNT, "28"));		
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_LOST_COUNT, "28"));		
	}
	
	@Test
	public void noSeqReadTest() throws Exception {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(239007);
		record.setReferenceName("chrY");

		ReadGroupSummary rgSumm = new ReadGroupSummary(null, false);
		// 64: unpaired read, 65: not proper pair read
		for (int flag : new int[] {64, 65}) {
			record.setFlags(flag);
			rgSumm.parseRecord(record);
		}	
		// add one more read with seq to avoid max lenght is zero
		record.setReadBases(new byte[] {1,2,3,4,5,6,7});
		rgSumm.parseRecord(record);
		
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		
		Element	root1 = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VARIABLE_GROUP)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("countedReads")).findFirst().get() ;
		assertTrue(checkChildValue(root1, ReadGroupSummary.READ_COUNT, "3")); 
		assertTrue(checkChildValue(root1, ReadGroupSummary.UNPAIRED_READ, "1"));
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_LOST_PERCENT, "100.00"));   
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_COUNT, "21"));		
		assertTrue(checkChildValue(root1, ReadGroupSummary.BASE_LOST_COUNT, "21"));		
		
		
		// <sequenceMetrics name="basesLost">
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS)		
					.stream().filter(ele -> ele.getAttribute(XmlUtils.NAME).equals("basesLost")).findFirst().get() ;	
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_TRIM , new int[] {1, 7,7, 7, 7,7,7}, "33.33");		
	}
	
	
}

