package org.qcmg.snp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.Rule;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.RulesUtil;

public class PileupRecordTest {
	
	private final static Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static List<org.qcmg.common.model.Rule> normalRules = new ArrayList<Rule>();
	private static List<Rule> tumourRules = new ArrayList<Rule>();
	private static int[] normalStartPositions;
	private static int[] tumourStartPositions;
	private static int initialTestSumOfCountsLimit;
	private static int minimumBaseQualityScore;
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setup() throws InvalidFileFormatException, IOException, SnpException {
		
		File iniFile = testFolder.newFile("qsnpRules.ini");
		IniFileGenerator.createRulesOnlyIni(iniFile);
//		createIniFile(iniFile, generateIniFileData());
		
		Wini ini = new Wini(iniFile);
//		Wini ini = new Wini(new File("../../../../defaultRules.ini"));
		// create rules based on entries in ini file
		normalRules = IniFileUtil.getRules(ini, "control");
		tumourRules = IniFileUtil.getRules(ini, "test");
		
		int noOfNormalFiles = IniFileUtil.getNumberOfFiles(ini, 'N');
		int noOfTumourFiles = IniFileUtil.getNumberOfFiles(ini, 'T');
		
		normalStartPositions = PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, true);
		tumourStartPositions =PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, false);
		
		initialTestSumOfCountsLimit = IniFileUtil .getLowestRuleValue(ini);
		
		String baseQualString = IniFileUtil.getEntry(ini, "parameters", "minimumBaseQuality");
		if (null != baseQualString)
			minimumBaseQualityScore = Integer.parseInt(baseQualString); 
	}
	
	@Test
	public void testRealLifePileupEntry() throws IOException {
		String pileup = "chr17	7577118	C	208	" +
				"a,$,.$,,,,,,,,,,,,a,a.$.....,....,,,.,,,,,,.,.,,.,,...,..$,.,+1a,,.,,,.,,,,,,,,,,.......,.,.,+1a.,,,..,,,,.,,,,.,,.,....,,..,,,.,,,...,,,.,,,,.,,.,.,,..,.........,,.a,,,.,,,.,.,,...,,,.,.,,.,.,.,.,.,,,.,,,..,,...,.+1G.,.^9.	" +
				"/06,60=444(13.6,731,,81-(=2,4(6.=(6/><55(+73>(8535+674<:-:8+/><;=>8<;>9=255(548221/6,)42;61.;;:;(1860.;4:44+6,0/9553:,2,6+,(6>0,2>>>8<6487=784:,6>8;0,82,45.5::>.4>64,<2=(2:7:=90,<,8<(=:(:>3,797(9<=,1:>>/,<-32	" +
				"184	a,$,aA.*,,..,aaa,A,+1a,.aaa..*a,.,.,.,,,,a.,..,..,.a*.a,.,aa,,.,,,,,.,..,,A..a,..aaA,*.,,aG,.,,.,A,.,Aa,aaA*,*..aaAA.,,.,aa,A,.,AaA,.a,A.,a....,.Aa,.A.,...*,.a.,...a,.a,,A.,a,.Aa,,,.,A,,,^d.	" +
				"0>+6,9(2>693;1=:/-0(5<>12&/45;98./.:,/6>9,</4><,)-5>-631>>9>;;<-,39(1:7(=<<<8:=,/&/69;0<5>96><86/2,8/82&;&(91,797>><<+685=6/51995(>,298>=(>5:7335,8,:<2,;292)7=0,6:,.9,2<;=>:69;/>:77,7:";
		
		assertEquals(false, parsePileupSuccessful(pileup, false));
	}
	
	
	@Test
	public void testRealLifePileupEntry2() throws IOException {
		String pileup = "chr1	62926	A	38	...................,..................	IIIIII0DIIII0IIIIEIIIBIIIIIII.'IIIIIII	33	....+2CG.....G.........ggG.....,,.g..	IIIIIIIIICIII1IIIIIIIIICGIIGIIIII";
		
		assertEquals(true, parsePileupSuccessful(pileup, true));
		assertEquals(false, parsePileupSuccessful(pileup, false));
	}
	
	@Test
	public void testRealLifePileupEntry3() throws IOException {
		String pileup = "chr14	22180819	C	56	.,,,..,,....,..,,.,,........,.....,....,.,,........,.^M.^T.^I.	\"III,AII91IIGIIIIEIIIIIIIIII'IIIIIIIIII)IIIIIIIIIIIIIGDI	55	,,tt,,T.,.T...,T...........T....,T..T.T,t..T......,^PT^M.^-.^F,	IIIIIII%IIBI;IIIIIIIIIIIIIIIBIIIIIIIIIIIIIIIIIIIIIIIII:";
		
		assertEquals(true, parsePileupSuccessful(pileup, false));
	}
	@Test
	public void testRealLifePileupEntry4() throws IOException {
		String pileup = "chr9	15578919	C	110	.$,,$,,,.,.,..,,,,$........$.......,,.,.....,,..,...,,.....,.,,,,......,...,.....,,,,.,..,.,..,,...,..,,,,..,,^T.^B.^T,^M,	&IIIII%I)I7%IIIII@6HGI%FHI%=II%IIGI@'?I6III%IAIIII(BII:\"?IIII'IE/EIII=II1CIF=IIIIIIIIIAIIIFIIAIIIIIIIIII?III5*	91	,.,,,,,.,,GG,..,.....,...N$.G.G..,.,,.G,,.,.,,..........G..,.........,....,.,G.,,..,,,.,,^M.^T.^1.	I-II1II&II7FI%&I%I,BBI9-B!&I1I)%I5IIIIIIIIGIIEII;I&IIIIIIIICEHDIIIII?IIIDIIIIIIIIIIIIII8II)";
		
		// false - fails on base quality check - all on same strand!!!
		assertEquals(false, parsePileupSuccessful(pileup, false));
		
		// switch strand of one of the variants - should be enough to pass the test
		pileup = "chr9	15578919	C	110	.$,,$,,,.,.,..,,,,$........$.......,,.,.....,,..,...,,.....,.,,,,......,...,.....,,,,.,..,.,..,,...,..,,,,..,,^T.^B.^T,^M,	&IIIII%I)I7%IIIII@6HGI%FHI%=II%IIGI@'?I6III%IAIIII(BII:\"?IIII'IE/EIII=II1CIF=IIIIIIIIIAIIIFIIAIIIIIIIIII?III5*	91	,.,,,,,.,,gG,..,.....,...N$.G.G..,.,,.G,,.,.,,..........G..,.........,....,.,G.,,..,,,.,,^M.^T.^1.	I-II1II&II7FI%&I%I,BBI9-B!&I1I)%I5IIIIIIIIGIIEII;I&IIIIIIIICEHDIIIII?IIIDIIIIIIIIIIIIII8II)";
		assertEquals(true, parsePileupSuccessful(pileup, false));
	}
	@Test
	public void testRealLifePileupEntry5() throws IOException {
		String pileup = "chr19	58131572	A	154	.$.$,.,...,..,.....,,,,......,,..,,,....,.,,,...,,..........,.,..,,..,.,,.,....,,,,,..,,,,,,,...,..,,,,.....,,,,.....,.....,.,,,,,,..,,,,...,,,,..,,........	DAIIII\"=IBIF%%DIIIIIIACIICIIIIII2/IIIIIIIIIIIIIIIIIIIIIIIIAIIIIIIII;IIIIIIIIIIIIIIEIIII<IIIIIIIIIIIIIIICIIIIIIIIIIIIIIIIIIIIIII%IIIIII+)IIIHIIIIIIIIIIIIII	91	.............,..G..,G.,.........,.G,,...,.G,....,......,,..G.,t...........,,,.......,,G.GG,	GIEI9AI&9.&*IIIBIH@IIIIDIIIIIIIIIII3IIIIIIIIIIIIIIIIIBIIIIIIHI%IIIIIIIIIIIIIIIIIIIIIIIIIII@";
		// false - fails on base quality check - all on same strand!!!
		assertEquals(false, parsePileupSuccessful(pileup, false));
		// switch strand of one of the variants - should be enough to pass the test
		pileup = "chr19	58131572	A	154	.$.$,.,...,..,.....,,,,......,,..,,,....,.,,,...,,..........,.,..,,..,.,,.,....,,,,,..,,,,,,,...,..,,,,.....,,,,.....,.....,.,,,,,,..,,,,...,,,,..,,........	DAIIII\"=IBIF%%DIIIIIIACIICIIIIII2/IIIIIIIIIIIIIIIIIIIIIIIIAIIIIIIII;IIIIIIIIIIIIIIEIIII<IIIIIIIIIIIIIIICIIIIIIIIIIIIIIIIIIIIIII%IIIIII+)IIIHIIIIIIIIIIIIII	91	.............,..G..,G.,.........,.G,,...,.g,....,......,,..G.,t...........,,,.......,,G.GG,	GIEI9AI&9.&*IIIBIH@IIIIDIIIIIIIIIII3IIIIIIIIIIIIIIIIIBIIIIIIHI%IIIIIIIIIIIIIIIIIIIIIIIIIII@";
		assertEquals(true, parsePileupSuccessful(pileup, false));
		
		// new record
		// false - fails on base quality check - all on same strand!!!
		pileup = "chr1	7868961	T	71	.$.$,.$.$..$.$............$.$................,........,....,.,,,.........,.^B.^,.^T,^P,	*8I2*01I>I86IB6IH7B??III<\"IEIIIIICIIGGIIIIII%GIIIII9IE9IIIIIIIIIIIIII@?	56	.....$..CC....,$...$C...,.........,C...C.....,,...,.......^H.	II:I>&CI>4II5II)6I\"I)AI+IDIIIII@IIIIIIIIIIC=GIIIIIIIIIII";
		assertEquals(false, parsePileupSuccessful(pileup, false));
		pileup = "chr1	7868961	T	71	.$.$,.$.$..$.$............$.$................,........,....,.,,,.........,.^B.^,.^T,^P,	*8I2*01I>I86IB6IH7B??III<\"IEIIIIICIIGGIIIIII%GIIIII9IE9IIIIIIIIIIIIII@?	56	.....$..CC....,$...$C...,.........,C...c.....,,...,.......^H.	II:I>&CI>4II5II)6I\"I)AI+IDIIIII@IIIIIIIIIIC=GIIIIIIIIIII";
		assertEquals(true, parsePileupSuccessful(pileup, false));
	}
	
	@Test
	public void testRealLifePileupEntry6() throws IOException {
		String pileup = "chr1	10108	C	747	.$..,....T................T....,t,........A.....Att,,.G.....T..A.t,,TT....T...,,,,,..*.........T..,,,,,,,,,,,..T..T.......,,,,,,........T.T.....T...,,,,t,,,.T....A.T........T.....,,,,,,,,........-1A...T.T..tt,,,........T...T,,,,,t,,,T....T..A..........,,,,,,,,,..........A......t,,,,,,,,A.....*TT..,,,,,,,..............AT...,,,,,..T...........t,,,,.........,,,,t......,,,,,............,,,,,,,,,,,,.........T........,,,,t,,,t,....-1A................,t,,,t,......-1A....,,t.................,,,,,,............,,......,,,,,,,,...........,,,,,........,,.......,,,......a,,,.+2CG....,,...,,,,,*........,,,,,,.........*,,,,,.....*,,,,....+2CA...,,,,,,...........,,,..,,,,........,,,,,....*.....,,,,,,*..,..T..,..*,,.+2CT.....,,*.*.,..,...,....+2TT,,.,..,...,,,,.,.,,*..+1T...,...,..t	9#BFBA###BA#B######A??2#######FFF2##############DDFFB#<##B?3##(<DFF#:#D#?AA##FFDFD#2#B#88#ABB2(##FDDFDDDDFFDD#:?<:A?2#1B2HHHFFFD?A##(8B###B#?A#<D#BDHDGHFCHB@?D####D##?##D?#@8#8##HHHHFGDAB(A#D#1B###:?###HDHHH?BBB8#AD#82B#HHHHFHFFD(ABB?3####9@D#@B?BDGHFFHGHHI#(DA?B92<D####D#AGHGFGFFIE#DA#((#3#9DHCIFAFI#BD<#?##B9B#D###A#(H?FF@#DADB2#BBD#B?BHEGAED#A#5##9BEEAFHBB<<?#GAFCF#B@<?B#A59A#FFF@FHGCFBCI#(D(BD#(#C?B<####@GHIFHIEFIFA<?AB#DD#D#?(5AD##?B<AEIFIGD#BB###BD#IFG?#AAA#D#ADD#BB;A2FFEAAABBBDDA(#??5#?C;#=?DBFGE@IFFE(D??DD#CBBBCCGF:A5?A#2BDEEB?DD##?FGHB;DBC;GFHGBB;A=EI9@C0HGGH>BFD#B#F2H<FHGE=#F;C#F?#<IHH?FF?BDBJ(DGG(;#HFB#D?GHHF=F?CC#CH(=BFEHFHHGGGHEHFEFFGHGDIE=DDEJE#IGE:G@JEG)HHFIDHIHIIA;C5CGGIEF@EIBHJEIJ;IIJ?EIGJ5?H#IG<JJI;#=DJAI=?EJEJIH#HHD9FF#	861	.$.$.$..$.................................,t,,,,T......A........................,,,t,t,,........T......$,,,,.....T....tt,,tt,.+2CA..A...,,,,,,..A.....,,,t,,,,,..T..........,,t,t,,.....A...T..........t,+1t,,,t,.A......T.........T.,t,.A...........T..t,,,,,,.........t,,t,,,,......+2CA.....T....t,,,,,,,,..........,,,tt,..................,,,,,,a............A....,t,................t,,,tt,,........,,,,,,t...........,,,,,,,,......T...........t,,,,,,,,,..............,,,,t,,,...................,,,t,,.........+3CTA.....,,,,,,........,,,,,,,,,,.....-1A....,,,,,,,,...........,,,.....................,,,,,,,..-1A........,,,,,......,,.........,,,...,,,,,,,,,........,,,,,...............,,,,.......,,,.....+2CA....,,..,,,.....,,,,..........,,,,,,....,,..........,,,,.,,.,,..T,A.,,ttT.ttT,,,,t.+1T,..T..*T..,+1t,+1t,+1t,,.....,+1t,+1t,+1t*,+1t..****.T........*,T...,+1t.,+1t..+1T.,,.,,,,,,.......T..*.,..,,..,...,.,,,.+1T,.......+1T.+1T.tt.T^>T^0T^F,	##?#B?<#B###############<#######?#A###FFFFFF#?######?#D####<#####?######@9##FDFFFDFF#####88D##8D###DDFA@#B#?##D#?FFFDFFD?###BD#FDD@FFB####B##FHFHHFHHF#2(##B#2D2#BDFHFHFHHA##B###A###(A8#AA(##HGHHHFH#(B2??#A4BA<BBDA(BA(HHH###B########?5<#BDFHHFD##9D?AA##HHHHFHIH##<9D#D1###:B<D2IEFFHHGIFBD#DBB###AHHGIEH#<BABAB####AA##5ABEAF<IHIDD@5#?B?##29(?A##HH<#<##D9A9?AB@BDADHGGIGHGHAD?B@AAAGHCHABHBAAA9BDBB#AEDGGIGAG?DBA<##D#9D##B52A#?:8IEGFBHH#D?#<9BA999?5#FH<AIGFG=#2?;?9=?D?A##D#B#BDEBGFH==;B##BB##D;9BFFGHGAAD##B;#BGFEIF?EHEEDB#D?#BB#EE?GGGBGABA;=B??DD;C?CD;DBBC#?BC9;==;?;#6B;HCGFGEDDA#9?#DD#AG0BABCDB6#=@>9D(#??#.DG)GD2#GGGAIA<BI#A#B##DAG80IE;2CAFFBFB?D?DBDFGHADF?#BEBG:H;D???;B#BAHFFGF@DA2FB@F@BDHG=EECHHCFH<FH@FAGC8FHAGIAJE#IEGGGGE?DHBBGG@B#HFBGE#GH=4BFCCFFFBIGI8@89JEEF1CJCJJIHIG=FIJ#))HFB(IGJ8G)J7C>G;GEI@CJDEBI?E=B5;JFGJFGIJJI.G?JJA>GI5JII>H?BBJ?JJJHIJJHH8#CCCC#";
		assertEquals(false, parsePileupSuccessful(pileup, false));
	}
	@Test
	public void testRealLifePileupEntry7() throws IOException {
		String pileup = "chr1	862389	A	150	GG**GG**GGg******Gg*G*g***GG**G**G**gg****g**Gg*gG*G**g*gGgggG**ggG****G*g***G***Gg****ggggGG*gG**G*gggG*G**gg****g**c*g*GGGgg*g*G****ggggGGgGGGGggg^]G^]G	DDFFDD7F<>HHGDD>B@JDDDJJIIBDIGDHHBBJJJJJDDIDDBEJHDBDDIJDIDJJ??GJJCBDD>DFJJHHEJJHHGHEEBJFFDAJEEDIDDJ:DBDJDEIJ8D?BJGBJB>#DDGJJDBD8DHBD<?DDBDGHBC7CDDDDCC	238	C$C$C$****************************************************************************************************************************************************************************************t********************************gGGGGGGGggGGgggg^]G^]g	B<9D<BFD@@#FDDFFFDHHDDHHDDDHHBDJJID<DJDDJDBDDDB>J@DDDDFBD@DDDJDBGDD3CJDDDDDDDJJBDBCJIDJEJDDDDIBDJJIIJJDD?;DDIIJDDJIHEIJHHJJIJJJ@EFHHHHJIEJJIJJIFJJEDEJFJIJCC;JJ@E:BDJF:JJJDJJJJBDDIBJJJBBJD#BDDIDJJIDII8IIBDJDDJDDDJJHDBDDBDDHDCHHHHDDCCBDBDCD";
		assertEquals(true, parsePileupSuccessful(pileup, false));
		
	}
	@Test
	public void testRealLifePileupEntry8() throws IOException {
		String pileup = "chr1	22333212	T	48	............**********************************^]C^]C	DDDACCCDDDDCFIIJJJHJJHJJJJJJJIJJHHHHFFFFFFFFFFCC	55	.................*************.*********************C^]C^]C	AD@DCCCDCDCDDDCDDHHIJJIIJIIJJJ4JJIIJJJJJJIIJHHHHFFFFCC@";
		assertEquals(true, parsePileupSuccessful(pileup, false));
		
	}
	
	@Test
	public void testRealLifePileupEntry9() throws IOException {
		String pileup = "chr21	40566639	G	67	.$.$............A..$..a..,.......,..A..A..,,,.,,,..,..,,,..,..,,,..,,^T.	6'5/IC+%+(''I)15%/'I1I;'+:0%AFII&1+'53'III/III5E6II)IIIIEIIIIIII2II	28	.$.,a.a.....,..,..,,...,..,.^E.	II5EI6)('?FII'19)IIAIIIIIIII";
		assertEquals(true, parsePileupSuccessful(pileup, false));
	}
	
	@Test
	public void testRealLifePileupEntry10() throws IOException {
		String pileup = "chr1	809681	G	80	,,,,,,.,,,A,,,..,,,...,,....,,,.,,.,a,,,.a...,,a,,...,,,.,,.,,,.,,,,....,,,,^M.^J.^J.^M.	IIIIII2IIIIIII0GIIIIGIII-IIEIIF8II1IIIHIIII>II;IIIIIIIIIIIII8IIIIIIIIIIIHIIIIIII	56	,,,,...,.,,,,,,.,....,.,..aa,..,a,..,,,,,,,,,,..,.,,,,^!,^T,	IIII@IBIGIIIII>IIHHIIIIIIIII>IIIIIIIIII%I=II,IIFIIGDII@;";
		System.out.println("10:");
		assertEquals(true, parsePileupSuccessful(pileup, false));
	}
	
	private static boolean parsePileupSuccessful(String record, boolean ignoreIndels) throws IOException {
		String[] params = tabbedPattern.split(record, -1);

		// get coverage for both normal and tumour
		int normalCoverage = PileupUtils.getCoverageCount(params, normalStartPositions);
		int tumourCoverage = PileupUtils.getCoverageCount(params, tumourStartPositions);
		
		if (normalCoverage + tumourCoverage < initialTestSumOfCountsLimit) {
			System.out.println("Failed the initialTestSumOfCounts check!");
			return false;
		}

		String normalBases = PileupUtils.getBases(params, normalStartPositions);
		String tumourBases = PileupUtils.getBases(params, tumourStartPositions);
		
		if ( ! ignoreIndels) {
			// means there is an indel at this position - ignore
			if (PileupUtils.doesPileupContainIndel(normalBases)) {
				System.out.println("Failed the indel check!");
				return false;
			}
//			if (normalBases.contains("+") || normalBases.contains("-")) {
//			System.out.println("Failed the indel check!");
//			return false;
//			}
			if (PileupUtils.doesPileupContainIndel(tumourBases)) {
				System.out.println("Failed the indel check!");
				return false;
			}
//			if (tumourBases.contains("+") || tumourBases.contains("-")) {
//				System.out.println("Failed the indel check!");
//				return false;
//			}
		}
		
		String normalBaseQualities = PileupUtils.getQualities(params, normalStartPositions);
		String tumourBaseQualities = PileupUtils.getQualities(params, tumourStartPositions);

		// get rule for normal and tumour
		Rule normalRule = RulesUtil.getRule(normalRules, normalCoverage);
		Rule tumourRule = RulesUtil.getRule(tumourRules, tumourCoverage);

		// get bases as PileupElement collections
		List<PileupElement> normalBaseCounts = PileupElementUtil.getPileupCounts(normalBases, normalBaseQualities);
		List<PileupElement> tumourBaseCounts = PileupElementUtil.getPileupCounts(tumourBases, tumourBaseQualities);

		// get variant count for both
		int normalVariantCount = PileupElementUtil.getLargestVariantCount(normalBaseCounts);
		int tumourVariantCount = PileupElementUtil.getLargestVariantCount(tumourBaseCounts);
		
		boolean normalFirstPass = false;
		boolean normalSecondPass = false;
		boolean tumourFirstPass = false;
		boolean tumourSecondPass = false;
		
		/*
		 * Need to know what passed so that the appropriate rule can be used 
		 */
		
		normalFirstPass = Pipeline.isPileupRecordAKeeperFirstPass(normalVariantCount, normalCoverage, normalRule, normalBaseCounts, minimumBaseQualityScore);
		if ( ! normalFirstPass) {
			normalSecondPass = Pipeline.isPileupRecordAKeeperSecondPass(normalVariantCount, normalCoverage, normalRule, normalBaseCounts, minimumBaseQualityScore);
			
			if (! normalSecondPass) {
				tumourFirstPass =  Pipeline.isPileupRecordAKeeperFirstPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, minimumBaseQualityScore);
				if (! tumourFirstPass) {
					tumourSecondPass = Pipeline.isPileupRecordAKeeperSecondPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, minimumBaseQualityScore);
				}
			}
		}
		
		if (normalFirstPass || normalSecondPass || tumourFirstPass || tumourSecondPass) {
			
			
			// if normal passed, need to test tumour to see what rule to use
			if (normalFirstPass || normalSecondPass) {
				tumourFirstPass = Pipeline.isPileupRecordAKeeperFirstPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, minimumBaseQualityScore);
				
				if ( ! tumourFirstPass) {
					tumourSecondPass = Pipeline.isPileupRecordAKeeperSecondPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, minimumBaseQualityScore);
				}
			}

		// only keep record if it has enough variants
