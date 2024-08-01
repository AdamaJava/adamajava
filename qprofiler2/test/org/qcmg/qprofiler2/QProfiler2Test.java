package org.qcmg.qprofiler2;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class QProfiler2Test {

    private static final String DODGY_FILE_NAME = "solid0039_20091125_DODGY.vcf";
    private static final String GFF_FILE_NAME = "solid0039_20091125_2.gff";
    private static final String BAM_FILE_NAME = "solid0039_20091125_2.sam";
    private static final String LONGREAD_BAM_FILE_NAME = "longread_valid_test.sam";

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();
    public static File BAM_FILE;
    public static File LONGREAD_BAM_FILE;

    @BeforeClass
    public static void setup() throws IOException {
        BAM_FILE = testFolder.newFile(BAM_FILE_NAME);
        createTestFile(BAM_FILE, null);

        LONGREAD_BAM_FILE = testFolder.newFile(LONGREAD_BAM_FILE_NAME);
        createLongReadTestFile(LONGREAD_BAM_FILE, null);

    }

    @AfterClass
    public static void cleanup() {
        // delete qprofiler.xml which was default output
        String outputFile = System.getProperty("user.dir") + FileSystems.getDefault().getSeparator() + "qprofiler.xml";
        File output = new File(outputFile);
        if (output.exists()) {
            output.delete();
        }
    }

    @Test
    public final void executeWithValidArguments() throws Exception {
        File logFile = testFolder.newFile("executeWithValidArguments.log");
        File outputFile = testFolder.newFile("executeWithValidArguments.xml");
        // qprofiler2 accept two same input once type are XML, VCF, FASTQ or BAM
        String[] args = {"-log", logFile.getAbsolutePath(), "-input", BAM_FILE.getAbsolutePath(),
                "-input", BAM_FILE.getAbsolutePath(), "-o", outputFile.getAbsolutePath()};
        int exitStatus = new QProfiler2().setup(args);
        assertEquals(0, exitStatus);

        //LongRead mode
        // qprofiler2 accept two same input once type are XML, VCF, FASTQ or BAM
        String[] lrArgs = {"-log", logFile.getAbsolutePath(), "-input", LONGREAD_BAM_FILE.getAbsolutePath(),
                "--long-read","-o", outputFile.getAbsolutePath()};
        exitStatus = new QProfiler2().setup(lrArgs);
        assertEquals(0, exitStatus);

        File dodgyFile = testFolder.newFile(DODGY_FILE_NAME);
        createTestFile(dodgyFile, getDodgyFileContents());
        // argument are correct input is doggy , the exception are caught
        args = new String[]{"-log", logFile.getAbsolutePath(), "-input", dodgyFile.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath()};
        exitStatus = new QProfiler2().setup(args);
        assertEquals(1, exitStatus);

        assertTrue(outputFile.exists());
    }

    @Test
    public final void executeWithNoArgs() {
        String[] args = {};
        try {
            int exitStatus = new QProfiler2().setup(args);
            assertEquals(1, exitStatus);
        } catch (Exception e) {
            fail("no exception should have been thrown from executeWithNoArgs()");
        }
    }

    @Test
    public final void executeWithInvalidArgs() throws Exception {
        File logFile = testFolder.newFile("executeWithInvalidArguments.log");

        //"-include" will be treated as "-i"; "-i" was short option for both "input" and "index"
        //eg. String[] args2 = new String[] {"-input", BAM_FILE.getAbsolutePath(), "-log",logFile.getAbsolutePath(), "-include", "html,all,matricies,coverage" };
        //get error: assertEquals("'i' is not a recognized option", e.getMessage() );

        try {
            //"--include" is long option, it won't be ambiguous with "input"
            String[] args2 = new String[]{"-i", BAM_FILE.getAbsolutePath(), "-log", logFile.getAbsolutePath(), "--include", "html,all,matricies,coverage"};
            new QProfiler2().setup(args2);
            fail("no exception should have been thrown from executeWithExcludeArgs()");
        } catch (Exception e) {
            //here "include" is invalid, so it throw the error with long option
            assertEquals("include is not a recognized option", e.getMessage());
        }
    }

    @Test
    public final void executeWithInvalidFileType() throws Exception {
        File gffFile = testFolder.newFile(GFF_FILE_NAME);
        File logFile = testFolder.newFile("executeWithInvalidFileType.log");
        File inputFile = testFolder.newFile("executeWithInvalidFileType.test");

        String[] args1 = {"-input", inputFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()};
        String[] args2 = new String[]{"-input", gffFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()};

        for (String[] args : new String[][]{args1, args2}) {
            try {
                new QProfiler2().setup(args);
                fail("Should have thrown a QProfilerException");
            } catch (Exception qpe) {
                assertTrue(qpe.getMessage().contains("Unsupported file type"));
            }
        }
    }

    @Test
    public final void executeWithInvalidFileTypeAndLongReadOption() throws Exception {
                File gffFile = testFolder.newFile("executeWithInvalidFileTypeLongRead.vcf");
                File logFile = testFolder.newFile("executeWithInvalidFileTypeLongRead.log");
                File inputFile = testFolder.newFile("executeWithInvalidFileTypeLongRead.xml");

                String[] args1 = {"-input", inputFile.getAbsolutePath(), "-log", logFile.getAbsolutePath(), "--long-read"};
                String[] args2 = new String[]{"-input", gffFile.getAbsolutePath(), "-log", logFile.getAbsolutePath(), "--long-read"};

                for (String[] args : new String[][]{args1, args2}) {
                try {
                    new QProfiler2().setup(args);
                    fail("Should have thrown a QProfilerException");
                } catch (Exception qpe) {
                    assertTrue(qpe.getMessage().contains("Long read option can only be chosen for BAM files"));
                }
            }
    }

    @Test
    public void schmeFileTest() throws IOException {
        String nameSpace = "https://adamajava.org/xsd/qprofiler2/v3";
        String xmlns = "xmlns=\"" + nameSpace + "\"";

        String schemaXsi = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
        String schemaStr = "xsi:schemaLocation=\"" + nameSpace + " https://purl.org/adamajava/xsd/qprofiler2/v3/qprofiler2.xsd\"";
        String input = BAM_FILE.getAbsolutePath();
        File logFile = testFolder.newFile("qProfilerNode.log");
        String[] args = {"-input", input, "-log", logFile.getAbsolutePath()};
        try {
            // print full header
            new QProfiler2().setup(args);
            assertEquals(1, Files.lines(Paths.get("qprofiler.xml")).filter(s -> s.contains(schemaStr)).count());
            assertEquals(1, Files.lines(Paths.get("qprofiler.xml")).filter(s -> s.contains(schemaXsi)).count());
            assertEquals(1, Files.lines(Paths.get("qprofiler.xml")).filter(s -> s.contains(xmlns)).count());
        } catch (Exception qpe) {
            fail("a QProfilerException for args: " + Arrays.toString(args));
        }
    }

    @Test
    public void bamHeaderOptionTest() throws IOException, ParserConfigurationException {

        String input = BAM_FILE.getAbsolutePath();
        File logFile = testFolder.newFile();
        String[] args = {"-input", input, "-log", logFile.getAbsolutePath()};
        try {
            // print full header
            new QProfiler2().setup(args);
            Path path = Paths.get("qprofiler.xml");
            List<String> lines = Files.readAllLines(path);
            assertEquals(1, lines.size());
            String line = lines.getFirst();

            assertTrue(line.contains("<headerRecords TAG=\"HD\""));
            assertTrue(line.contains("<headerRecords TAG=\"SQ\""));
            assertFalse(line.contains("<headerRecords TAG=\"PG\""));
            assertFalse(line.contains("<headerRecords TAG=\"RG\""));

            // default mode, only HD and RG
            args = new String[]{"-input", input, "-log", logFile.getAbsolutePath(), "--bam-full-header"};
            new QProfiler2().setup(args);

            lines = Files.readAllLines(path);
            assertEquals(1, lines.size());
            line = lines.getFirst();

            assertTrue(line.contains("<headerRecords TAG=\"HD\""));
            assertTrue(line.contains("<headerRecords TAG=\"SQ\""));
            assertTrue(line.contains("<headerRecords TAG=\"PG\""));
            assertTrue(line.contains("<headerRecords TAG=\"RG\""));
            assertTrue(line.contains("@RG	ID:1959T"));
            assertTrue(line.contains("@RG	ID:1959N"));

        } catch (Exception qpe) {
            fail("a QProfilerException for args: " + Arrays.toString(args));
        }
    }

    private static void createLongReadTestFile(File file, List<String> data) {

        if (data == null) {
            data = new ArrayList<>();
            data = new ArrayList<>();
            data.add("@HD\tVN:1.6\tSO:coordinate");
            data.add("@SQ\tSN:chr22\tLN:50818468");
            data.add("@RG\tID:COLO829_BL.28464\tPL:ONT\tSM:COLO829_BL");

            //Read length 301 - Supplementary alignment
            data.add("12b013c5-a784-4448-9eca-140d6db89e1c\t2048\tchr22\t20164189\t1\t11M3D72M6D11M3I35M1I1M2I37M3D20M6I46M3D9M4I2M2I26M2D13M3713H\t*\t0\t0\tCCACCACCATCACCACCACCACCACCAGCAGCATCACCACCAATACCACCACCACCACCAGCATCACCATCAACACCACCATCACCACCACCATCACCACCAGCACCACCAGCACCACCACCATCACCACCAGCATCACCACCAACACCACCATCACCACCAGCATCAGCACCACCACCACCACCACCACCACTACCGTCACCACCACCACCACCACCACCACCATCACCACCACCACCACCAGCAGCATCACCACCAATACCACCACCACCACCAGCATCACCATCAACACCACCATCAC\t/6:<BEGGKJFOHKHIHOHLLKHGDCBEHGDJIF?>>7201FEGHIIHD=>CCCBCLKHSDDFHGKSJJQJPKJSMSMKNNIJSHFRGGIIFGGFILHJHHIGHFFCIHHHHHFFFPLKQMSMSLHQHHISODECDBIIKJPMIKOJJILKLJKGRJFOGHIGIIFLFHEMHLLIIGISSJJJILIGGGFHHHGHJH<;;;<CGHFNJKLINRSQMJJMKJPIKKIGHFFJGJSJHLSLJKJNJKMJILJHSKIPLFPIHHJNJIOOLKLHOLLHSCBBB?EFEHGIJJIMILSISSHOLG\ts1:i:40\ts2:i:0\tSA:Z:chr1,110120463,+,2357S1655M2I,60,48;chr21,43335930,+,202S1716M2I2094S,1,305;chr21,43335930,+,724S1700M16D1590S,60,299;\tMD:Z:11^ACT19C2C5T2C8T8C0A1C8C10^ATCAGA16T3T4C26T2C2T3T10C5C2T1^GTT4T5T5T17T5T3T13T5T1^GTT1C14T5T2C11^CC5T7\tRG:Z:COLO829_BL.28464\tML:B:C,3,16,0,2,5,18,6,28,3,4,4,1,1,12,5,8,12,2,11,3,18,3,33,17,1,17,2,1,2,5,2,1,3,4,5,36,5,2,38,9,10,125,14,15,8,13,1,14,5,9,8,10,252,239,255,253,250,237,248,227,252,3,2,254,254,243,251,247,243,253,244,252,237,252,222,238,3,238,1,254,4,250,253,254,252,11,7,43,7,5,30,16,21,52,241,240,19,17,254,241,250,15,17,20\tMM:Z:C+h?,108,444,3,1,3,5,3,11,3,5,32,10,167,1,14,3,10,31,37,244,11,17,7,20,19,17,78,15,44,13,9,16,0,96,35,46,22,15,25,4,1,11,2,0,7,9,47,1,32,38,34,5;C+m?,108,444,3,1,3,5,3,11,3,5,32,10,167,1,14,3,10,31,37,244,11,17,7,20,19,17,78,15,44,13,9,16,0,96,35,46,22,15,25,4,1,11,2,0,7,9,47,1,32,38,34,5;\tNM:i:67\tAS:i:260\tde:f:0.1463\trl:i:1308\tcm:i:3\tnn:i:0\ttp:A:P\tms:i:287\tqs:i:22");

            //Read length 1258
            data.add("e0ea8507-83a9-4e00-bb0c-b0a27f9ed0a4\t0\tchr22\t20178190\t60\t138M1I201M2D67M2D14M1D6M1I5M1D25M1D163M1I426M1D77M1I132M\t*\t0\t0\tGGGCACGTCTCTTCCCAGTGAACCTCAGATCCCTCGGGCGCCCACGCTCCCCACTGAGGGCCTGGGGCCACAGCACTCTGCCTGTGAGGGGTGGGCCCTTGGAGGCTTGGGAAGGCGCGCGCCCACAGGCACGTGGGGTAGAGCGCGCTGAGCGCTGTCTCAGTGATGGTTCCCCACACTGGCCCCTGACATGGGGCTTGGTGTGGCAGCTTGTTGGGGAGGTGGTGCGTGGAGCAGGATGGGGACCGGGAGGTGGTTGCTGTGGGCTCTGCGGGGCGTGAGGGCCGGTGTTTGCCCCAAGCTCAGTGCCTGCCTGTGACCACACCAGCTCATAAGCGACCGGGGTGGCCCAGGGGTGAGCGGAGAGGGGACCTGAGCAGGCAACACGGGCCTGGAGAGGAGCTTTGTCTCTAGGAGAAGGAGCCTGCTGCTGTACACCTGGTATCACGCCACCCCAAGCAAGCCCCGCAGAGCCCTCGGGAGCTCCACTCGGAGATGGAGACGCGCTTCCCTGTGTCGGGGCTTCGCTCAGAACATTTTTATGGGCTGTTAATTGTTATGCTTTCATTTAATTCTGGGTGTCATTCCATGTCAGCCAAGCCCGACAGGCTGGGATTTATGTCTTCCAAATTATGGGGCTCGGGCTGTTCAGAGGTAAAACAATTACCCATCAATCAGCCACCAACCGGCTTGTGCTGGGGCTGGCAGCCCCATTAGCGGGCGTGGGCAGCCATGGCCCAGCAAGACCCCTTGAGGGACAGGACTGCCTTCGAGGGCAAGGCAGTCTGGGTGAAGTCTCGCCCCAGGTTGGCCCTGTGCTGTGGCAGGGCGCAGCTCTCTGGGGAGGCGGCCGCTCAGGGAAGGACCCCAGGGTCCACATTCACACCCAACTGGCTCCAGTGCTGCAGCCCTCGGCCTCCCATATGATGGTGTGACCCTAGGTGGGCTGAGGGTGACCCGCCGGAAGACGCTGCAGCATCAAGGTGGGGGGACCATGGGTCCCATATGCTGGGGCCTGGGGAGCCTTGGCCAGCCATGGTCACTCAGTGGAGGGGCAGGGTCACCTGCCTCCTGAGCACCCCATCCCCAGGAGGGAGGAACCAGGTGTGACCCACTTCACAGGGAGGGGAAGCCTTGGCTGTCAGAGGGGACACTGCGTCTCTCACTGGGAGGACCTCCCGGCCTGGGCTTGAGGCCCACCTGGATCTGTGCAAACGTAGATGTGGGTAGGGTGGGGCAGGGGAGGGGAGGTGGGCAG\tFDDDEEFHHHJSHIKILJIGGGIHI5CJSFGC:ACFFGGIHHEEEFJOJEA6556AGHHQHNINLKMJNIMOKKNJMLMLLLK///-0E@FKMNMKQLIHLISIFG6()%%&&(--CHHJIKSPHJGHFJMHB2100..0)(&&()016@FHFMGHGFGDDFFFEECCEPPAA@MKLKNHNNKSKSLKSMINPQJSSOPMLSKJQLPLSIKMSQ>GJKSSQMKJHSLIABSNMQOJSJKIJLHFL>=CSHHKFEKMILSNLKLMILOSIOHD?>???HLKPJIHAABCBSJJSPFEDGKSPLOMSSMKRNMMSOLNJKHSLNMQLKHOJKC<-----.--/1BAHFQJLHDGOSF<<<CFFC;;<E>SKILB@AJSRKQMLSLISIKLQSSIKKMNLSKIKSOMMLGE*),8;99;<@AEA>=766662)))))3028-,,,-33365566--*('&%&()*..679EHSJSFLJGG?AA?JLIFA@?655<BSK<;=AHHFJHGLMHKMNSJNKKHPOSPMSKSPSKSOKLSKOISSSMLPLMMSRMQSOLSMOSIOSJSSOSDKSSSPSSRSO.:ISKSSMSOIOIHIOJHGGEHF3,,,,-D@>>210000,01*((()'(*1576699<9<HFFHGFKJNSQNNMOHPMOSOSKMNSKSOMSSMIPLLKSSNSSNOSNMKRGRJPEEBNHSNHFKMJROMSSRJOMIPKJILKPJMSPPKKFGDHJKKOPLSOKJNOMHKLSOLHA@?8/;??IKNJMHILSSBACCDMKOKKSKKHCKKLKOMKGGGIGKKKSKGJHSIMSNMMNJJEDEFEMKSSSGMKLLHHHFGGJ=BCGE6?NQGJLLNPOMGGGGFLFHFGFMNMPSQSLSLNSNMKONPJMOMMPIOFFGKQKI@<?:91****,<175538HGGSNKIKPKKNPHLOSONSSMMHIFSKH7<99899EGJMLMKSMMKQSSQOSNMMLSKOOMSSKLQNKRKQPRJKIQSLLQKHRJSGJEGKNIHONQS?>?>,2/02889998=??AE888888DALNLJJJ.,<=.=4A@JLLPLJLSNSJISOKLLKPOLPONOKDKSSLRKNNI=11()((*???JHHGOHNLSSNSNLLNSO:<<?.<88:>@GIMILN?>=>>>OOPMKNSSNNNSSPSKMKKHHGF9GLKSSKKSSSSSGOJPPNSQKSMJLSMLLSOLM77FAGJIFGFHFHHHHQIJHHIJ1@=;<===41235;0))***<ACCIIHFFJLGS9E\ts1:i:1139\ts2:i:0\tMD:Z:111G1C0A0T27C0T190T3^CT1T65^TC14^C7C3^C0C1G22^G3G585^G50G158\tRG:Z:COLO829_BL.28464\tML:B:C,2,5,1,1,1,2,10,12,1,6,6,1,4,15,29,194,5,2,9,0,3,3,0,16,1,6,7,2,15,8,12,3,6,1,2,13,22,6,2,0,77,49,67,28,9,3,7,254,254,254,253,13,243,254,14,14,254,9,36,31,60,7,5,20,2,252,251,254,19,254,13,248,240,31,16,27,11,249,252,253,235,220,12,253,255,145,39,56,33,13\tMM:Z:C+h?,26,11,5,69,2,0,16,14,1,7,12,11,15,2,11,7,10,14,3,2,0,5,3,2,2,20,10,11,0,11,4,3,12,9,28,2,0,6,2,0,0,28,0,2,3;C+m?,26,11,5,69,2,0,16,14,1,7,12,11,15,2,11,7,10,14,3,2,0,5,3,2,2,20,10,11,0,11,4,3,12,9,28,2,0,6,2,0,0,28,0,2,3;\tNM:i:25\tAS:i:2366\tde:f:0.0182\trl:i:0\tcm:i:204\tnn:i:0\ttp:A:P\tms:i:2368\tqs:i:20");

            //Read length 212
            data.add("ee9f0c60-5af7-495b-9c3b-d4b30971c3d2\t0\tchr22\t20173734\t60\t212M\t*\t0\t0\tCAAGGACTCCCCGCAGCCCAGTGTCTGTGGAGTGGGGCGTGAGGTGCTGCCTCCCATGTTGCTGCTTAGAAGGACGCAGCCCTGGAAACCCTCACTGTGGAGTCTCTGAGCCCCTCATCCGCAGAGCAGCAGTTGCTGCTGTTTGCACAGACAGCAAATCCGGGGCATGTTTGTTTAGTAAGATTACCTCTGCTGGGGCCTGGCCCAAGGGA\t9EISNSRSMHDEJGJJSLJSSNLSSPLPNMSJNNSSMKSMNSQSSJSNSSKSHSKJKNOSJSJSSMRMSSSLPNMSMLECCB?::>??B988;HKD@=4444:767;?IFHHEEFDQKISSMNPLSSPSKLLSSLSKSSSSKPLJMNSKKOQSJOKSLSJKIB;FDMOQISMSAA@AAJJSNNORPSMJMKNLKSSJHNMMOJJGHKPSLOJ\ts1:i:211\ts2:i:0\tMD:Z:212\tRG:Z:COLO829_BL.23729\tML:B:C,2,27,23,3,3,9,27,17,7,252\tMM:Z:C+h?,17,10,9,11,11;C+m?,17,10,9,11,11;\tNM:i:0\tAS:i:424\tde:f:0.0\trl:i:0\tcm:i:41\tnn:i:0\ttp:A:P\tms:i:424\tqs:i:21");
        }

        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            for (String line : data) out.write(line + "\n");
        } catch (IOException e) {
            System.err.println("IOException caught whilst attempting to write to test file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void createTestFile(File file, List<String> data) {

        if (data == null) {
            data = new ArrayList<>();
            data.add("@HD	VN:1.0	SO:coordinate");
            data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
            data.add("@RG	ID:1959N	SM:eBeads_20091110_ND	DS:rl=50");
            data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
            data.add("@SQ	SN:chr1	LN:249250621");
            data.add("@SQ	SN:chr11	LN:243199373");
            // unmapped
            data.add("243_146_1	101	chr1	10075	0	*	=	10167	0	" +
                    "ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
                    "RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
        }

        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            for (String line : data) out.write(line + "\n");
        } catch (IOException e) {
            System.err.println("IOException caught whilst attempting to write to test file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private List<String> getDodgyFileContents() {
        List<String> data = new ArrayList<>();
        data.add("##gff-version 3");
        data.add("##solid-gff-version 3.5");
        data.add("##source-version Gff3Merger 0.1");
        data.add("##date 2010-03-10");
        data.add("##time 15:04:36");
        data.add("##reference-file /path/reference/hg19.fa");
        data.add("##color-code AA=0,AC=1,AG=2,AT=3,CA=1,CC=0,CG=3,CT=2,GA=2,GC=3,GG=0,GT=1,TA=3,TC=2,TG=1,TT=0");
        data.add("##primer-base F3=T");
        data.add("##max-num-mismatches 10");
        data.add("##max-read-length 50");
        data.add("##line-order fragment");
        data.add("##contig 1 chr1");
        data.add("##contig 2 chr2");
        data.add("##contig 3 chr3");
        data.add("##contig 4 chr4");
        data.add("##contig 5 chr5");
        data.add("##contig 6 chr6");
        data.add("##contig 7 chr7");
        data.add("##contig 8 chr8");
        data.add("##contig 9 chr9");
        data.add("##contig 10 chr10");
        data.add("##contig 11 chr11");
        data.add("##contig 12 chr12");
        data.add("##contig 13 chr13");
        data.add("##contig 14 chr14");
        data.add("##contig 15 chr15");
        data.add("##contig 16 chr16");
        data.add("##contig 17 chr17");
        data.add("##contig 18 chr18");
        data.add("##contig 19 chr19");
        data.add("##contig 20 chr20");
        data.add("##contig 21 chr21");
        data.add("##contig 22 chr22");
        data.add("##contig 23 chrX");
        data.add("	##contig 24 chrY");
        data.add("	##contig 25 chrM");
        data.add("##conversion unique");
        data.add("##clear-zone 5");
        data.add("##history AnnotateGff3Changes.java /path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/outputs/../intermediate_maToGff3/output.325614874493032/output.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0.gff3 /path/reference/hg19.fa --out=/path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/outputs/../intermediate_maToGff3/output.325614874493032/output.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0.annotated.gff3 --tints=agy --qvThreshold=15 --b=true --cn=true");
        data.add("##history mapping/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma 50 3 0.200000 B=1.000000 P=1 L=25 F=0 m=-2.000000");
        data.add("##history MaToGff3.java /path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/temp_maToGff3/split.323675630031032/temp.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0 --out=/path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/outputs/../intermediate_maToGff3/output.325614874493032/output.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0.gff3 --clear=5 --qvs=/data/results/Aditya/gabe/gabe_raw_reads/solid0039_20091125_2_TD04_LMP_F3/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3_QV.qual --mmp=-2.0 --tempdir=/scratch/solid");
        data.add("##history filter_fasta.pl --output=/data/results/solid0039/solid0039_20091125_2_TD04_LMP/eBeads_20091110_CD/results.01/primary.20091221012849245 --name=solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD --tag=F3 --minlength=50 --mincalls=25 --prefix=T /data/results/solid0039/solid0039_20091125_2_TD04_LMP/eBeads_20091110_CD/jobs/postPrimerSetPrimary.937/rawseq");
        data.add("##hdr seqid	source	type	start	end	score	strand	phase	attributes");
        data.add("1	solid	read	10148	10190	14.4	-	.	aID=1212_1636_246;at=F3;b=GGTTAGGGTTAGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG;g=G0103200103201032001033001032001032001032001032001;mq=43;o=0;q=31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18,18,13,14,18,19,16,14,5,16,23,18,21,16,16,14,20,13,17,20,11;r=23_2;s=a23;u=0,4,1,1");
        data.add("1	solid	read	10236	10275	19.6	-	.	aID=681_1482_392;at=F3;b=TTAGGGTTAGGGTTAGGGTTTAGGGTTTAGGGTTAGGGTT;g=T0320010320010320010032001003200103200100320000320;mq=18;o=0;q=25,27,28,29,25,27,28,28,23,26,27,26,27,28,28,24,28,24,30,26,20,29,23,25,27,10,27,26,22,24,20,13,23,24,29,17,26,26,23,27,25,13,22,23,27,8,16,28,20,26;u=8,0,46,1,27,3,1");
        data.add("1	solid	read	10248	10290	10.5	+	.	aID=1578_430_829;at=F3;b=AAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCC;g=A0010023001002301003301002301002300002301002300003;mq=72;o=0;q=8,23,18,16,18,14,20,21,20,9,11,28,14,18,11,14,7,15,13,5,14,20,5,21,8,11,11,14,8,25,22,7,5,20,7,20,8,11,8,11,7,13,11,14,8,11,20,21,8,5;r=20_2,35_1;s=a20,a35;u=0,0,1");
        data.add("1	solid	read	10456	10480	9.4	+	.	aID=266_752_1391;at=F3;b=TAACCCTAACCCTCGCGGTACCCTC;g=T1303013131001013013023010022333003100220000000202;mq=7;o=15;q=13,11,20,18,15,5,25,16,16,31,8,7,8,14,6,26,11,5,5,11,5,10,5,5,8,18,15,8,24,7,7,19,7,26,10,19,21,5,8,8,8,21,8,18,7,20,19,14,9,14;r=20_0,34_1;s=a20,a34;u=0,0,1");
        data.add("1	solid	read	13290	13336	7.9	-	.	");
        data.add("1	solid	read	13301	13342	4.9	+	.	aID=1986_1440_4;at=F3;b=ACGCTGTTGGCCTGGATCTGAGCCCTGGgtGAGGTCAAAGCC;g=A1333110103021023221223202100112201213023020022210;mq=21;o=0;q=13,2,15,5,3,11,13,4,9,7,19,12,2,7,3,2,11,11,3,4,19,8,6,2,11,9,2,4,2,2,4,4,4,2,9,16,4,2,2,19,5,2,4,2,2,2,13,17,11,6;r=5_2,24_0,29_1,31_0,38_0;rb=29_T,30_G;s=a5,a24,y29,y30,y31,a38;u=0,0,0,0,0,1,0,1");
        data.add("1	solid	read	14933	14973	9.5	+	.	aID=2140_530_759;at=F3;b=GTGCTGGCCCAGGGCGGGCAGCGGCCCTGCCTCCTACCCTT;g=G1132103001200330031233230022302202312020222222202;mq=54;o=0;q=27,20,20,17,21,22,13,15,6,22,20,19,3,9,25,23,14,17,2,25,24,7,2,3,25,26,5,18,8,22,25,21,20,14,5,26,5,5,23,20,23,19,19,13,5,13,20,16,8,5;r=24_0,29_1,38_0;s=a24,a29,a38;u=0,0,0,1");
        return data;
    }

}
