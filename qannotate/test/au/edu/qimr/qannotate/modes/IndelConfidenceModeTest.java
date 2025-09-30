package au.edu.qimr.qannotate.modes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.vcf.VcfFileReader;

import au.edu.qimr.qannotate.Main;

import static org.junit.Assert.*;

public class IndelConfidenceModeTest {
	public static String dbMask = "repeat.mask";
	public static String input = "input.vcf";
	public static String output = "output.vcf";
	public static String log = "output.log";
	
	@After
	public void clear(){
		
        new File(dbMask).delete();
        new File(input).delete();
        new File(output).delete();
        new File(log).delete();
	}
	
	@Test
	public void infoTest(){
		
		IndelConfidenceMode mode = new IndelConfidenceMode();	
		
		String str = "chr1	11303744	.	C	CA	37.73	PASS	SOMATIC;HOM=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745	GT:AD:DP:GQ:PL:ACINDEL	.:.:.:.:.:0,39,36,0[0,0],0,4,4	0/1:30,10:40:75:75,0,541:7,80,66,8[4,4],1,7,8";
//		String str = "chr1	11303744	.	C	CA	37.73	HOM5	SOMATIC;HOMTXT=AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745	GT:AD:DP:GQ:PL:ACINDEL	.:.:.:.:.:0,39,36,0[0,0],0,4,4	0/1:30,10:40:75:75,0,541:7,80,66,8[4,4],1,7,8";

		VcfRecord vcf = new	VcfRecord(str.split("\\t"));
        assertSame(MafConfidence.HIGH, mode.getConfidence(vcf));
 				
		//HOMCNTXT is no longer checked
		vcf.setInfo("SOMATIC;HOMCNTXT=9,AGCCTGTCTCaAAAAAAAAAA;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.HIGH, mode.getConfidence(vcf));
		vcf.setInfo("SOMATIC;HOM=9,AGCCTGTCTCaAAAAAAAAAA;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.LOW, mode.getConfidence(vcf));
		
		
		//no homopolymers (repeat)
		vcf = new	VcfRecord(str.split("\\t"));
		vcf.setInfo("SOMATIC;NIOC=0.087;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.HIGH, mode.getConfidence(vcf));
		
		//somatic SSOI
		vcf.setInfo("SOMATIC;NIOC=0.087;SSOI=0.1;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.HIGH, mode.getConfidence(vcf));
		
		//germline SSOI high
		vcf.setInfo("NIOC=0.087;SSOI=0.2;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.HIGH, mode.getConfidence(vcf));
		
		//germline SSOI low
		vcf.setInfo("NIOC=0.087;SSOI=0.1;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.LOW, mode.getConfidence(vcf));
				
		//9 base repeat
		//vcf.setFilter(IndelUtils.FILTER_HOM + "9");
		vcf.setInfo("SOMATIC;HOM=9,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.LOW, mode.getConfidence(vcf));

		//no repeat but too many nearby indel
		vcf.setInfo("SOMATIC;HOM=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.187;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.LOW, mode.getConfidence(vcf));
		
		
		//vcf.setFilter(IndelUtils.FILTER_HOM + "3" );
		vcf.setInfo("SOMATIC;HOM=3,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745");
        assertSame(MafConfidence.HIGH, mode.getConfidence(vcf));
		
		vcf.setFilter(IndelUtils.FILTER_MIN );
        assertSame(MafConfidence.LOW, mode.getConfidence(vcf));
		
	}
	
	
	@Test
	 public void confidenceRealLifeIndel() {
		 //chr2    29432822        .       C       CGCGAATT        .       HCOVT   AC=2;AF=1.00;AN=2;DP=1174;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=59.98;QD=7.19;SOR=11.114;HOM=0,GGATTTTATCgcgaattTCCAAGCTGA;CONF=ZERO   GT:GD:AD:DP:GQ:PL       .:.:.:.:.:.     1/1:CGCGAATT/CGCGAATT:0,258:258:99:8481,606,0
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr2","29432822",".","C","CGCGAATT",".","HCOVT","AC=2;AF=1.00;AN=2;DP=1174;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=59.98;QD=7.19;SOR=11.114;HOM=0,GGATTTTATCgcgaattTCCAAGCTGA","GT:GD:AD:DP:GQ:PL"," .:.:.:.:.:.","1/1:CGCGAATT/CGCGAATT:0,258:258:99:8481,606,0"});
		 
		 IndelConfidenceMode cm = new IndelConfidenceMode();
        assertSame(MafConfidence.ZERO, cm.getConfidence(vcf1));
	 }
    @Test
    public void confidenceRealLifeIndel2() {
        //chr2    29432822        .       C       CGCGAATT        .       HCOVT   AC=2;AF=1.00;AN=2;DP=1174;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=59.98;QD=7.19;SOR=11.114;HOM=0,GGATTTTATCgcgaattTCCAAGCTGA;CONF=ZERO   GT:GD:AD:DP:GQ:PL       .:.:.:.:.:.     1/1:CGCGAATT/CGCGAATT:0,258:258:99:8481,606,0
        VcfRecord vcf1 = new VcfRecord(new String[]{"chr2","29432822",".","C","CGCGAATT",".","HCOVT","AC=2;AF=1.00;AN=2;DP=1174;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=59.98;QD=7.19;SOR=11.114;HOM=0,GGATTTTATCgcgaattTCCAAGCTGA","GT:GD:AD:DP:GQ:PL"," .:.:.:.:.:.","1/1:CGCGAATT/CGCGAATT:0,258:258:99:8481,606,0"});

        IndelConfidenceMode cm = new IndelConfidenceMode();
        assertSame(MafConfidence.ZERO, cm.getConfidence(vcf1));
    }
	
	@Test
	public void maskTest() {
		
		try{
			createMask();
			createVcf();
			
			String[] args ={"--mode","IndelConfidence", "-i", new File(input).getAbsolutePath(), "-o", new File(output).getAbsolutePath(),
					"-d", new File(dbMask).getAbsolutePath(), "--log", new File(log).getAbsolutePath()};									
			Main.main(args);
						
	        try (VcfFileReader reader = new VcfFileReader(new File(output))) {
	        	int i = 0;
	        	//mask 53744 53780 
	        	for ( VcfRecord re : reader) {		
	        		i ++;
	        		if(i == 1) 
	        			//chr1	53741	.	CTT	C
                        assertEquals(re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE), MafConfidence.HIGH.name());
	        		else  if(i == 2)
	        			//chr1	53742	.	C	CA
                        assertEquals(re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE), MafConfidence.HIGH.name());
	        		else  if(i == 3)
	        			//chr1	53742	.	CTT	C
                        assertEquals(re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE), MafConfidence.ZERO.name());
	        		else  if(i == 4) 	        			
	        			//chr1	53743	.	C	CA
	        			//the insertion happened bwt 53743 and 53744, so it is before repeat region
                        assertEquals(re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE), MafConfidence.HIGH.name());
	        		 else  if(i == 5)
	        			//chr1	53744	.	CTT	C
                        assertEquals(re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE), MafConfidence.ZERO.name());
	        	}
	        	assertEquals(5, i);	
	        }
			
		} catch(Exception e){
			System.out.println(e.getMessage());
			fail( "My method threw an exception" );
		}
	
	}

    @Test
    public void homopolymerOptionTest() {
        try{
            createMask();
            createHomVcf();

            String[] args ={"--mode","IndelConfidence", "-i", new File(input).getAbsolutePath(), "-o", new File(output).getAbsolutePath(),
                    "-d", new File(dbMask).getAbsolutePath(), "--log", new File(log).getAbsolutePath()};
            Main.main(args);

            try (VcfFileReader reader = new VcfFileReader(new File(output))) {
                int i = 0;
                for ( VcfRecord re : reader) {
                    if (++i == 1) {
                        assertEquals(MafConfidence.LOW.name(), re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE));
                    } else if (i == 2) {
                        assertEquals(MafConfidence.HIGH.name(), re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE));
                    }
                }
                assertEquals(2, i);
            }

            args = new String[]{"--mode", "IndelConfidence", "-i", new File(input).getAbsolutePath(), "-o", new File(output).getAbsolutePath(),
                    "-d", new File(dbMask).getAbsolutePath(), "--log", new File(log).getAbsolutePath(), "--homCutoff", "8"};
            Main.main(args);

            try (VcfFileReader reader = new VcfFileReader(new File(output))) {
                int i = 0;
                for ( VcfRecord re : reader) {
                    if (++i == 1) {
                        assertEquals(MafConfidence.HIGH.name(), re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE));
                    } else if (i == 2) {
                        assertEquals(MafConfidence.HIGH.name(), re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE));
                    }
                }
                assertEquals(2, i);
            }

        } catch(Exception e){
            System.out.println(e.getMessage());
            fail( "My method threw an exception" );
        }
    }
	
	public static void createMask() throws IOException{
		        final List<String> data = new ArrayList<>();
        data.add("genoName genoStart genoEnd repClass repFamily");
        data.add("1 53744 53780 Low_complexity Low_complexity");
        data.add("chr1 54712 54820 Simple_repeat Simple_repeat");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(dbMask))) {
            for (final String line : data)   out.write(line +"\n");                  
         }  
	}
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        
        data.add("chr1	53741	.	CTT	C	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
        data.add("chr1	53742	.	C	CA	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");      
        data.add("chr1	53742	.	CTT	C	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
        data.add("chr1	53743	.	C	CA	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
        data.add("chr1	53744	.	CTT	C	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
       
        try(BufferedWriter out = new BufferedWriter(new FileWriter(input))) {
            for (final String line : data)   out.write(line +"\n");                  
         }  

	}
    public static void createHomVcf() throws IOException{
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t29ab944c-f37f-4531-95ac-57c52c53be6f\t78ab1438-d237-4cff-a76a-835320a5fb0f");
        data.add("chr1\t897580\trs397863599\tAT\tA\t404.73\tPASS\tBaseQRankSum=1.304;ClippingRankSum=0.000;DP=50;ExcessHet=3.0103;FS=2.463;MQ=60.00;MQRankSum=0.000;QD=8.26;ReadPosRankSum=-0.193;SOR=0.751;NIOC=0;SSOI=0.286;SVTYPE=DEL;END=897581;IN=1;VLD;DB;VAF=0.696;HOM=8,GACTCACTGA_TTTTTTTCTA\tGT:AD:DP:G;D:GQ:PL:ACINDEL\t0/1:29,20:49:AT/A:99:442,0,732:14,50,49,14[7,7],18[18],0,0,1\t0/1:10,51:61:AT/A:99:1367,0,121:43,61,61,48[21,27],48[43],0,0,1");
        data.add("chr1\t1303675\trs60404130\tC\tCCCT\t1037.73\tPASS\tBaseQRankSum=1.750;ClippingRankSum=0.000;DP=44;ExcessHet=3.0103;FS=13.869;MQ=59.81;MQRankSum=0.000;QD=30.90;ReadPosRankSum=2.125;SOR=1.634;NIOC=0.030;SSOI=0.758;SVTYPE=INS;END=1303676;IN=1;VLD;DB;VAF=0.86;HOM=5,CCTGTTGCCCcctCCTCTTTCTC\tGT:AD:DP:GD:GQ:PL:ACINDEL\t1/1:3,24:27:CCCT/CCCT:72:1075,72,0:23,33,33,25[6,19],28[25],0,1,2\t1/1:5,17:22:CCCT/CCCT:23:537,23,0:18,34,33,18[10,8],21[21],1,1,4");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(input))) {
            for (final String line : data)   out.write(line +"\n");
        }

    }
}
