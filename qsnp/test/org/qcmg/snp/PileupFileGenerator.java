package org.qcmg.snp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PileupFileGenerator {
	public static final char NL = '\n';
	
	public static void createPileupFile(File pileupFile) throws IOException {
		FileWriter writer = new FileWriter(pileupFile);
		try {
			// add data
			for (String s : generatePileupFileData()) {
				writer.write(s + NL);
			}
		} finally {
			writer.close();
		}
	}
	
	public static List<String> generatePileupFileData() {
		List<String> data  = new ArrayList<String>();
		data.add("chr1	16450	G	11	,..,.......	IIIIIIHIIII	9	,........	IIIIIIIII");
		data.add("chr1	16451	A	12	,..,.......^1.	IIIII7=IIIII	9	,........	IHIIIIIII");
		data.add("chr1	16452	A	12	,..,........	IIII=HIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16453	A	12	,..,........	IIIIGIIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16454	A	12	,$..,........	IIIIIIIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16455	C	11	..,........	IIIIIIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16456	C	11	..,........	IIII:IIIIIH	9	,........	IIIIIIIII");
		data.add("chr1	16457	A	11	..,........	III<AIIIIII	9	,........	I;IIECIII");
		data.add("chr1	16458	C	11	..,........	III7IIIIIII	9	,........	IIIIIAIII");
		data.add("chr1	16459	T	11	..,........	III9IIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16460	A	11	..,........	IIICFIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16461	T	11	..,........	III<CIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16462	T	11	..,........	III8IIIIII@	9	,........	IIIIIIIII");
		data.add("chr1	16463	T	11	..,........	IIIIIIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16464	T	11	..,....AAa.	IIIIIIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16465	A	11	..,........	IIIIEIIIIIG	9	,........	IIIIIIIII");
		data.add("chr1	16466	T	11	..,........	IIIC%IIIII+	9	,........	I>IIIIIII");
		data.add("chr1	16467	G	11	..,........	III0%IIIII+	9	,........	I3IFI?IIH");
		data.add("chr1	16468	A	11	..,........	III/IIIIIIC	9	,........	I;IEIIIIC");
		data.add("chr1	16469	A	11	..,........	III3IIIIGIG	9	,........	IIIIIIIII");
		data.add("chr1	16470	C	11	..,........	IIIFGIIIEI>	9	,........	IEIIIIIII");
		data.add("chr1	16471	C	11	..,........	IIII5IIIII/	9	,........	I9IIIIIII");
		data.add("chr1	16472	A	11	..,........	IEI5HIIIII>	9	,........	IIIIIIIII");
		data.add("chr1	16473	A	11	..,..$......	I;I4<IIIIII	9	,........	IIIIII@I=");
		data.add("chr1	16474	G	10	..,..$.....	I3ICAIIIII	9	,........	IIIIIIHII");
		data.add("chr1	16475	T	9	.$.,......	@@IIIIIII	9	,........	IIIIIIIII");
		data.add("chr1	16476	A	8	.,......	II<IIIIE	9	,$........	I1IIIIIII");
		data.add("chr1	16477	G	8	.,......	II5IHII<	8	........	-I>IGIII");
		data.add("chr1	16478	A	8	.,......	IIIIIIII	8	........	FIIIIIIH");
		data.add("chr1	16479	A	8	.$,$......	?IGIIIII	8	........	IIIIIIIE");
		data.add("chr1	16480	C	6	.$.....	4I(IIF	8	........	+II@CI%C");
		
		// 20 +
		data.add("chr1	62920	C	31	...................,..........^\".	HIDIIIIIIIIIIIII:IIIIHIIIIIII<I	29	...........................^\",^!,	1IIIIIIIIIII?IIIIIIIIIIGIIG)7");
		data.add("chr1	62921	T	31	...................,...........	IIDIIIBII(IFIIFI?DIII@IIIIIIIDI	30	...........................,,^!.	DIIIIIIIDAIIFIIIIIIIIIIIHII7AI");
		data.add("chr1	62922	T	31	...................,...........	IIGIIIIII(>>II)II1IIIIIIIIIII*?	30	...........................,,.	IIIIIIIIB>IIIDIIIIIIIIII;II?FI");
		data.add("chr1	62923	C	33	...................,...........^H.^!.	IIIIIII9IIICII)II>III%IIIIIII*III	31	...........................,,.^H.	IIIIIIIIICIIIGIIIIIIIIIIIIIIIII");
		data.add("chr1	62924	A	36	...................,.............^!.^B.^:.	IIIIIIIEIIIIIIIIIIIII%IIIIIIIIIIIIII	33	...........................,,..^!.^!.	IIIIIIIII8IIIIIIIIIIIIII+IIIIIIII");
		data.add("chr1	62925	G	38	...................,................^!.^!.	IIIIIIIIIIIIIIIIIIIIIFIIIIIII4IIIIIIII	33	...........................,,....	IIIIIIIIIBIIIIIIIIIIIIII+IIIDIIII");
		data.add("chr1	62926	A	38	...................,..................	IIIIII0DIIII0IIIIEIIIBIIIIIII.'IIIIIII	33	....+2CG.....G.........ggG.....,,.g..	IIIIIIIIICIII1IIIIIIIIICGIIGIIIII");
		data.add("chr1	62927	G	38	...................,..................	IIIIII09I=;2-I&II?IIIIIHBB7@IC'IIIIIII	33	...........................,,....	IIIIIIII0EIII1IIIIIIIIIFIII%IIIII");
		data.add("chr1	62928	T	38	...................,..................	CIIIIII<II%.1I&I>CIIIIII%=/:IA?IIIIIII	33	...........................,,....	IIIIIIII-IIII'IIIIIIIIIIII(%II?II");
		data.add("chr1	62929	C	38	........$...........,..................	IIIIIII(II&GAIAI9IIIIIII%C@II,EIIIIIII	33	...........................,,....	BIIIIIIIH;III'IIIIIIIIIIII(IIIDII");
		data.add("chr1	62930	T	37	..................,..................	IIIIIIIIIIIEIIIIIIIIIIIIIDII5IIIIIIII	33	.........$..................,,....	IIIII+II@CIIGIIIIIIIIIIIIII@IIIII");
		data.add("chr1	62931	T	37	.$.$.$.$..............,..................	<2>5II2IIII%IIIIIIIIIIIIEFII@IIIIIIII	32	.$........$.................,,....	?IIII+II<IIG=IIIIIIIIIIIIIHIIIII");
		data.add("chr1	62932	C	33	......$........,..................	IIHII0I%IIIIIIIIIII==DIII<IIIIIII	30	.$.......................,,....	?IIIIIIIIIIIIIIIIIIIGIIIIIIIII");
		data.add("chr1	62933	C	32	.............,..................	IIIIAIAIIIIIIIIIII<I5IIB>IIIIIII	29	.$.$.....................,,....	B8IIIIIII%IIIIIIIIIGIIIIIIIII");
		data.add("chr1	62934	C	32	.....$........,..................	IIII&IAIIIIIIIIBIIIIA<I2IIIIIIII	27	.....................,,....	IIIIIII%IIIIIIIIII>IC&IIIII");
		data.add("chr1	62935	T	31	.$.$...$.......,..................	ABIIAIIIIIIIII8IIIIFEI0IIIIIIII	27	.....................,,....	I)IIIIIIIIIIIIIIII@II&IIIII");
		data.add("chr1	62936	T	28	.........,..................	IIIIIIIIIII<IIIIIII5IIIIIIII	27	.....................,,....	I)IIIIIIIIIIIIIIIIIIIIIIIII");
		data.add("chr1	62937	C	28	.........,..................	IIII6IIBIII@IIIEIEI'?IIIIIII	27	.....................,,....	IIIIIIIIIIIIIIIIIIIIIIIIIII");
		data.add("chr1	62938	T	28	.........,..................	IIII'IGCIIIIIIH=:?I';IIIIIII	27	.....................,,....	IIIIIIIFIIIIIIIIII@I-IIIIII");
		data.add("chr1	62939	A	28	...$......,..................	II6IEIHIIIIIII*195IIFIIIIIII	27	.$.$......$.............,,....	@?IIIII:IIIIIIIIII?I-IIIIII");
		data.add("chr1	62940	T	27	........,..................	IIIIIIIIIIIII*?I:IHIIIIIIII	24	..................,,....	IIIIIIIIIIIIIIIII>AIIIII");
		
		// 50 +
		data.add("chr1	69440	T	63	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,^!.^\",^!,^!,	IIIIIIIIIIII@IFIIIIII&HIIIIIIIIIIIIIIIIIIIIIIIII&IIIIIIIIIII5<=	" +
				"62	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,^!,^!,	IIIIIHIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIEIHIICIII?IIB+");
		data.add("chr1	69441	A	66	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,^T.^!.^!,	IIIIIFIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIIIIIIII*IIIIIIIIIBI:IIII%	" +
				"66	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,^!.^!,^!,^!,	IIIIIIIIIIIIIIIIIFIIIIIIIDIIIIIIIIIIIIIIIIIIIIIIIDEIIIIII:AII8I<>/");
		data.add("chr1	69442	G	68	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,^!.^E,	IIIII8IIIIIIIIIIIIIHHI=IIIIIIIIIIIIIIIIIIIIIIIII(IIIGIIIIIEI4I6II@I9	" +
				"67	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,^!,	IIIIIIIIIIIIIIIIIEIIIIHIICIIIIIIIIIIIIIIIIIIIIIII?IIIIIIII@IIIIIIII");
		data.add("chr1	69443	C	71	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,^!.^!.^T.	IIIII<IIIIHIIIIIIIIIIIGIIIIIIIIIIIIIIIIIIIIIIIII.IIIIIIIIIIICIFIIIIIIII	" +
				"70	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,^!.^T.^!.	IIIIIIIIIIBIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
		data.add("chr1	69444	A	73	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,...^T.^!,	IIIIICIIII1IIIIIIIIIGI<DFIIIHIIIIIIIIICII(IIIIII;I'GIIIIIIIIIIHIIGIGDIHI-	" +
				"73	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,...^1.^!.^!,	IIIIIIIIIIBIIIIIIIIIG=IIIBIIHIIIIIIIIIII%IIII!FIIIIIIIIHIIIIIIIIIIIIEIII&");
		data.add("chr1	69445	A	78	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,^!.^!.^!.^!,^!,	IIIIIIIIIIIIIIIIIIIIIICIIIIIGIIIIIIFII?II(IIIIIG;I'@IIICIIII8I2IHAI)?I5IDIII15	" +
				"78	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,^!.^!.^!,^I,^!,	IIIIIIIIIIIIIIIIIIIIHHIIICIIFIIIIIIIIIII%IIII!EIII=IIIIII<E?A9I5HI;ADIII4II7?:");
		data.add("chr1	69446	T	79	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,^!.	IIIIIIIIIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIIIIII+III)IIIIIII-IDIIII)IIIIGIII=II	" +
				"79	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,^!,	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFIIIIIII;ICIDIHIIAIII&I3IIIID3");
		data.add("chr1	69447	A	83	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.^!,^I,^!,^.,	IIICIAIIIIIIIIIIIIIAI19IIIIIIIIIIIIIIIIIIIIIIIII,III)IIIIIII%I2IICI9IIIIEAII1II:2=>	" +
				"81	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,^!,^!,	IIIIIIIIIIIIIIIIIIIIIIEIIEIIIIIIIIIIIIIIIIIIIIIIIEIIIIIII;IIIIIIIIDIII&I7III<AIAB");
		data.add("chr1	69448	T	86	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,^!,^E,^!,	III?I5IIIIIIIIIIGII4I)EIIIIIIIIIIIIIIIIIIIIIIIII/IIIIIIIII?I%H2IIBI?IIIIH@II0III8II<C3	" +
				"85	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,^B.^!.^!.^!,	IGIIIIIIIIIIIIIIIIIII<IIIEIIIIIIIIIIIIIIIIIIIIIII?IFIHIII9IGDIIIII6IIIII7III95DIIIII/");
		data.add("chr1	69449	G	89	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,^!,^!,^!,	IIIII@IIIIII%IDI<II8F.GIIIIIEIIIIIIIIIIIICIIIIII(IIIBIIIII3I@I=II%I%IIII(III9III+FI:C=)@;	" +
				"88	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,^!.^!,^H,	IIIIICIHIIIIIIEII%IIH7BIIIIII<IIIIIIIII=IIIIIIIH%HI?II>EI;I7@9IIIBDIIIII)IIIID8IIIIIII.5");
		data.add("chr1	69450	C	93	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,^!,^E,^E,^B,	IIIIIIIHIIII%IIIIIIIIIIIIIIIHIIIIIIIIIIII?IIIIII(IIIGIIIIICI.I<II%I%IIII(III;III2II<8BBII@I,=	" +
				"92	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,^!.^!.^?,^E,	IIIIIFIFIIIIIIIII%IIIIFIIEIIIIIIIIIIIIIHIIIIIIII%IIIIICII8IGI1IIIBIIIIII)IIIIIIIIIIIII8;II7H");
		data.add("chr1	69451	A	96	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,^M.^?.^!.	IIIIIIIIIIAIIIIIBIIIIIAIIIIIIIIIIIIIIIIIIEIIIIII/IIIIIIIIIII.IIIIIIIIIII:IIICGIIIIIGIII;III:IIII	" +
				"95	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,^!.^!.^!,	IIIIIIIIIIIIIIII@BIIIIIIIIIIIIIIIIIIIIIIIIIIIEIIIIIIIIIIIIIIIFIIIIIIII9I,IIIIIIIIIIIII3IIIIII?B");
		data.add("chr1	69452	A	96	,,,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	IIIIIIIIII;IIIIIFFIIIC<IFIIIIIIIIIIIII:IICIIIIII/IIIIGIIIIIIDIIIIIIFIIIICIII:GIIAII9>CIAI;I?IIII	" +
				"97	,,,,,,,,,...,,,..,,,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,^!.^!.	IIIIIIIIIIEIIIIIIEIIIIIII>II4IIIIIIIIIIIIIIIIEIIIIIIIIIIIIFIIIIIIICIII.I:III-CIIIIIIII1GIIIIIFIII");
		data.add("chr1	69453	G	96	,,$,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	IIIIIIIIIICIIIIIIFIIIIIIFIIIIIIIIIIIIIHIICIIIIIIAIIIIEIIIIIIIIHIIII.IIIIIIII-III*IIG.AII:)II?III	" +
				"97	,$,,,,,,,,...,,$,..,,$,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIIIIIIIIIIIIDIIIIIIIIIIIFIIIIIIIIIIIHIIIIIIII/IIIIIII3III1IIIII%:IIIIIIII?4IIG4IIIHI");
		data.add("chr1	69454	C	95	,$,,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	IIIIIIIIIEIIIIIIIIIII(IHIIIIIIIIIIIIIIII&IIIIIFEIGIIIIIIIIIIIIIIII5IIIIIIII6III+I)>+?IIE%II)III	" +
				"94	,,,,,,,,...,,..,,$,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIIIIIIIIIAIIIIIIDIIAIIIIIIIIIIHIIIIIIIIIII;IIIIIIIEGIIDIAIII+FIIIAIIIIF;IIA;III>I");
		data.add("chr1	69455	C	94	,$,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.a,,..,.,....,...,,.,,,,,,,,,,,aa,...	IIIIIIII:IIIIIDIIIII(IHIIIIIIIIIIIIIIII&IIIIID8IFIIIIIIIII<IIIIII;IIIIIIIIEIII&I)->BCII/IB%III	" +
				"93	,,,,,,,,...,,..,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.aa..aa..,..	IIIIIIIIIIIIIIIIIIIII6IIHIIIDIIAIIIIIIIIIFHIIIIIIIIIII?AIBIIIIIEIIII5III7%FIICIIIIHIII.IIIHFI");
		data.add("chr1	69456	C	93	,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,a,,,,,,,,...	IIIIIIIIIIIIIAIIHI<*IIIIIIIIIIIIIIIIIIIIIIIII4IIIIIIIIIII2IIIIIIIIIIIIIIIIIII7IIBFEIIIFEEEIII	" +
				"93	,$,,,,,,,...,,..,,,,,.....,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIIDIIIIIFIIIFIIIIIIIIIIIIIIIIIIIIIIIIIIIII;IIIIIIIIII4I6III@%IIIIIIIIDIIIIHIIADI");
		data.add("chr1	69457	C	93	,,,.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	IIIIIIIIIIIIIIIIII?*IIIIIIIIIIIIIIIIIIIIIIIII6IIIIIIIIIIIFIIIIFIIIIIIGIIIAIIIIIIII9ID-IDIIIII	" +
				"92	,,,,,,,...,,..,,,,,..$...$,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIFIIIII?IIDHIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII(I=IIIIIIIIIIIIIIIIIIIIIIHI");
		data.add("chr1	69458	T	93	,$,$,$.,,,..,,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIDIIIIIIIIIIIIIIIIIIIIIIIIIIIAIIIIIIIIEII-IIIIIII	" +
				"90	,$,$,,,,,...,,..,,,,,...$,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIIIIIIII;IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII<IAIIIIIIIIIIIIIIIIIIIIIIII");
		data.add("chr1	69459	A	90	.,,,..,$,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	IIIIGIIIIIIIIIIIHIIIII%IIIIIIIIIIII'IIIIIHCIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFI.IIHIIIIICIIII	" +
				"87	,$,,,,...,,..,,,,,..,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIIIIIIIIIGIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII%I3IIIIIIIIIIIIIIIIIIIIEIEI");
		data.add("chr1	69460	C	89	.$,$,$,..,,.,..,,,,..........,,,.........,..,...,.....,.,,,..,.,....,...,,.,,,,,,,,,,,,,,...	AIIIBIIIIIIIIII8IIIII%IIIIIIIIIIII'HIIIIB1IHIIIIIIIIIIIIIIIIIIIIIFIII=III(I.IIGIIIIIIIIII	" +
				"86	,$,,,...,,..,,,,,..,...,,...,,..,.,..,,,,,.,,,.,,,,,.,,,,.....,..,,,,,,...,.,,..,,..,..	IIIIIIIIIIIIIIIIIIIIIGIIAIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII?II%I.IIIIIIIIIIIIIICIIIIIDI%I");

		return data;
	}
}
