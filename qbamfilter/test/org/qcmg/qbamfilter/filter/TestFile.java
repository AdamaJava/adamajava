/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qcmg.qbamfilter.filter;

import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.*;

public class TestFile {
    //static final String OUTPUT_FILE_NAME = "output.bam";
    public static final String INPUT_FILE_NAME = "input.sam";

    /**
     * A testing data with total five reads
     * below flag used for perspective reads, 
     * flag -> base2
     * 115  -> 111,0011
     *  83  -> 101,0011
     * 163  -> 1010,0011
     * 1040 -> 100,0001,0000
     * 177  -> 1011,0001
     */
    public static void CreateBAM(String fileName){

          List<String> data = new ArrayList<String>();
          data.addAll(CreateSamHeader());
          data.addAll(CreateSamBody());

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

    private static List<String> CreateSamHeader(){
        List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qbamfilter::Test	VN:0.2pre");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile");
        return data;
    }

    private static List<String> CreateSamBody(){
        List<String> data = new ArrayList<String>();
        data.add("243_146_202	115	chr1	10075	6	13H37M	chr1	10167	142	" +
                        "ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
                        "RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'	ZM:i:1");
        data.add("642_1887_1862	83	chr1	10167	1	15H15M2D20M	=	10176	59	" +
                        "CCTAACNCTAACCTAACCCTAACCCTAACCCTAAC	.(01(\"!\"&####07=?$$246/##<>,($3HC3+	RG:Z:1959T	" +
                        "CS:Z:T11032031032301032201032311322310320133320110020210	CQ:Z:#)+90$*(%:##').',$,4*.####$#*##&,%$+$,&&)##$#'#$$)");
        data.add("970_1290_1068	163	chr1	10176	255	42M8S	=	10167	59	" +
                        "AACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAACCTAAC	I&&HII%%IIII4CII=4?IIF0B((!!7F@+129G))I>.6I&&HII%%	RG:Z:1959T	" +
                        "CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=	ZM:i:2");
        data.add("681_1482_392	1040	chr1	10236	20	10H10M200N30M	*	0	0	" +
                        "AACCCTAACCCTAAACCCTAAACCCTAACCCTAACCCTAA	IIIIIIIIEBIIIIFFIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:1959T	" +
                        "CS:Z:T00320010320010320010032001003200103200100320000320	CQ:Z::<=>:<==8;<;<==9=9?;5>8:<+<;795.89>2;;8<:.78<)1=5;	ZM:Z:2");
        data.add("1997_1173_1256	177	chr11	10242	100	22H10M8I10M	chr1	10236	0	" +
                        "AACCCTAAACAACCCTAACCTAAACCCT	IIII27IICHIIIIHIIIHIIIII$$II	RG:Z:1959T	" +
                        "CS:Z:G10300010320010032001003000100320000032000020001220	CQ:Z:5?8$2;>;:458=27597:/5;7:2973:3/9;18;6/:5+4,/85-,'(");
        return data;

    }
    
    
    static void CreateBAM_MD(String fileName){
              List<String> data = new ArrayList<String>();
          data.addAll(CreateSamHeader());
          data.addAll(CreateSamBody_MD());

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
    private static List<String> CreateSamBody_MD(){
        List<String> data = new ArrayList<String>();
        data.add("2257_1059_987	131	chr1	10392	0	48M2H	=	11897	1555	CCTAACCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	IIIIIIIIIIIIIIIHI$$IIFI;EII@F))I>9G9?I?AI))IE78G	NH:i:2	RG:Z:1959T	CS:Z:G30230100023010023000023010023000023010023000023003	CQ:Z:7=<?<287476;878274$5;073)=<107)<:%53'95+78):51'26/	SM:i:1	CM:i:3	NM:i:0	MD:Z:48");
        data.add("620_1272_1494	81	chr1	11549	2	1H49M	chr1	51432809	0	AATATGTTTAATTTGTGAACTGATTACCATCAGAATTGTACTGTTCTGT	@8>ICAHII:6IBAC<EII@EIID-2EFIIIIII?-FIIIIIIIIEIII	NH:i:2	RG:Z:1959T	CS:Z:T31122011213110302212310100321210211100303001133303	CQ:Z::>87/;=A=/A>=*$<9>19?9.82-839-4:51,8*91&5563/58'2/	SM:i:3	CM:i:1	NM:i:0");
        data.add("1191_1384_1167	131	chr1	10394	0	50M	=	11493	1149	TAACCCTTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	HIIIFGIIIIIIIIH>HIAEIACIIII))IB7E,,I&&A;@I<4F//I>,	NH:i:2	RG:Z:1959T	CS:Z:G13010020301002301002301002300002300000301002300000	CQ:Z:36<;5269><:=9:63,=9)=:(<>;<6);7,,:,>;&,6&;4),;/<3,	SM:i:0	CM:i:6	NM:i:1	MD:Z:6C43");
        data.add("1068_1029_1827	131	chr1	10421	0	48M2H	=	11564	1193	ACCCTAACCCTAACCCTAAAACCTAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIII?<<;I>HIIIFCI&&IIII,,II=B66IA4	NH:i:2	RG:Z:1959T	CS:Z:G21002301002301002300010230100230000230000230000200	CQ:Z:?>@>=AA9@?@<<4?@?7A?A<;6)@7@;,8;&@585:,><5):6?<&/7	SM:i:0	CM:i:5	NM:i:2	MD:Z:19C0C27");
        data.add("1963_1808_17	65	chr1	10421	0	13M2D37M	=	11564	-1396	ACCCTAACCCTAACCCTAAAACCTAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIII?<<;I>HIIIFCI&&IIII,,II=B66IA4	NH:i:2	RG:Z:1959T	CS:Z:G21002301002301002300010230100230000230000230000200	CQ:Z:?>@>=AA9@?@<<4?@?7A?A<;6)@7@;,8;&@585:,><5):6?<&/7	SM:i:0	CM:i:5	NM:i:2	MD:Z:13^AC37");
        //data.add("1963_1808_17    65      chr1    158055839       79      13M2D37M        =       158054493       -1396   ACTGCACACAGTCACATTCACACACACTGCACACACATTCACACACACTG      IIIIIIIIIIIIIIIIIIIIIIIIIIIIFIIIIIIIII((IIIIIII11;      RG:Z:1959T  NH:i:2  XW:Z:11_15      CS:Z:T31213111112121113021111111121311111113011111111111        CQ:Z::;;88;<<==:87=?<:95?=>==<>A98/>>=;<?>67(=>>>;==<1; NM:i:2  MD:Z:13^AC37");
          data.add("1737_726_489	65	chr1	10421	0	29M1I20M	=	11564	-1396	ACCCTAACCCTAACCCTAAAACCTAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIII?<<;I>HIIIFCI&&IIII,,II=B66IA4	NH:i:2	RG:Z:1959T	CS:Z:G21002301002301002300010230100230000230000230000200	CQ:Z:?>@>=AA9@?@<<4?@?7A?A<;6)@7@;,8;&@585:,><5):6?<&/7	SM:i:0	CM:i:5	NM:i:2	MD:Z:12T0G0A34");
      //data.add("1737_726_489    113     chr11    10595        54      29M1I20M        =       16788        6193    ATCTTTCCTTCTCCGTGACTAGAAGAAAAGGGACTCCTTAATAACAAAGT      ,=6-.,;9.;DE%%%&H&&=AG))I?B##36IDFIIHIIIIIIIIIIIII      RG:Z:1959T  NH:i:3  XW:Z:17_19      CS:Z:T31200110330302022120021002222232021130222020200223        CQ:Z:<9>A<<?;846<281<=*;1&.#6-38)711-&90&'%332*%5'&)%2, NM:i:3  MD:Z:13T0A34");
        return data;

    }

}
