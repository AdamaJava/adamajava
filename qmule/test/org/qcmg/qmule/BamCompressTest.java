package org.qcmg.qmule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BamCompressTest {
    public static final String INPUT_SAM = "./input.sam";
    public static final String OUTPUT_BAM = "./output.bam";

    @After
    public void deleteFiles() {
        File in = new File(INPUT_SAM);
        File out = new File(OUTPUT_BAM);

        in.delete();
        out.delete();
    }

    @Before
    public void before() {
        createBAM(INPUT_SAM);
    }

    @Test
    public void mainTest() throws Exception {
        final String[] args1 = {"-i", INPUT_SAM, "-o", OUTPUT_BAM, "--compressLevel", "1"};
        final String[] args2 = {"-i", INPUT_SAM, "-o", OUTPUT_BAM, "--compressLevel", "9"};

        BAMCompress.main(args1);
        BAMCompress.main(args2);
    }

    public static void createBAM(String INPUT_SAM) {
        List<String> mydata = new ArrayList<>();

        //common
        mydata.add("@HD	VN:1.0");
        mydata.add("@SQ	SN:GL000196.1	LN:38914");

        mydata.add("@RG	ID:2010072264129530	LB:Library_20100413_C	DS:RUNTYPE{50F}	SM:S0414_20100607_2_FragBC_bcSample1_F3_bcA10_05");
        mydata.add("@PG	ID:2010072264129500	PN:MANUAL");
        mydata.add("1035_217_1202	272	GL000196.1	319	3	50M	*	0	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	NH:i:2	IH:i:2	HI:i:2	MD:Z:26T3CG18");
        mydata.add("603_1107_1232	0	GL000196.1	480	1	25M25H	*	0	0	AATCACTTGAACCCAGGAGGCGGAG	IIIIIIIIIIIIIIIIIIIIIIII:	RG:Z:2010072264129530	CS:Z:T30321120120100120220330223100133302310303131133123	AS:i:24	CQ:Z:BBBB@AAA>><>B@;9AA<:BB=@>:AB<<=@9@7'9<22>?921<:/'1	XN:i:24	NH:i:10	IH:i:2	HI:i:1	CC:Z:GL000247.1	CP:i:35405	MD:Z:25");
        mydata.add("828_1019_1921	0	GL000196.1	38525	3	37M5D13H	*	0	0	AGGCTGAGGTGGGCGGATCACTTGAGGTCCAGAGTTC	IIIIIIIIIIIIIIIII;?IIIB@IIIBAIIIIIIII	RG:Z:2010072264129530	CS:Z:T32032122011003302321120122012012221023222003301200	AS:i:30	CQ:Z:<?=?8?9<<=@?<@@?@5'96<:)8;?9*8@?:7B?@<:A@;<+?0-8:8	XN:i:28	NH:i:10	IH:i:2	HI:i:2	MD:Z:29C7");

        //add invalid record since mapq is not zero for unmapped reads
        mydata.add("HWI-ST1240:142:H089FADXX:2:1107:3855:25088	163	GL000196.1	36008	29	75M	=	36083	142	GGATCTAGAATGCTGAAGGATCTAGTGTGTTGAGGGATCTAGCATGCTGAAGGATCTAGCATGTTAAGGGATCTA	BBBFFFFFFFFFFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFI	X0:i:1	X1:i:0	ZC:i:5	MD:Z:8G66	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:U");
        mydata.add("HWI-ST1240:142:H089FADXX:2:1107:3855:25088	83	GL000196.1	36083	29	4S67M4S	=	36008	-142	TCTAGCATGTCGAGAGATCTAGCATGCTGAAGGATCTAGCATGCTGAAGGATCTAGCATGTTGAGGGTTCTAGTG	FFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFFFFFFFBBB	ZC:i:5	MD:Z:63A3	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:M");

        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(INPUT_SAM));

            //create SAM
            for (String line : mydata) {
				writer.write(line + "\n");
			}
            writer.close();

        } catch (IOException e) {
            System.err.println(e + "\n\t can't write to : " + INPUT_SAM);
        }
    }
}
