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


public class AlignerCompareTest {
    public static final String INPUT_SAM1 = "./input1.sam";
    public static final String INPUT_SAM2 = "./input2.sam";
    public static final String OUTPUT_BAM = "./output.bam";

    @After
    public void deleteFiles() {
        //delete inputs
        File in1 = new File(INPUT_SAM1);
        File in2 = new File(INPUT_SAM2);
        in1.delete();
        in2.delete();

        //delete output
        File[] files = new File("./").listFiles();
        assert files != null;
        for (File f : files) {
			if (f.toString().startsWith(OUTPUT_BAM)) {
				f.delete();
			}
		}

    }

    @Before
    public void before() {
        createSAMs();
    }

    @Test
    public void mainTest() throws Exception {

        final String[] args1 = {"-i", INPUT_SAM1, "-i", INPUT_SAM2, "-o", OUTPUT_BAM};
        AlignerCompare.main(args1);
    }

    public static void createSAMs() {
		List<String> myData = getMyData();

		List<String> myData1 = getMyData1();

		List<String> myData2 = getMyData2();

		//add invalid record since mapq is not zero for unmapped reads

        try {

            BufferedWriter writer1 = new BufferedWriter(new FileWriter(INPUT_SAM1));
            BufferedWriter writer2 = new BufferedWriter(new FileWriter(INPUT_SAM2));

            //create SAM
            for (String line : myData) {
                writer1.write(line + "\n");
                writer2.write(line + "\n");
            }

            for (String line : myData1)
                writer1.write(line + "\n");

            for (String line : myData2)
                writer2.write(line + "\n");

            writer1.close();
            writer2.close();

        } catch (IOException e) {
            System.err.println(e + "\n\t can't write to : " + INPUT_SAM1 + " or " + INPUT_SAM2);
        }
    }

	private static List<String> getMyData2() {
		List<String> mydata2 = new ArrayList<>();
		mydata2.add("603_1108_0002	73	GL000196.1	319	3	50M	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	NH:i:2	IH:i:2	HI:i:2	MD:Z:26T3CG18");
		mydata2.add("603_1108_0002	133	GL000196.1	319	0	*	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530");
		return mydata2;
	}

	private static List<String> getMyData1() {
		List<String> mydata1 = new ArrayList<>();
		mydata1.add("603_1108_0002	73	GL000196.1	319	3	50M	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA" + "	X0:i:1	X1:i:0	MD:Z:100	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:0	NM:i:0	SM:i:37	XM:i:0	XO:i:0	XT:A:U");
		mydata1.add("603_1108_0002	133	GL000196.1	319	0	*	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	HI:i:2");
		return mydata1;
	}

	private static List<String> getMyData() {
		List<String> myData = new ArrayList<>();

		//common
		myData.add("@HD	VN:1.4	SO:queryname");
		myData.add("@SQ	SN:GL000196.1	LN:38914");

		myData.add("@RG	ID:2010072264129530	LB:Library_20100413_C	DS:RUNTYPE{50F}	SM:S0414_20100607_2_FragBC_bcSample1_F3_bcA10_05");
		myData.add("@PG	ID:2010072264129500	PN:MANUAL");
		myData.add("603_1107_1232	0	GL000196.1	480	1	25M25H	*	0	0	AATCACTTGAACCCAGGAGGCGGAG	IIIIIIIIIIIIIIIIIIIIIIII:	RG:Z:2010072264129530	CS:Z:T30321120120100120220330223100133302310303131133123	AS:i:24	CQ:Z:BBBB@AAA>><>B@;9AA<:BB=@>:AB<<=@9@7'9<22>?921<:/'1	XN:i:24	NH:i:10	IH:i:2	HI:i:1	CC:Z:GL000247.1	CP:i:35405	MD:Z:25");
		myData.add("603_1107_1233	163	GL000196.1	36008	29	75M	=	36083	142	GGATCTAGAATGCTGAAGGATCTAGTGTGTTGAGGGATCTAGCATGCTGAAGGATCTAGCATGTTAAGGGATCTA	BBBFFFFFFFFFFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFI	X0:i:1	X1:i:0	ZC:i:5	MD:Z:8G66	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:U");
		myData.add("603_1107_1233	83	GL000196.1	36083	29	4S67M4S	=	36008	-142	TCTAGCATGTCGAGAGATCTAGCATGCTGAAGGATCTAGCATGCTGAAGGATCTAGCATGTTGAGGGTTCTAGTG	FFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFFFFFFFBBB	ZC:i:5	MD:Z:63A3	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:M");

		myData.add("603_1108_0001	0	GL000196.1	38525	3	37M5D13H	*	0	0	AGGCTGAGGTGGGCGGATCACTTGAGGTCCAGAGTTC	IIIIIIIIIIIIIIIII;?IIIB@IIIBAIIIIIIII	RG:Z:2010072264129530	CS:Z:T32032122011003302321120122012012221023222003301200	AS:i:30	CQ:Z:<?=?8?9<<=@?<@@?@5'96<:)8;?9*8@?:7B?@<:A@;<+?0-8:8	XN:i:28	NH:i:10	IH:i:2	HI:i:2	MD:Z:29C7");
		return myData;
	}
}