//		if (isPileupRecordAKeeper(normalVariantCount, normalCoverage, normalRule, normalBaseCounts, minimumBaseQualityScore) 
//				|| isPileupRecordAKeeper(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, minimumBaseQualityScore)) {
			
			List<PileupElement> tumourBaseCountsPassRule = PileupElementUtil
			.getPileupCountsThatPassRule(tumourBaseCounts, tumourRule, tumourSecondPass, minimumBaseQualityScore);
			System.out.println("Tumour Genotype: " + PileupElementUtil.getGenotype(tumourBaseCountsPassRule, params[2].charAt(0)));
			List<PileupElement> normalBaseCountsPassRule = PileupElementUtil
			.getPileupCountsThatPassRule(normalBaseCounts, normalRule, normalSecondPass, minimumBaseQualityScore);
			System.out.println("Normal Genotype: " + PileupElementUtil.getGenotype(normalBaseCountsPassRule, params[2].charAt(0)));
			System.out.println("We have a keeper!!!");
			return true;
		} else {
			System.out.println("Did not pass the isPileupRecordAKeeper test");
			return false;
		}
	}
	
//	private static boolean isPileupRecordAKeeper(int variantCount, int coverage, Rule rule, List<PileupElement> baseCounts, double percentage) {
//		// first check to see if it passes the rule
//		if (PileupElementUtil.passesCountCheck(variantCount, coverage, rule) && PileupElementUtil.passesWeightedVotingCheck(baseCounts, percentage))
//			return true;
//		else return PileupElementUtil.passesCountCheck(variantCount, coverage, rule, true) 
//				&& PileupElementUtil.getLargestVariant(baseCounts).isFoundOnBothStrands() 
//				&& PileupElementUtil.passesWeightedVotingCheck(baseCounts, percentage, true);
//	}

	
//	private List<String> generateIniFileData() {
//		List<String> data  = new ArrayList<String>();
//		data.add("[normalRule1]");
//		data.add("min = 0");
//		data.add("max = 20");
//		data.add("value = 3");
//		data.add("[normalRule2]");
//		data.add("min = 21");
//		data.add("max = 50");
//		data.add("value = 4");
//		data.add("[normalRule3]");
//		data.add("min = 51");
//		data.add("max =");
//		data.add("value = 10");
//		data.add("[tumourRule1]");
//		data.add("min = 0");
//		data.add("max = 20");
//		data.add("value = 3");
//		data.add("[tumourRule2]");
//		data.add("min = 21");
//		data.add("max = 50");
//		data.add("value = 4");
//		data.add("[tumourRule3]");
//		data.add("min = 51");
//		data.add("max=");
//		data.add("value = 5");
//		data.add("[minimumBaseQuality]");
//		data.add("value = 10");
//		data.add("[pileupFormat]");
//		data.add("order = NT");
//		data.add("[wiggleCoverageValue]");
//		data.add("normal = 20");
//		data.add("tumour = 20");
//		return data;
//	}
	
//	private static void createIniFile(File iniFile, List<String> data) {
//		PrintWriter out = null;;
//		try {
//			out = new PrintWriter(new BufferedWriter(new FileWriter(iniFile)));
//
//			for (String line : data) {
//				out.println(line);
//			}
//		} catch (IOException e) {
//			System.err.println("IOException caught whilst writing out inin file");
//			e.printStackTrace();
//		} finally {
//			if (null != out) out.close();
//		}
//	}
	
}
