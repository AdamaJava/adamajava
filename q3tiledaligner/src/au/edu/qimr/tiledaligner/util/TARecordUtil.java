package au.edu.qimr.tiledaligner.util;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPair;
import au.edu.qimr.tiledaligner.model.IntLongPairs;
import au.edu.qimr.tiledaligner.model.TARecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.NumberUtils;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class TARecordUtil {
	
	public static final int TILE_OFFSET = TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET;
	public static final int TILE_LENGTH = TiledAlignerUtil.TILE_LENGTH;
	public static final int REVERSE_COMPLEMENT_BIT = TiledAlignerUtil.REVERSE_COMPLEMENT_BIT;
	public static final int MIN_BLAT_SCORE = TiledAlignerUtil.MINIMUM_BLAT_RECORD_SCORE;
	public static final int MIN_TILE_COUNT = MIN_BLAT_SCORE - TILE_LENGTH;
	public static final int MAX_GAP_FOR_SINGLE_RECORD = 500000;
//	public static final int MAX_GAP_FOR_SINGLE_RECORD = 10000;
	public static final int BUFFER = 10;
	public static final int RANGE_BUFFER = 5;
	public static final int MIN_BLAT_SCORE_MINUS_BUFFER = MIN_BLAT_SCORE - BUFFER;
	public static final int MIN_BLAT_SCORE_MINUS_RANGE_BUFFER = MIN_BLAT_SCORE - RANGE_BUFFER;
	public static final PositionChrPositionMap pcpm = new PositionChrPositionMap();
	
	private static final QLogger logger = QLoggerFactory.getLogger(TARecordUtil.class);
	
	
	/**
	 * What is being examined here is if parts of the sequence are mapping relatively close by (say within 10kb of each section of sequence
	 * that would mean that smithwaterman wouldn't work if the highest start position count was used (as is currently the case).
	 * 
	 *  A great example is here:
	 *  00000003 agaatgtaattatatctagtgctgcagaaagg 00000034
		>>>>>>>> |||||||||||||||||||||||||||||||| >>>>>>>>
		92655209 agaatgtaattatatctagtgctgcagaaagg 92655240
		
		00000035 cctttagaaataagagggccatatgacgtggcaaatct 00000072
		>>>>>>>> |||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92655637 cctttagaaataagagggccatatgacgtggcaaatct 92655674
		
		00000073 aggcttgctgtttgggctctctgaaagtgacgccaaggctgcggtgtcca 00000122
		>>>>>>>> |||||||||||||||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92656070 aggcttgctgtttgggctctctgaaagtgacgccaaggctgcggtgtcca 92656119
		
		00000123 ccaactgccgagcagcgcttctccatggag 00000152
		>>>>>>>> |||||||||||||||||||||||||||||| >>>>>>>>
		92656120 ccaactgccgagcagcgcttctccatggag 92656149
		
		00000153 aaactagaaaaactgcttttggaattatctctacagtgaagaaacctcgg 00000202
		>>>>>>>> |||||||||||||||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92660327 aaactagaaaaactgcttttggaattatctctacagtgaagaaacctcgg 92660376
		
		00000203 ccatcagaaggagatgaagattgtcttccagcttccaagaaagccaagtg 00000252
		>>>>>>>> |||||||||||||||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92660377 ccatcagaaggagatgaagattgtcttccagcttccaagaaagccaagtg 92660426
		
		00000253 tgagggctgaaaagaatgccccagtctctgtcagcac 00000289
		>>>>>>>> ||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92660427 tgagggctgaaaagaatgccccagtctctgtcagcac 92660463

		This record has 4 blocks, but they are far enough away from each other that running smith waterman against it as a whole wouldn't work.
		
		The plan is to identify these sequences and try and find a better means of reporting them.
	 * @param record
	 * @return
	 */
//	public static boolean taRecSuitableForAlternativeAlignment(TARecord record) {
//		
//		TLongIntMap positions = record.getCompoundStartPositions(10000);
//		System.out.println("positions size: " + positions.size());
//		positions.forEachEntry((l,i) -> {System.out.println("p.key: " + l + ", value: " + NumberUtils.getPartOfPackedInt(i, true)); return true;});
//		
//		TLongIntMap bestPositions = record.getBestCompoundStartPositions(10000, 100);
//		System.out.println("bestPositions size: " + bestPositions.size());
//		bestPositions.forEachEntry((l,i) -> {System.out.println("p.key: " + l + ", value: " + NumberUtils.getPartOfPackedInt(i, true)); return true;}); 
//		
//		
//		return false;
//	}
	
//	public static void main(String[] args) {
//		String seq = "AAAGAATGTAATTATATCTAGTGCTGCAGAAAGGCCTTTAGAAATAAGAGGGCCATATGACGTGGCAAATCTAGGCTTGCTGTTTGGGCTCTCTGAAAGTGACGCCAAGGCTGCGGTGTCCACCAACTGCCGAGCAGCGCTTCTCCATGGAGAAACTAGAAAAACTGCTTTTGGAATTATCTCTACAGTGAAGAAACCTCGGCCATCAGAAGGAGATGAAGATTGTCTTCCAGCTTCCAAGAAAGCCAAGTGTGAGGGCTGAAAAGAATGCCCCAGTCTCTGTCAGCACC";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		System.out.println("38 in fancy speak: " + NumberUtils.getTileCount(38,0));
//		
//		countPosition.put(NumberUtils.getTileCount(127,0), getLongList(1773033468));
//		countPosition.put(NumberUtils.getTileCount(68,0), getLongList(1773029213));
//		countPosition.put(NumberUtils.getTileCount(27,0), getLongList(1773028779));
//		countPosition.put(NumberUtils.getTileCount(20,0), getLongList(1773028352));
//		countPosition.put(NumberUtils.getTileCount(9,0), getLongList(849417735));
//		countPosition.put(NumberUtils.getTileCount(7,0), getLongList(1871817837));
//		countPosition.put(NumberUtils.getTileCount(6,0), getLongList(489936744l, 2519505810l, 841100535, 723777900, 2692250839l, 4611686020551367800l, 4611686020113723373l, 4611686019028499874l));
//		
//		
//		TARecord rec = new TARecord(seq, countPosition);
//		
//		System.out.println("taRecSuitableForSmithWaterman: " + taRecSuitableForAlternativeAlignment(rec));
//		
//		
//		/*
//		 * the next record should not return any compound collections
//		 */
//		countPosition.clear();
//		countPosition.put(NumberUtils.getTileCount(48,0), getLongList(2859915179l));
//		countPosition.put(NumberUtils.getTileCount(8,0), getLongList(4611686021185871748l, 4611686020065615314l, 4611686018836134883l));
//		countPosition.put(NumberUtils.getTileCount(7,0), getLongList(1224943698,1338097123, 1666708881, 2625345865l, 4611686020564558640l ,4611686019732189580l, 4611686019861845410l));
//		
//		
//		rec = new TARecord(seq, countPosition);
//		
//		System.out.println("taRecSuitableForSmithWaterman: " + taRecSuitableForAlternativeAlignment(rec));
//		
//		/*
//		 * not sure about this one
//		 */
//		countPosition.clear();
//		countPosition.put(3276800, getLongList(2960694027l, 4611686019344588891l));
//		countPosition.put(3145728, getLongList( 1529683796,1808006950, 1414478922, 4611686018507797949l, 4611686018820208328l, 4611686018982675941l, 4611686019880971921l,4611686020699639737l));
//		countPosition.put(3080192, getLongList(841241781, 1506401925, 4611686019453818857l, 4611686021350488568l, 4611686021377430294l, 4611686019868196139l));
//		countPosition.put(3014656, getLongList(660178815, 4611686018650962746l, 4611686019038669433l));
//		countPosition.put(2949120, getLongList(1854121183, 555571942, 1114302415, 1162194121, 1178038352, 1520149521, 4611686018899658818l));
//		countPosition.put(2883584, getLongList(833646922, 2389448732l, 4611686019787516765l));
//		countPosition.put(2818048, getLongList(493879239, 435979951));
//		countPosition.put(2752512, getLongList(308804068, 2187940672l, 2976557677l, 4611686019587760389l));
//		
//		countPosition.put(2490368, getLongList(59423804l,71979384l,83729291l,93975158l,142876166l,187566804l,192469715l,220418500l,247850478l,253941183l,374416071l,417095586l,497408217l,551277875l,608528841l,706315597l,714088822l,750416990l,769499316l,771098131l,824337991l,838056866l,855744329l,857963556l,866413318l,895043313l,914749180l,924361520l,939306705l,940317247l,974223445l,979509072l,1014545680l,1031749100l,1060154771l,1161692896l,1166698867l,1167479231l,1175566911l,1176089652l,1179501823l,1179965258l,1201685740l,1204429206l,1259440483l,1266161170l,1276755723l,1303319345l,1344742780l,1469032147l,1477155070l,1545295560l,1560696325l,1611280683l,1616198903l,1632071601l,1652339098l,1685660463l,1767488409l,1772143959l,1843149141l,1876207525l,1891367552l,1907810459l,1958318134l,1996259151l,1996679039l,2041844790l,2060349162l,2063973408l,2081199656l,2248306566l,2248592468l,2263039458l,2286288121l,2346360670l,2389375993l,2391338123l,2401504351l,2463893120l,2471633094l,2471652471l,2531386020l,2581902778l,2589333518l,2618512633l,2626553819l,2770693696l,2913782304l,2937755607l,2968109697l,2987640855l,3022598920l,3027768542l,3039615440l,3061304939l,3062938687l,3063819698l,3099852764l,943785683l,1492068194l,2266263179l,4611686018493418114l,4611686018532595346l,4611686018532706752l,4611686018570519606l,4611686018600344299l,4611686018601740806l,4611686018623582384l,4611686018664576649l,4611686018687780735l,4611686018693419499l,4611686018712984153l,4611686018789905896l,4611686018806261063l,4611686018836790119l,4611686018851036521l,4611686018858342890l,4611686018863350959l,4611686018923789007l,4611686018937740732l,4611686018941868518l,4611686018943974828l,4611686018966668474l,4611686018974272199l,4611686018997656675l,4611686019009238639l,4611686019010062594l,4611686019023119259l,4611686019061319841l,4611686019131877386l,4611686019176294789l,4611686019184020258l,4611686019193509073l,4611686019194062093l,4611686019218552898l,4611686019225358506l,4611686019238736189l,4611686019313517037l,4611686019347646404l,4611686019393974014l,4611686019399132811l,4611686019413869527l,4611686019425883875l,4611686019433715280l,4611686019447361730l,4611686019475414375l,4611686019481850383l,4611686019492353849l,4611686019495822461l,4611686019514747747l,4611686019561416869l,4611686019563634221l,4611686019574443301l,4611686019623277697l,4611686019646581956l,4611686019691505001l,4611686019691727328l,4611686019693773209l,4611686019726802750l,4611686019754265961l,4611686019754467870l,4611686019771248499l,4611686019797149840l,4611686019808590594l,4611686019856531594l,4611686019882370118l,4611686019890453211l,4611686019893977368l,4611686019916571505l,4611686019919785895l,4611686019950093310l,4611686019957639890l,4611686020082114006l,4611686020082753435l,4611686020088937195l,4611686020119540462l,4611686020138997964l,4611686020190249298l,4611686020208309723l,4611686020219339210l,4611686020279874929l,4611686020330352845l,4611686020333435009l,4611686020381916652l,4611686020383533855l,4611686020391852827l,4611686020399670282l,4611686020420339503l,4611686020422810295l,4611686020430264699l,4611686020433798325l,4611686020490575377l,4611686020505091845l,4611686020547214584l,4611686020562914619l,4611686020565003948l,4611686020646390372l,4611686020653003800l,4611686020672504338l,4611686020786003420l,4611686020976970174l,4611686020992158429l,4611686021022736816l,4611686021153064004l,4611686021158748852l,4611686021219165383l,4611686021310249820l,4611686021371660304l,4611686021383202151l,4611686021383970356l,4611686021386888468l,4611686021403739980l,4611686021411708604l,4611686021438806689l,4611686021459831641l,4611686021465470380l,4611686021469171938l,4611686021478988717l,4611686021479368873l,4611686021482870541l,4611686021486803747l,4611686021490138285l,4611686021491019259l,4611686021528498270l,4611686018571431810l));
//		
//		
//		rec = new TARecord("CGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCC", countPosition);
//		
//		System.out.println("taRecSuitableForSmithWaterman: " + taRecSuitableForAlternativeAlignment(rec));
//		
////		getSplitPositions(createTARec_chr15_34031839_split(), 1680373145, 1680373145 + 135534747, 2307285721l, 2307285721l + 102531392);
//		
//		pcpm.loadMap(PositionChrPositionMap.grch37Positions);
//		
//		System.out.println("trying out splitting");
//		TARecord r = createTARec_chr15_34031839_split_withTileStartPositionsWithinRange();
//		TIntObjectMap<Set<IntLongPairs>> splits = getSplitStartPositions(r);
//		System.out.println("trying out splitting, splits.size: " + splits.size());
//		List<BLATRecord[]> blatRecs = blatRecordsFromSplits(splits, "splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799", r.getSequence().length(), pcpm);
//		System.out.println("blat record count: " + blatRecs.size());
//		for (BLATRecord br : blatRecs.get(0)) {
//			System.out.println("blat record: " + br.toString());
//		}
//		
//		/*
//		 * from qsv:
//		 * 119     119     0       0       0       0       0       0       0       +       splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799      188     69      188     chr15   12345   34031838        34031957        1       119     69      34031838
//		 * 65      67      2       0       0       0       0       0       0       -       splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799      188     0       69      chr10   12345   127633806       127633875       1       69      119     127633806
//		 * 
//		 * from here:
//		 * 60	60	0	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799	188	9	69	chr10	12345	127633806	127633866	1	60	119	127633806
// 		 * 119	119	0	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799	188	69	188	chr15	12345	34031838	34031957	1	119	69	34031838
//		 */
//		
//		
//		
//		r = createTARec_chr8_125551528_split_withTileStartPositionsWithinRange();
//		splits = getSplitStartPositions(r);
//		System.out.println("trying out splitting, splits.size: " + splits.size());
//		blatRecs = blatRecordsFromSplits(splits, "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892", r.getSequence().length(), pcpm);
//		System.out.println("blat record count: " + blatRecs.size());
//		for (BLATRecord br : blatRecs.get(0)) {
//			System.out.println("blat record: " + br.toString());
//		}
//		
//		
//		
//		//createTARec_splitcon_chr7_100867120_chr7_100867215
//		r = createTARec_splitcon_chr7_100867120_chr7_100867215();
//		splits = getSplitStartPositions(r);
//		System.out.println("trying out splitting, splits.size: " + splits.size());
//		blatRecs = blatRecordsFromSplits(splits, "splitcon_chr7_100867120_chr7_100867215", r.getSequence().length(), pcpm);
//		System.out.println("blat record count: " + blatRecs.size());
//		for (BLATRecord br : blatRecs.get(0)) {
//			System.out.println("blat record: " + br.toString());
//		}
//	}
	
	
//	public static TARecord createTARecord() {
//		String seq = "AAAGAATGTAATTATATCTAGTGCTGCAGAAAGGCCTTTAGAAATAAGAGGGCCATATGACGTGGCAAATCTAGGCTTGCTGTTTGGGCTCTCTGAAAGTGACGCCAAGGCTGCGGTGTCCACCAACTGCCGAGCAGCGCTTCTCCATGGAGAAACTAGAAAAACTGCTTTTGGAATTATCTCTACAGTGAAGAAACCTCGGCCATCAGAAGGAGATGAAGATTGTCTTCCAGCTTCCAAGAAAGCCAAGTGTGAGGGCTGAAAAGAATGCCCCAGTCTCTGTCAGCACC";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		countPosition.put(8323072, getLongList(1773033468));
//		countPosition.put(1769472, getLongList(1773028779));
//		countPosition.put(589824, getLongList(849417735));
//		countPosition.put(524288, getLongList(1871817837));
//		countPosition.put(458752, getLongList(489936744,2519505810l,841100535,723777900, 2692250839l,4611686020551367800l, 4611686020113723373l,4611686019028499874l));
//		countPosition.put(393216, getLongList(2590425720l, 1213578263,1275022186, 3022457391l, 4611686020263048134l, 4611686019266487298l, 4611686019044195867l, 4611686018535246479l, 4611686018514089669l, 4611686020558051630l, 4611686020710517289l, 4611686018604963266l, 4611686020650563757l, 4611686021085481831l, 4611686018519835715l, 4611686020197744473l, 4611686018975194550l, 4611686020139682797l, 4611686018611751762l, 4611686019333492255l, 4611686020725560615l, 4611686019472767768l, 4611686019265594152l, 4611686019588474803l, 4611686020068315911l, 4611686021325807621l, 4611686020929619672l, 4611686019614000954l, 4611686021073078339l));
//		
//		
//		countPosition.put(4456448, getLongList(1773029213));
//		countPosition.put(1310720, getLongList(1773028352));
//		
//		TARecord rec = new TARecord(seq, countPosition);
//		return rec;
//	}
//	
//	public static TARecord createTARec_chr15_34031839_split() {
//		/*
//		 * splitcon_chr10_127633807_chr15_34031839
//		 */
//		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		countPosition.put(3342336, getLongList(917200986, 4611686019268629681l));
//		countPosition.put(3276800, getLongList(4611686021388081931l));
//		countPosition.put(3211264, getLongList(392820424, 4611686019841866826l));
//		countPosition.put(3145728, getLongList(80410045l,555288037l,1453584017l,2272251833l,4611686019957071700l,4611686020235394854l));
//		countPosition.put(3080192, getLongList(1026430953l,2923100664l,2950042390l,1440808235l,4611686019933789829l));
//		countPosition.put(3014656, getLongList(223574842l,611281529l,4611686019087566719l));
//		countPosition.put(2949120, getLongList(472270914l,58740652l,432146535l,4611686018921267141l,4611686020281509087l,4611686018982959846l,4611686019541690319l,4611686019589582025l,4611686019605426256l,4611686019947537425l));
//		countPosition.put(2883584, getLongList(1360128861l,855953047l,2911039236l,4611686018924132828l,4611686019261034826l,4611686020816836636l));
//		countPosition.put(2818048, getLongList(1140157854l,1187575884l,1681646586l,2930823348l,4611686020897215369l,4611686019075024747l,4611686018792400455l,4611686019018591529l,4611686019282413711l,4611686020193800769l,4611686018863367855l));
//		countPosition.put(7012352, getLongList(2341317558l));
//		countPosition.put(2752512, getLongList(1160372485l,35037654l,169226777l,170064290l,307865270l,545855294l,590079559l,898276097l,960024381l,1061962185l,1147621313l,1172620199l,1215575699l,1344546577l,1475039753l,1481135695l,1489366131l,1848876640l,2123168685l,2387069827l,2483681389l,2727155027l,2770651358l,2944511489l,4611686018599671290l,4611686018836017654l,4611686019133035956l,4611686019139021341l,4611686019675789035l,4611686019760451753l,4611686019999276630l,4611686020080100993l,4611686020219126251l,4611686020236126087l,4611686020658156826l,4611686020892837724l,4611686020897215520l,4611686020909866188l,4611686021225286132l,4611686021458919982l,4611686018736191972l,4611686020615328576l,4611686021403945581l,4611686021274257912l));
//		
//		System.out.println("lowest count: " + NumberUtils.getPartOfPackedInt(2752512, true));
//		
//		TARecord rec = new TARecord(seq, countPosition);
//		return rec;
//	}
	
//	public static TARecord createTARec_chr15_34031839_split_withTileStartPositions() {
//		/*
//		 * splitcon_chr10_127633807_chr15_34031839
//		 */
//		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		/*
//		 * match count: [107, 0], number of starts: 1
//p: 75868643634102
//match count: [51, 0], number of starts: 2
//p: 4398963712090
//p: 4611812463105823921
//match count: [50, 0], number of starts: 1
//p: 4611817962783415051
//match count: [49, 0], number of starts: 2
//p: 9895997470408
//p: 4611821259772083274
//match count: [48, 0], number of starts: 6
//p: 9895685060029
//p: 9896159938021
//p: 9897058234001
//p: 9897876901817
//p: 4611816861840777044
//p: 4611816862119100198
//match count: [47, 0], number of starts: 5
//p: 9896631080937
//p: 9898527750648
//p: 9898554692374
//p: 10996557085995
//p: 4611817961329122949
//match count: [46, 0], number of starts: 3
//p: 9895828224826
//p: 9896215931513
//p: 4611819059994527615
//match count: [45, 0], number of starts: 10
//match count: [44, 0], number of starts: 6
//p: 9896964778845
//p: 17593041997463
//p: 17595097083652
//p: 4611813562272954844
//p: 4611821259191251274
//p: 4611821260747053084
//		 */
//		
//		countPosition.put(7012352, getLongList(75868643634102l));
//		countPosition.put(3342336, getLongList(4398963712090l, 4611812463105823921l));
//		countPosition.put(3276800, getLongList(4611817962783415051l));
//		countPosition.put(3211264, getLongList(9895997470408l, 4611821259772083274l));
//		countPosition.put(3145728, getLongList(9895685060029l,9896159938021l,9897058234001l,9897876901817l,4611816861840777044l,4611816862119100198l));	// 4611816862119100198l is the one we want!!!
//		countPosition.put(3080192, getLongList(9896631080937l,9898527750648l,9898554692374l,10996557085995l,4611817961329122949l));
//		countPosition.put(3014656, getLongList(9895828224826l,9896215931513l,4611819059994527615l));
//		countPosition.put(2883584, getLongList(9896964778845l,17593041997463l,17595097083652l,4611813562272954844l,4611821259191251274l,4611821260747053084l));
//		TARecord rec = new TARecord(seq, countPosition);
//		return rec;
//	}
	
//	public static TARecord createTARec_chr15_34031839_split_withTileStartPositionsWithinRange() {
//		/*
//		 * splitcon_chr10_127633807_chr15_34031839
//		 */
//		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		/*
//		 * match count: [107, 0], number of starts: 1
//p: 75868643634102
//match count: [48, 0], number of starts: 1
//p: 4611816862119100198
//match count: [44, 0], number of starts: 1
//p: 4611821260747053084
//match count: [43, 0], number of starts: 2
//p: 17593867691002
//p: 4611814663054250561
//match count: [42, 0], number of starts: 3
//p: 17594573114243
//p: 4611815762591203819
//p: 4611815762608203655
//match count: [41, 0], number of starts: 12
//p: 17593872835136
//p: 17593884743111
//p: 17593973560642
//p: 17593999726488
//p: 17594546763755
//p: 17594567136894
//p: 4611816862002159212
//p: 4611816862074935759
//p: 4611816862656098325
//p: 4611816862669844665
//p: 4611816862676796898
//p: 4611816862705450293
//		 */
//		
//		countPosition.put(7012352, getLongList(75868643634102l));
//		countPosition.put(3145728, getLongList(4611816862119100198l));	// 4611816862119100198l is the one we want!!!
//		TARecord rec = new TARecord(seq, countPosition);
//		return rec;
//	}
	
//	public static TARecord createTARec_chr8_125551528_split_withTileStartPositionsWithinRange() {
//		/*
//		 * splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892
//		 */
//		String seq = "CGCGGCCGGGGAAGGTCAGCGCCGTAATGGCGTTCTTGGCGTCGGGACCCTACCTGACCCATCAGCAAAAGGTGTTGCGGCTTTATAAGCGGGCGCTACGCCACCTCGAGTCGTGGTGCGTCCAGAGAGACAAATACCGATACTTTGCTTGTTTGATGAGAGCCCGGTTTGAAGAACATAAGAATGAAAAGGATATGGCGAAGGCCACCCAGCTGCTGAAGGAGGCCGAGGAAGAATTCTGGTACCGTCAGCATCCAC";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		/*
//		 *match count: [121, 0], number of starts: 1
//p: 137440471823016
//match count: [115, 0], number of starts: 1
//p: 1518347092
//match count: [23, 0], number of starts: 1
//p: 4611703612118049933
//match count: [20, 0], number of starts: 1
//p: 4611728900885488804
//match count: [17, 0], number of starts: 1
//p: 4611754189652927675
//match count: [12, 0], number of starts: 1
//p: 4611789374025016524
//match count: [7, 0], number of starts: 1
//p: 4611896026652910893
//		 */
//		
//		countPosition.put(NumberUtils.getTileCount(121, 0), getLongList(137440471823016l));
//		countPosition.put(NumberUtils.getTileCount(115, 0), getLongList(1518347092));
//		
//		TARecord rec = new TARecord(seq, countPosition);
//		return rec;
//	}
	
//	public static TARecord createTARec_splitcon_chr7_100867120_chr7_100867215() {
//		/*
//		 * splitcon_chr7_100867120_chr7_100867215
//		 */
//		String seq = "TGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCG";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		
//		/*
//		 *match count: [165, 2], number of starts: 1
//p: 86862753118281
//match count: [135, 2], number of starts: 1
//p: 274879241468242
//match count: [61, 2], number of starts: 1
//p: 1100846151560
//match count: [8, 2], number of starts: 1
//p: 69270567073733
//match count: [7, 2], number of starts: 4
//p: 3299873399669
//p: 4611801468519265617
//p: 4611849846975127200
//p: 4611925713246426543
//match count: [6, 2], number of starts: 2
//p: 3299862685429
//p: 3299927520776
//		 */
//		
//		countPosition.put(NumberUtils.getTileCount(165, 2), getLongList(86862753118281l));
//		countPosition.put(NumberUtils.getTileCount(135, 2), getLongList(274879241468242l));
//		countPosition.put(NumberUtils.getTileCount(61, 2), getLongList(1100846151560l));
//		
//		TARecord rec = new TARecord(seq, countPosition);
//		return rec;
//	}
	
	/**
	 * The purpose of this method is to examine if there are 2 sets of start positions, potentially miles apart (different contigs) that could make up the sequence
	 * Not sure on the best way of doing this, but will start by finding the largest start position, seeing how many tiles the second chunk would need to be composed of and going from there....
	 * 
	 * @param record
	 * @return
	 */
//	public static TLongIntMap getSplitPositions(TARecord record, long firstContigStart, long firstContigStop, long secondContigStart, long secondContigStop) {
//		
//		long firstContigStartRS = NumberUtils.setBit(firstContigStart, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
//		long firstContigStopRS = NumberUtils.setBit(firstContigStop, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
//		long secondContigStartRS = NumberUtils.setBit(secondContigStart, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
//		long secondContigStopRS = NumberUtils.setBit(secondContigStop, TiledAlignerUtil.REVERSE_COMPLEMENT_BIT);
//		
//		TLongList perfectMAtchPositions = record.getPerfectMatchPositions();
//		TLongIntMap map = new TLongIntHashMap();
//		if (null == perfectMAtchPositions || perfectMAtchPositions.isEmpty()) {
//			
//			TIntObjectMap<TLongList> countsAndStartPosisions = record.getCounts();
//			
//			int [] keys = countsAndStartPosisions.keys();
//			Arrays.sort(keys);
//			
//			for (int i = keys.length - 1 ; i >= 0 ; i--) {
//				TLongList list =  countsAndStartPosisions.get(keys[i]);
//				for (int j = 0 ; j < list.size() ; j ++) {
//					if (inRange(list.get(j), firstContigStart, firstContigStop, secondContigStart, secondContigStop)
//							|| inRange(list.get(j), firstContigStartRS, firstContigStopRS, secondContigStartRS, secondContigStopRS)) {
//						System.out.println("Found start position within range: " + list.get(j) + " that has tile count: " + NumberUtils.getPartOfPackedInt(keys[i], true));
//						map.put(list.get(j), keys[i]);					
//					}
//				}
//			}
//			System.out.println("Number of entries in split map = " + map.size());
//		}
//		return map;
//	}
	
	public static boolean inRange(long position, long startOne, long stopOne, long startTwo, long stopTwo) {
		return (position >= startOne && position <= stopOne) || (position >= startTwo && position <= stopTwo);
	}
	
	public static TLongList getLongList(long ... list) {
		TLongList listToReturn = new TLongArrayList(list.length + 1);
		for (long l : list) {
			listToReturn.add(l);
		}
		return listToReturn;
	}
	
	public static List<BLATRecord[]> blatRecordsFromSplits(TIntObjectMap<Set<IntLongPairs>> splits, String name, int seqLength, PositionChrPositionMap headerMap) {
		if (null != splits && ! splits.isEmpty()) {
		
			/*
			 * get the highest scoring list of splits
			 */
			int [] keys = splits.keys();
//			keys = sortTileCount(keys);
			Arrays.sort(keys);
			int maxKey = keys[keys.length - 1];
			Set<IntLongPairs> maxSplits = splits.get(maxKey);
			
			List<BLATRecord[]> blats = new ArrayList<>(maxSplits.size() + 1);
			for (IntLongPairs maxSplit : maxSplits) {
				IntLongPair[] pairs = maxSplit.getPairs();
				BLATRecord [] blatties = new BLATRecord[pairs.length];
				for (int i = 0 ; i < pairs.length ; i++) {
					blatties[i] = new BLATRecord(blatRecordFromSplit(pairs[i], name, seqLength, headerMap));
				}
				Arrays.sort(blatties);
				blats.add(blatties);
			}
			return blats;
		}
		return Collections.emptyList();
		
	}
	
	/**
	 * If the ILPs can form a single BLAT record, then return that, otherwise, return as few BLAT records as possible. ie. if there are 3 ILPs in the pair, and 2 can be combined to form a single BLAT rec then do so
	 * 
	 * @param splits
	 * @param name
	 * @param seqLength
	 * @param headerMap
	 * @return
	 */
	public static List<BLATRecord[]> blatRecordsFromSplitsNew(TIntObjectMap<Set<IntLongPairs>> splits, String name, int seqLength, PositionChrPositionMap headerMap) {
		if (null != splits && ! splits.isEmpty()) {
			
			/*
			 * get the highest scoring list of splits
			 */
			int [] keys = splits.keys();
//			keys = sortTileCount(keys);
			Arrays.sort(keys);
			int maxKey = keys[keys.length - 1];
			
			Set<IntLongPairs> maxSplits = splits.get(maxKey);
			logger.info("Number of splits: " + splits.size() + ", number of splits with max coverage: " + maxSplits.size());
			
			List<BLATRecord[]> blats = new ArrayList<>(maxSplits.size() + 1);
			for (IntLongPairs maxSplitILPs : maxSplits) {
				BLATRecord [] blatties = null;
				/*
				 * will attempt to create a single BLAT record
				 */
				if (IntLongPairsUtil.isIntLongPairsAValidSingleRecord(maxSplitILPs)) {
					String [] blatData = TARecordUtil.blatRecordFromSplits(maxSplitILPs, name, seqLength, headerMap, TILE_LENGTH);
					if (null != blatData && blatData.length > 0) {
						blatties = new BLATRecord[] {new BLATRecord(blatData)};
					}
				} else {
					
					IntLongPair[] pairs = maxSplitILPs.getPairs();
					logger.info("createing BLAT records, number of constituent ILPs in ILPS: " + pairs.length);
					if (pairs.length < 3) {
						/*
						 * return a BLAT record for each constituent in the maxSplitILPs
						 */
						blatties = new BLATRecord[pairs.length];
						for (int i = 0 ; i < pairs.length ; i++) {
							String [] blatARray = blatRecordFromSplit(pairs[i], name, seqLength, headerMap);
							if (null != blatARray && blatARray.length > 0) {
								blatties[i] = new BLATRecord(blatARray);
							}
						}
						Arrays.sort(blatties);
					} else {
						
						/*
						 * from constituent ILPs, find largst, and then see if any combination of adding other ILPs results in a single BLAT record
						 */
						Optional<IntLongPairs> oSingleBLATRec = IntLongPairsUtil.getSingleBLATRecordFromILPs(maxSplitILPs);
						if (oSingleBLATRec.isPresent()) {
							/*
							 * need to find the ILPs that didn't make it into the ILPS so that they can be added as separate BLAT records
							 */
							List<IntLongPair> rejectedILPs = IntLongPairsUtil.getRejectedILPs(maxSplitILPs, oSingleBLATRec.get());
							logger.info("found optional singleBLATRecord! Number of rejected ILPs: " + rejectedILPs.size());
							
							blatties = new BLATRecord[rejectedILPs.size() + 1];
							for (int i = 0 ; i < rejectedILPs.size() ; i++) {
								String [] blatArray = blatRecordFromSplit(rejectedILPs.get(i), name, seqLength, headerMap);
								if (null != blatArray && blatArray.length > 0) {
									blatties[i] = new BLATRecord(blatArray);
								}
							}
							String [] blatArray = blatRecordFromSplits(oSingleBLATRec.get(), name, seqLength, headerMap, TILE_LENGTH);	
							if (null != blatArray && blatArray.length > 0) {
								blatties[blatties.length - 1] = new BLATRecord(blatArray);
							}
							
							if (null != blatties && blatties.length > 1) {
								Arrays.sort(blatties);
							}
						}
					}
				}
				if (null != blatties && blatties.length > 0) {
					blats.add(blatties);
				}
			}
			return blats;
		}
		return Collections.emptyList();
	}
	
	public static String[] blatRecordFromSplit(IntLongPair split, String name, int seqLength, PositionChrPositionMap headerMap) {
		return blatRecordFromSplit(split, name, seqLength, headerMap, TILE_LENGTH);
	}
	
	public static String[] blatRecordFromSplit(IntLongPair split, String name, int seqLength, PositionChrPositionMap headerMap, int tileLength) {
		ChrPosition cp = headerMap.getChrPositionFromLongPosition(split.getLong());
		boolean reverseStrand = NumberUtils.isBitSet(split.getLong(), REVERSE_COMPLEMENT_BIT);
		int length = NumberUtils.getPartOfPackedInt(split.getInt(), true) + tileLength - 1;
		int mismatch = NumberUtils.getPartOfPackedInt(split.getInt(), false);
		int positionInSequence = NumberUtils.getShortFromLong(split.getLong(), TILE_OFFSET);
		
		String[] array = new String[21];
		array[0] = "" + length;	//number of matches
		array[1] = "" + mismatch;		//number of mis-matches
		array[2] = "0";					//number of rep. matches
		array[3] = "0";					//number of N's
		array[4] = "0";					// Q gap count
		array[5] = "0";					// Q gap bases
		array[6] = "0";					// T gap count
		array[7] = "0";					// T gap bases
		array[8] = reverseStrand ? "-" : "+";			// strand
		array[9] = name;					// Q name
		array[10] = seqLength + "";			// Q size
		
		/*
		 * start and end are strand dependent
		 * if we are on the forward, its the beginning of the first bloak, and end of the last
		 * if we are on reverse, need to reverse!
		 */
		int start = reverseStrand ? (seqLength - positionInSequence - length) :  positionInSequence;
		int end = reverseStrand ?  (seqLength - positionInSequence) : positionInSequence + length;
		
		array[11] = "" + start;			// Q start
		array[12] = "" + end;	// Q end
		array[13] = cp.getChromosome();			// T name
		array[14] = "12345";					// T size
		int tStart = cp.getStartPosition();
		
		array[15] = "" + tStart;					// T start
		array[16] = "" + (length + tStart);			// T end
		array[17] = "1";							// block count
		array[18] = "" + length;					// block sizes
		array[19] = "" + positionInSequence;		// Q block starts
		array[20] = "" + tStart;					// T block starts
		
		return array;
	}
	
	/**
	 * This method takes a IntLongPairs object and returns a ChrPosition to int[] map, which is how the IntLongPairs is represented in a BLATRecord
	 * The keys in the map can be sorted to get the ChrPositions in order.
	 * 
	 * This method also trims any overlaps that can commonly occur due to the nature of the tiled aligner approach
	 * 
	 * 
	 * @param splits
	 * @param seqLength
	 * @param headerMap
	 * @return
	 */
	public static Map<ChrPosition, int[]> getChrPositionAndBlocksFromSplits(IntLongPairs splits, int seqLength, PositionChrPositionMap headerMap) {
		IntLongPair[] pairs = IntLongPairsUtil.sortIntLongPairs(splits, seqLength);
		
		int [][] ranges = new int[pairs.length][];
		ChrPosition[] cps = new ChrPosition[pairs.length];
		int i = 0;
		for (IntLongPair ilp : pairs) {
			
			/*
			 * start position is dependent on whether the reverse complement bit has been set.
			 */
			ranges[i] = new int[]{ IntLongPairsUtil.getStartPositionInSequence(ilp, seqLength), getExactMatchOnlyLengthFromPackedInt(ilp.getInt())};
//			ranges[i] = new int[]{ NumberUtils.getShortFromLong(ilp.getLong(), TILE_OFFSET), getExactMatchOnlyLengthFromPackedInt(ilp.getInt())};
			cps[i] = headerMap.getChrPositionFromLongPosition(ilp.getLong(), ranges[i][1]);
			i++;
		}
		
		trimRangesToRemoveOverlap(ranges, cps);
		
		Map<ChrPosition, int[]> results = new THashMap<>();
		
		for (int j = 0 ; j < pairs.length ; j++) {
			results.put(cps[j], ranges[j]);
		}
		return results;
	}

	/**
	 * NOT SIDE EFFECT FREE
	 * <br>
	 * This method will update the values in the passed in  2D int array if there are overlapping values
	 * It will attempt to remove any overlap from the larger of the 2 ranges should an overlap exist
	 * 
	 * It requires that the ranges array be sorted by position in query string
	 * 
	 * Also need to check to see if the genomic ranges overlap, and trim accordingly if they do
	 * 
	 * @param ranges
	 */
	public static void trimRangesToRemoveOverlap(int[][] ranges, ChrPosition[] cps) {
		/*
		 * trim ranges if there is an overlap
		 */
		for (int j = 0 ; j < ranges.length - 1; j++) {
			if (j + 1 < ranges.length) {
				int [] thisIntArray = ranges[j];
				int [] nextIntArray = ranges[j + 1];
				ChrPosition thisCP = cps[j];
				ChrPosition nextCP = cps[j + 1];
				
				int diff = (thisIntArray[0] + thisIntArray[1]) - nextIntArray[0];
				if (diff > 0) {
					if ( thisIntArray[1] >= nextIntArray[1]) {
						/*
						 * update this, take diff away from length
						 */
						thisIntArray[1] = thisIntArray[1] - diff;
//						ChrPosition orig = cps[j];
						if (diff >= thisCP.getLength()) {
							logger.warn("Diff is greater the cp length! diff: " + diff + ", cp length: " + thisCP.getLength());
							for (int [] range : ranges) {
								logger.warn("range: " + Arrays.toString(range));
							}
							for (ChrPosition cp : cps) {
								logger.warn("cp: " + cp.toIGVString());
							}
						} else {
							cps[j] = new ChrPositionName(thisCP.getChromosome(), thisCP.getStartPosition(), thisCP.getEndPosition() - diff, thisCP.getName());
						}
					} else {
						/*
						 * update next array, take diff away from start (and length)
						 */
						nextIntArray[0] = nextIntArray[0] + diff;
						nextIntArray[1] = nextIntArray[1] - diff;
						
//						ChrPosition orig = cps[j + 1];
						
						if (diff >= nextCP.getLength()) {
							logger.warn("Diff is greater the cp length! diff: " + diff + ", cp length: " + nextCP.getLength());
							for (int [] range : ranges) {
								logger.warn("range: " + Arrays.toString(range));
							}
							for (ChrPosition cp : cps) {
								logger.warn("cp: " + cp.toIGVString());
							}
						} else {
						
							cps[j + 1] = new ChrPositionName(nextCP.getChromosome(), nextCP.getStartPosition() + diff, nextCP.getEndPosition(), nextCP.getName());
						}
					}
				} else {
					/*
					 * now need to check that the reference positions don't overlap (ChrPosition array)
					 * ALSO need to take into account whether the sequence was reverse complemented
					 */
					boolean reverseComplemented = "R".equals(thisCP.getName());
					int diffRef = reverseComplemented ? nextCP.getEndPosition() - thisCP.getStartPosition() :  thisCP.getEndPosition() - nextCP.getStartPosition();
					if (diffRef > 0) {
						if ( thisIntArray[1] >= nextIntArray[1]) {
							/*
							 * update this, take diff away from length
							 */
							thisIntArray[1] = thisIntArray[1] - diffRef;
//						ChrPosition orig = cps[j];
							if (diffRef >= thisCP.getLength()) {
								logger.warn("Diff is greater the cp length! diff: " + diffRef + ", cp length: " + thisCP.getLength());
								for (int [] range : ranges) {
									logger.warn("range: " + Arrays.toString(range));
								}
								for (ChrPosition cp : cps) {
									logger.warn("cp: " + cp.toIGVString());
								}
							} else {
								if (reverseComplemented) {
									cps[j] = new ChrPositionName(thisCP.getChromosome(), thisCP.getStartPosition() + diffRef, thisCP.getEndPosition(), thisCP.getName());
								} else {
									cps[j] = new ChrPositionName(thisCP.getChromosome(), thisCP.getStartPosition(), thisCP.getEndPosition() - diffRef, thisCP.getName());
								}
							}
						} else {
							/*
							 * update next array, take diff away from start (and length)
							 */
							nextIntArray[0] = nextIntArray[0] + diffRef;
							nextIntArray[1] = nextIntArray[1] - diffRef;
							
//						ChrPosition orig = cps[j + 1];
							
							if (diffRef >= nextCP.getLength()) {
								logger.warn("Diff is greater the cp length! diff: " + diffRef + ", cp length: " + nextCP.getLength());
								for (int [] range : ranges) {
									logger.warn("range: " + Arrays.toString(range));
								}
								for (ChrPosition cp : cps) {
									logger.warn("cp: " + cp.toIGVString());
								}
							} else {
								if (reverseComplemented) {
									cps[j + 1] = new ChrPositionName(nextCP.getChromosome(), nextCP.getStartPosition(), nextCP.getEndPosition() - diffRef, nextCP.getName());
								} else {
									cps[j + 1] = new ChrPositionName(nextCP.getChromosome(), nextCP.getStartPosition() + diffRef, nextCP.getEndPosition(), nextCP.getName());
								}
							}
						}
						
					}
				}
			}			
		}
	}
	
	/**
	 * 
	 * @param splits
	 * @param name
	 * @param seqLength
	 * @param headerMap
	 * @param tileLength
	 * @return
	 */
	public static String[] blatRecordFromSplits(IntLongPairs splits, String name, int seqLength, PositionChrPositionMap headerMap, int tileLength) {
		
		Map<ChrPosition, int[]> chrPosBlocks = getChrPositionAndBlocksFromSplits(splits, seqLength, headerMap);
		/*
		 * order the keys
		 */
		ChrPosition[] keys = new ChrPosition[chrPosBlocks.size()];
		chrPosBlocks.keySet().toArray(keys);
		Arrays.sort(keys);
		
		List<int[]> values = new ArrayList<>(chrPosBlocks.values());
		values.sort((int[] array1, int[] array2) -> array1[0] - array2[0]);

		int qGapBases = 0;
		
		for (int i = 0 ; i < values.size() - 1; i++) {
			int [] thisBlock = values.get(i);
			int [] nextBlock = values.get(i + 1);
			qGapBases += nextBlock[0] - (thisBlock[1] + thisBlock[0]);
		}
		
		
		boolean reverseStrand = keys[0].getName().equals("R");
		/*
		 * get length and qGapBases
		 */
		int length = 0;
		int tGapBases = 0;
		for (int i = 0 ; i < keys.length - 1; i++) {
			ChrPosition thisCp = keys[i];
//			int[] thisBlock = chrPosBlocks.get(thisCp);
			length += thisCp.getLength() - 1; 
			ChrPosition nextCp = keys[i + 1];
			
			
			/*
			 * If either of these ChrPositions are wholly contained within the other, then return
			 */
			if (ChrPositionUtils.isChrPositionContained(thisCp, nextCp) || ChrPositionUtils.isChrPositionContained(nextCp, thisCp)) {
				return new String[]{};
			}
			
//			int[] nextBlock = chrPosBlocks.get(nextCp);
			tGapBases += (nextCp.getStartPosition() - thisCp.getEndPosition());
//			qGapBases += nextBlock[0] - (thisBlock[1] + thisBlock[0]);
		}
		/*
		 * add last length
		 */
		length += keys[keys.length - 1].getLength() - 1;
//		int length = firstCp.getLength() + secondCp.getLength();
//		int qGapBases = secondBlock[0] - firstBlock[1];
		
		
		String[] array = new String[21];
		array[0] = "" + length;	//number of matches
		array[1] = "" + IntLongPairsUtil.getMismatches(splits);	//number of mis-matches
		array[2] = "0";					//number of rep. matches
		array[3] = "0";					//number of N's
		array[4] = qGapBases > 0 ? "1" : "0";					// Q gap count
		array[5] = "" + qGapBases;					// Q gap bases
		array[6] = "" + (keys.length - 1);					// T gap count
		array[7] = "" + tGapBases;			// T gap bases
		array[8] = reverseStrand ? "-" : "+";			// strand
		array[9] = name;					// Q name
		array[10] = seqLength + "";			// Q size
		
		/*
		 * start and end are strand dependent
		 * if we are on the forward, its the beginning of the first block, and end of the last
		 * if we are on reverse, need to reverse!
		 */
//		int start = reverseStrand ? (seqLength - positionInSequence - length) :  positionInSequence;
//		int end = reverseStrand ?  (seqLength - positionInSequence) : positionInSequence + length;
		
		array[11] = "" + chrPosBlocks.get(keys[0])[0];						// Q start
		// get last bloack
		int[] lastBlock = chrPosBlocks.get(keys[keys.length - 1]);
		array[12] = "" + (lastBlock[0] + lastBlock[1] - 1);	// Q end
		array[13] = keys[0].getChromosome();			// T name
		array[14] = "12345";							// T size
		int tStart = keys[0].getStartPosition();
		
		array[15] = "" + tStart;						// T start
		array[16] = "" + keys[keys.length - 1].getEndPosition();		// T end
		array[17] = "" + keys.length;								// block count
		array[18] = "" + Arrays.stream(keys).map(cp -> "" + (cp.getLength() - 1)).collect(Collectors.joining(","));					// block sizes
		array[19] = "" + Arrays.stream(keys).map(cp -> "" + (cp.getName().equals("R") ? seqLength - (chrPosBlocks.get(cp)[0] + chrPosBlocks.get(cp)[1]) : chrPosBlocks.get(cp)[0])).collect(Collectors.joining(","));				// Q block starts, strand dependent
		array[20] = "" + Arrays.stream(keys).map(cp -> "" + cp.getStartPosition()).collect(Collectors.joining(","));	// T block starts
		
		return array;
	}
	
	/**
	 * This method will examine the IntLongPair records within the supplied IntLongPairs object.
	 * 
	 * If they are within 10kb of each other, and on the same strand, we can potentially combine them into a single BLATRecord.
	 * If the criteria is not met, an empty Optional is retured instead.
	 * 
	 * @param splits
	 * @return
	 */
	public static String[] areSplitsCloseEnoughToBeSingleRecord(IntLongPairs splits, String name,  int seqLength, PositionChrPositionMap headerMap, int tileLength) {
		/*
		 * check strand first
		 */
		
		IntLongPair[] pairs = splits.getPairs();
		boolean firstPositionIsRevComp = NumberUtils.isBitSet(pairs[0].getLong(), REVERSE_COMPLEMENT_BIT);
		long firstPosition = NumberUtils.getLongPositionValueFromPackedLong(pairs[0].getLong());
		boolean allGood = true;
		for (int i = 1 ; i < pairs.length ; i++) {
			if (NumberUtils.isBitSet(pairs[i].getLong(), REVERSE_COMPLEMENT_BIT) != firstPositionIsRevComp) {
				allGood = false;
				break;
			}
			long thisPosition = NumberUtils.getLongPositionValueFromPackedLong(pairs[i].getLong());
			if (Math.abs(thisPosition - firstPosition) > MAX_GAP_FOR_SINGLE_RECORD) {
				allGood = false;
				break;
			}
		}
		if (allGood) {
			
			/*
			 * final check here is to sort the positions genomically, and then make sure that each 
			 */
			Arrays.sort(pairs, (pair1, pair2) -> Long.compare(NumberUtils.getLongPositionValueFromPackedLong(pair1.getLong()), NumberUtils.getLongPositionValueFromPackedLong(pair2.getLong())));
			boolean orderCorrect = true;
			int lastSeqPosition = -1;
			for (IntLongPair pair : pairs) {
				int thisSeqPosition = NumberUtils.getShortFromLong(pair.getLong(), TILE_OFFSET);
				if (lastSeqPosition > -1) {
					if (thisSeqPosition < lastSeqPosition) {
						orderCorrect = false;
						break;
					}
				}
				lastSeqPosition = thisSeqPosition;
			}
			
			if (orderCorrect) {
				String[] deetsForBlat = blatRecordFromSplits(splits,  name, seqLength, headerMap, tileLength);
				return deetsForBlat;
			}
		}
		
		return new String[]{};
	}
	
	/**
	 * tile counts hold a combination of tile matches and mismatch count.
	 * Need to take the mismatch count away from the tile count.
	 * 
	 * Favour tile counts that have zero mismatches over those that have mismatches
	 * eg.
	 * tile count of 40 is better than tile count of 41 with a mismatch count of 1.
	 * 
	 * @param tileCounts
	 * @return
	 */
	public static int[] sortTileCount(int[] tileCounts) {
		
		
//		Arrays.sort(tileCounts);
		return Arrays.stream(tileCounts)
				.mapToObj(k -> NumberUtils.splitIntInto2(k))
				.sorted((a,b) -> {int diff = Integer.compare((a[0] - a[1]), (b[0] - b[1]));
				                if (diff == 0) {
				                	diff = b[1] - a[1];
				                }
								return diff;})
				.mapToInt(a -> NumberUtils.pack2IntsInto1(a[0], a[1]))
				.toArray();
	}
	
	
	public static TIntObjectMap<Set<IntLongPairs>> getSplitStartPositions(TARecord record) {
		TIntObjectMap<Set<IntLongPairs>> results = new TIntObjectHashMap<>();
		TIntObjectMap<TLongList> countsAndStartPositions = record.getCounts();
		
		if (countsAndStartPositions.size() > 1 || (countsAndStartPositions.size() == 1 && countsAndStartPositions.get(countsAndStartPositions.keys()[0]).size() > 1 )) {
		
			int [] keys = countsAndStartPositions.keys();
			keys = sortTileCount(keys);
			int maxTileCount = getLengthFromPackedInt(keys[keys.length - 1]);
			int seqLength = record.getSequence().length();
			
			/*
			 * if our max tile count is less than 1/3 (randomly plucked..) then don't proceed as we will just end up with a range of splits that don't cover much of the sequence.
			 */
			if (maxTileCount >= (seqLength / 4)) {
			
				/*
				 * we don't want to look for splits right down in the weeds (where the tile count is low, but the number of start positions are high), as there will likely be plenty of low quality splits
				 * and so set a limit for the starting split - it should be in the top 3 (say) tile counts 
				 */
				int minTileCountCutoff = Math.max(keys.length - 3, 0);
				boolean areWeDone = false;
				boolean checkResults = true;
				for (int i = keys.length - 1 ; i >= minTileCountCutoff ; i--) {
					if (areWeDone) {
						break;
					}
					int tileCountAndCommon = keys[i];
					
					/*
					 * if we have more than 10% commonly occurring tiles, skip
					 */
//					int commonlyOccurringTiles = NumberUtils.getPartOfPackedInt(tileCountAndCommon, false);
//					if (commonlyOccurringTiles > (0.1 * seqLength)) {
//						continue;
//					}
					
//						int tileCount = getLengthFromPackedInt(tileCountAndCommon);
					int tileCount = NumberUtils.getPartOfPackedInt(tileCountAndCommon, true);
					/*
					 * only proceed if tileCount and tile length would give a score of 20
					 */
					if (tileCount >= MIN_TILE_COUNT) {
						TLongList list =  countsAndStartPositions.get(tileCountAndCommon);
						for (int j = 0 ; j < list.size() ; j ++) {
							long l = list.get(j);
							boolean isForwardStrand =  ! NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
							short tilePositionInSequence = NumberUtils.getShortFromLong(l, TILE_OFFSET);
							long positionInGenome = NumberUtils.getLongPositionValueFromPackedLong(l);
							long positionInGenomeEnd = positionInGenome + tileCount + (TILE_LENGTH - 1);
							
							/*
							 * see if there are any possible ranges
							 */
							List<int[]> ranges = getPossibleTileRanges(seqLength, tilePositionInSequence, TILE_LENGTH, tileCount, MIN_BLAT_SCORE_MINUS_RANGE_BUFFER,  ! isForwardStrand);
//								List<int[]> ranges = getPossibleTileRanges(seqLength, tilePositionInSequence, TILE_LENGTH, tileCount, MIN_BLAT_SCORE,  ! isForwardStrand);
//								List<int[]> ranges = getPossibleTileRanges(seqLength, tilePositionInSequence, TILE_LENGTH, tileCount, MIN_BLAT_SCORE - BUFFER,  ! isForwardStrand);
							if ( ! ranges.isEmpty()) {
								/*
								 * deal with 2 scenarios here
								 * First is where we have a single range. In this case if a match is found, and there is still room for another match, another attempt at a match is made
								 * Second case is where we have 2 ranges.
								 * 
								 * Both of these scenarios could result in IntLongPairs that contain 3 IntLongPair objects
								 */
								
								if (ranges.size() == 1) {
									List<IntLongPair> resultsForRange = getPositionsThatFitInRange(ranges.get(0), positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
									if ( ! resultsForRange.isEmpty()) {
										
										IntLongPairs pairs = getILPSFromLists(resultsForRange, null, new IntLongPair(tileCountAndCommon, l));
												
										/*
										 * check to see if we could fit an additional bit of sequence in here
										 */
										int[][] remainingRanges = getRemainingRangeFromIntLongPairs(pairs, seqLength);
										if (remainingRanges.length > 0) {
//												System.out.println("Found " + remainingRanges.length + " remaining ranges for pairs: " + pairs.toString() + ", remaining ranges[0]: " + Arrays.toString(remainingRanges[0]));
											for (int[] remainingRange : remainingRanges) {
												List<IntLongPair> resultsForRemainingRange = getPositionsThatFitInRange(remainingRange, positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
												if ( ! resultsForRemainingRange.isEmpty()) {
													
//														System.out.println("Found " + resultsForRemainingRange.size() + " matches for remaining range: " + Arrays.toString(remainingRange));
													
//														System.out.println("Adding new IntLongPair!");
													IntLongPairsUtil.addBestILPtoPairs(pairs, resultsForRemainingRange);
//														pairs.addPair(resultsForRemainingRange.get(0));
													
													/*
													 * don't want to add any more to this pair
													 */
//														break;
													
//													} else {
//														System.out.println("Found NO matches for remaining range: " + Arrays.toString(remainingRange));
												}
											}
											
											/*
											 * go again....
											 */
											remainingRanges = getRemainingRangeFromIntLongPairs(pairs, seqLength);
											if (remainingRanges.length > 0) {
//													System.out.println("Found " + remainingRanges.length + " remaining ranges for pairs: " + pairs.toString() + ", remaining ranges[0]: " + Arrays.toString(remainingRanges[0]));
												for (int[] remainingRange : remainingRanges) {
													List<IntLongPair> resultsForRemainingRange = getPositionsThatFitInRange(remainingRange, positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
													if ( ! resultsForRemainingRange.isEmpty()) {
														
//															System.out.println("Found " + resultsForRemainingRange.size() + " matches for remaining range: " + Arrays.toString(remainingRange));
														
														/*
														 * sort and take largest again
														 */
//															resultsForRemainingRange.sort(null);
//															System.out.println("Adding new IntLongPair!");
														IntLongPairsUtil.addBestILPtoPairs(pairs, resultsForRemainingRange);
//															pairs.addPair(resultsForRemainingRange.get(0));
														
														/*
														 * don't want to add any more to this pair
														 */
//															break;
														
//														} else {
//															System.out.println("Found NO matches for remaining range: " + Arrays.toString(remainingRange));
													}
												}
											}
											
//											} else {
//												System.out.println("No remaining ranges found for pairs: " + pairs.toString());
										}
										
										/*
										 * check that pairs is not a subset of existing pairs
										 */
										if ( ! IntLongPairsUtil.isPairsASubSetOfExistingPairs(getPairsFromMap(results), pairs)) {
											Set<IntLongPairs> resultsListList = results.putIfAbsent(IntLongPairsUtil.getBasesCoveredByIntLongPairs(pairs, seqLength, TILE_LENGTH), new HashSet<>(Arrays.asList(pairs)));
											if (null != resultsListList) {
												resultsListList.add(pairs);
											}
											checkResults = true;
										}
									}
							
								} else if (ranges.size() == 2) {
									/*
									 * 3 IntLongPair objects here
									 * create the IntLongPairs object with ordered INtLongPair objects - makes the IntLongPairs.equals() and hashcode() valid
									 */
									List<IntLongPair> resultsForRange1 = getPositionsThatFitInRange(ranges.get(0), positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
									List<IntLongPair> resultsForRange2 = getPositionsThatFitInRange(ranges.get(1), positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
									if ( ! resultsForRange1.isEmpty() ||  ! resultsForRange2.isEmpty()) {
										IntLongPairs pairs = getILPSFromLists(resultsForRange1, resultsForRange2, new IntLongPair(tileCountAndCommon, l));
										
										
										/*
										 * check to see if we could fit an additional bit of sequence in here
										 */
										int[][] remainingRanges = getRemainingRangeFromIntLongPairs(pairs, seqLength);
										if (remainingRanges.length > 0) {
//												System.out.println("Found " + remainingRanges.length + " remaining ranges for pairs: " + pairs.toString() + ", remaining ranges[0]: " + Arrays.toString(remainingRanges[0]));
											for (int[] remainingRange : remainingRanges) {
												List<IntLongPair> resultsForRemainingRange = getPositionsThatFitInRange(remainingRange, positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
												if ( ! resultsForRemainingRange.isEmpty()) {
													
//														System.out.println("Found " + resultsForRemainingRange.size() + " matches for remaining range: " + Arrays.toString(remainingRange));
													
													/*
													 * no need to sort as they are added to the list in a sorted manner
													 */
//														resultsForRemainingRange.sort(null);
//														System.out.println("Adding new IntLongPair!");
													IntLongPairsUtil.addBestILPtoPairs(pairs, resultsForRemainingRange);
//														pairs.addPair(resultsForRemainingRange.get(0));
													
													/*
													 * don't want to add any more to this pair
													 */
//														break;
													
//													} else {
//														System.out.println("Found NO matches for remaining range: " + Arrays.toString(remainingRange));
												}
											}
										}
										
										/*
										 * check that pairs is not a subset of existing pairs
										 */
										if ( ! IntLongPairsUtil.isPairsASubSetOfExistingPairs(getPairsFromMap(results), pairs)) {
											Set<IntLongPairs> resultsListList = results.putIfAbsent(IntLongPairsUtil.getBasesCoveredByIntLongPairs(pairs, seqLength, TILE_LENGTH), new HashSet<>(Arrays.asList(pairs)));
											if (null != resultsListList) {
												resultsListList.add(pairs);
											}
											checkResults = true;
										}
//											Set<IntLongPairs> resultsListList = results.putIfAbsent(IntLongPairsUtils.getBasesCoveredByIntLongPairs(pairs, seqLength, TILE_LENGTH), new HashSet<>(Arrays.asList(pairs)));
//											if (null != resultsListList) {
//												resultsListList.add(pairs);
//											}
									}
								}
							}
						}
					}
				}
					
					
				/*
				 * check results, if we have covered all bases in seqLength with our splits, no need to go looking for more
				 */
				if (checkResults &&  ! results.isEmpty()) {
					for (int key : results.keys()) {
						if (key == seqLength) {
							/*
							 * done
							 */
							areWeDone = true;
							break;
						}
					}
					checkResults = false;
				}
			}
		}
		return results;
	}
	
	public static List<IntLongPairs> getPairsFromMap(TIntObjectMap<Set<IntLongPairs>> map) {
		List<IntLongPairs> list = new ArrayList<>();
		map.forEachValue(s -> list.addAll(s));
		return list;
	}
	
	public static IntLongPairs getILPSFromLists(List<IntLongPair> list1, List<IntLongPair> list2, IntLongPair pair) {
		List<IntLongPair> results = new ArrayList<>(4);
		results.add(pair);
		Optional<IntLongPair> list1ILP = getBestILPFromList(list1, pair);
		list1ILP.ifPresent(ilp -> results.add(ilp));
		Optional<IntLongPair> list2ILP = getBestILPFromList(list2, pair);
		list2ILP.ifPresent(ilp -> results.add(ilp));
		results.sort(null);
		return new IntLongPairs(results.toArray(new IntLongPair[]{}));
	}
	
	/**
	 * This will only return an OPtional that is not empty if it is within <code>MAX_GAP_FOR_SINGLE_RECORD</code> bases of the primary <code>IntLongPair</code>
	 * 
	 * @param list
	 * @param p1 Primary <code>IntLongPair</code>
	 * @return
	 */
	public static Optional<IntLongPair> getBestILPFromList(List<IntLongPair> list, IntLongPair p1) {
		/*
		 * check list, if only 1 entry, easy.
		 * If more than one, sort by ability to make single record, tile count, and then by strand and closeness to originating IntLongPair (p1 here)
		 */
		if (null != list && ! list.isEmpty()) {
			if (list.size() > 1) {
			
				/*
				 * sort by tile count, strand, and location
				 */
				boolean p1OnForwardStrand = NumberUtils.isBitSet(p1.getLong(), REVERSE_COMPLEMENT_BIT);
				long p1Position = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
				list.sort((IntLongPair ilp1, IntLongPair  ilp2) -> {
					/*
					 * valid for single record first
					 */
					boolean ilp1Valid = IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p1, ilp1);
					boolean ilp2Valid = IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p1, ilp2);
					if (ilp1Valid && ! ilp2Valid) {
						return -1;
					} else 	if ( ! ilp1Valid && ilp2Valid) {
						return 1;
					}
					
					/*
					 * tile count
					 */
					int [] ilp1Array = NumberUtils.splitIntInto2(ilp1.getInt());
					int [] ilp2Array = NumberUtils.splitIntInto2(ilp2.getInt());
					int diff = 	Integer.compare(ilp2Array[0] - ilp2Array[1], ilp1Array[0] - ilp1Array[1]);
					if (diff == 0) {
						diff = ilp1Array[1] - ilp2Array[1];
					}
					if (diff != 0) {
						return diff;
					}
					
//					int diff = 	NumberUtils.getPartOfPackedInt(ilp2.getInt(), true) - NumberUtils.getPartOfPackedInt(ilp1.getInt(), true);
//					if (diff != 0) {
//						return diff;
//					}
					/*
					 * strand
					 */
					if ( NumberUtils.isBitSet(ilp1.getLong(), REVERSE_COMPLEMENT_BIT) == p1OnForwardStrand
							&& NumberUtils.isBitSet(ilp2.getLong(), REVERSE_COMPLEMENT_BIT) != p1OnForwardStrand) {
						diff = -1;
					} else if ( NumberUtils.isBitSet(ilp2.getLong(), REVERSE_COMPLEMENT_BIT) == p1OnForwardStrand
							&& NumberUtils.isBitSet(ilp1.getLong(), REVERSE_COMPLEMENT_BIT) != p1OnForwardStrand) {
						diff = 1;
					}
					if (diff != 0) {
						return diff;
					}
					/*
					 * proximity to original ILP
					 */
					long ilp1Diff = Math.abs( NumberUtils.getLongPositionValueFromPackedLong(ilp1.getLong()) - p1Position);
					long ilp2Diff = Math.abs( NumberUtils.getLongPositionValueFromPackedLong(ilp2.getLong()) - p1Position);
					return (ilp1Diff < ilp2Diff) ? -1 : 1;
				
				});
			}
			
			/*
			 * if leading candidate is too far away from original entry, return empty optional
			 */
			IntLongPair ilp = list.get(0);
//			long p1Position = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
//			long ilpPosition = NumberUtils.getLongPositionValueFromPackedLong(ilp.getLong());
//			
//			return Math.abs(p1Position - ilpPosition) < MAX_GAP_FOR_SINGLE_RECORD ? Optional.of(ilp) : Optional.empty();
			return Optional.of(ilp);
			
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Takes a map of positions (keyed by tileCount) and returns an ordered list of IntLongPair objects with the largest tileCount first
	 * 
	 * @param positions
	 * @return
	 */
//	public static List<IntLongPair> getIntLongPairFromMap(TIntObjectMap<TLongList> positions) {
//		List<IntLongPair> list = new ArrayList<>(positions.size() * 2);
//		
//		if ( ! positions.isEmpty()) {
//			/*
//			 * streams would be good here....
//			 */
//			positions.forEachEntry((k,v) -> {
//				for (long vLong : v.toArray()) {
//					list.add(new IntLongPair(k, vLong));
//				}
//				return true;
//			});
//			list.sort(null);
//		}
//		return list;
//	}
	
	
	/**
	 * 
	 * @param ilp
	 * @param seqLength
	 * @return
	 */
	public static int[][] getRemainingRangeFromIntLongPairs(IntLongPairs ilp, int seqLength) {
		return getRemainingRangeFromIntLongPairs(ilp, seqLength, TILE_LENGTH);
	}
	public static int[][] getRemainingRangeFromIntLongPairs(IntLongPairs ilp, int seqLength, int tileLength) {
		
		int tileLengthMinusOne = tileLength - 1;
		/*
		 * get tile start position and length for each IntLongPair
		 */
		
		int basesCovered = IntLongPairsUtil.getBasesCoveredByIntLongPairs(ilp, seqLength, tileLength);
		
		if (seqLength - basesCovered > MIN_BLAT_SCORE_MINUS_RANGE_BUFFER) {
			IntLongPair[] pairs = IntLongPairsUtil.sortIntLongPairs(ilp, seqLength);
			
			
			/*
			 * now loop through and add to list if there the gap between ranges or boundaries is large enough
			 */
			List<int[]> ranges = new ArrayList<>(4);
			int lastEndPosition = 0;
			for (IntLongPair pair : pairs) {
				
				int startPosition = NumberUtils.getShortFromLong(pair.getLong(), TILE_OFFSET);
				boolean reverseStrand = NumberUtils.isBitSet(pair.getLong(), REVERSE_COMPLEMENT_BIT);
				int tileCount = NumberUtils.getPartOfPackedInt(pair.getInt(), true);
				if (reverseStrand) {
					int [] forwardStrandStartAndStopPositions = getForwardStrandStartAndStop(startPosition, tileCount, TILE_LENGTH, seqLength);
					startPosition = (short) forwardStrandStartAndStopPositions[0];
				}
				
				if (startPosition - lastEndPosition > MIN_BLAT_SCORE_MINUS_RANGE_BUFFER) {
					ranges.add(new int[]{lastEndPosition, startPosition - 1});
				}
				
				lastEndPosition = startPosition + tileCount + tileLengthMinusOne;
			}
			
			if (seqLength - lastEndPosition > MIN_BLAT_SCORE_MINUS_RANGE_BUFFER) {
				ranges.add(new int[]{lastEndPosition, seqLength});
			}
			return ranges.toArray(new int[][]{});
		}
		
		return new int[][]{};
	}
	
	
	/**
	 * Given a length, a tile start position, the tile length, the tile count and the minimum acceptable size of a chunk, 
	 * this method will return a list of ranges that correspond to the parts of the length that are not covered by this match
	 * 
	 * For example, if the length of the sequence is 100, the match that we are dealing with is 50 in length, starting at position 0
	 * then there will be one range returned and it will start at 50 + tile length (63 typically) and end ot the length - 1.
	 * 
	 * If any unrepresented regions are smaller than the minLength value, then they will not be returned as a range.
	 * It is possible that an empty lit is returned and this would tell that user that there was not space for another match at either the start or end of the sequence.
	 * 
	 * The ranges will be made up of an int array with 2 elements.
	 * The first element is the start position of any potential matching region
	 * The second element is the end position of any potential matching region
	 * 
	 * eg. consider the following:
	 * length = 100
	 * startTilePosition = 30
	 * tileLength = 13
	 * tileCount = 20
	 * minLength = 20
	 * 
	 * In this instance, we should have 2 ranges returned.
	 * The first would correspond to the gap at the beginning of the sequence (int[]{0,29})
	 * and the second would correspond to the gap at the end of the sequence (int[]{30 + 13 + 20 ,99})
	 * 
	 * @param totalLength
	 * @param startTilePosition
	 * @param tileLength
	 * @param tileCount
	 * @param minLength
	 * @return
	 */
	public static List<int[]> getPossibleTileRanges(int totalLength, int startTilePosition, int tileLength, int tileCount, int minLength) {
		return getPossibleTileRanges(totalLength, startTilePosition, tileLength, tileCount, minLength, false);
	}
	public static List<int[]> getPossibleTileRanges(int totalLength, int startTilePosition, int tileLength, int tileCount, int minLength, boolean reverseStrand) {
		
		return getPossibleTileRanges(totalLength, startTilePosition, tileLength + tileCount, minLength, reverseStrand);
//		List<int[]> results = new ArrayList<>(3);
//		
//		/*
//		 * check to see if there is space for a range before the startTilePosition
//		 */
//		int endPosition = startTilePosition + tileLength + tileCount;
//		if (reverseStrand) {
//			/*
//			 * startTilePosition is now end position
//			 * and start position is tileCount + tileLength from the end
//			 */
//			endPosition = totalLength - startTilePosition;
//			startTilePosition = endPosition - tileLength - tileCount;
//		}
//		
//		
//		if (startTilePosition >= (minLength)) {
//			results.add(new int[]{0, startTilePosition - 1});
//		}
//		if (endPosition < (totalLength - minLength)) {
//			results.add(new int[]{endPosition, totalLength});
//		}
//		
//		return results;
	}
	
	public static List<int[]> getPossibleTileRanges(int totalLength, int startPosition, int length, int minLength, boolean reverseStrand) {
		List<int[]> results = new ArrayList<>(3);
		
		/*
		 * check to see if there is space for a range before the startTilePosition
		 */
		int endPosition = startPosition + length;
		if (reverseStrand) {
			/*
			 * startTilePosition is now end position
			 * and start position is tileCount + tileLength from the end
			 */
			endPosition = totalLength - startPosition;
			startPosition = totalLength - (startPosition + length);
//			startPosition = endPosition - tileLength - tileCount;
		}
		
		
		if (startPosition >= (minLength)) {
			results.add(new int[]{0, startPosition - 1});
		}
		if (endPosition < (totalLength - minLength)) {
			results.add(new int[]{endPosition, totalLength});
		}
		
		return results;
	}
	
	public static List<IntLongPair> getPositionsThatFitInRange(int[] range, long genomicPositionStart, long genomicPositionEnd, TIntObjectMap<TLongList> countsAndStartPositions, int [] sortedKeys, int tileLength, int seqLength, int positionInArray) {
//		public static List<IntLongPair> getPositionsThatFitInRange(int[] range, long genomicPositionStart, long genomicPositionEnd, TIntObjectMap<TLongList> countsAndStartPositions, int [] sortedKeys, int tileLength, int seqLength) {
		List<IntLongPair> results = new ArrayList<>();
		int maxTileCount = range[1] - range[0] - (tileLength - 1);
		
		/*
		 * need to add a buffer to the maxTileCount
		 * as there are instances where there is an overlap
		 */
		
		int bufferToUse = (int)(seqLength  * 0.4) + 1;
//		int bufferToUse = (int)(seqLength  * 0.35);
//		int bufferToUse = seqLength > 120 ? BUFFER * 7 : BUFFER;
		
		maxTileCount += bufferToUse;
		
		for (int i = positionInArray ; i >= 0 ; i--) {
//			for (int i = sortedKeys.length - 1 ; i >= 0 ; i--) {
			int tileCount = NumberUtils.getPartOfPackedInt(sortedKeys[i], true);
//			if (tileCount >= MIN_TILE_COUNT) {
				if (tileCount >= MIN_TILE_COUNT && tileCount <= maxTileCount) {
				
				/*
				 * with the tile count, now need to work out possible start positions
				 */
//				int minTileStartPosition = range[0];
//				int maxTileStartPosition = Math.max(minTileStartPosition, range[1] - (tileCount + tileLength) + 1);
				
				TLongList list =  countsAndStartPositions.get(sortedKeys[i]);
				for (int j = 0 ; j < list.size() ; j ++) {
					long l = list.get(j);
					boolean reverseStrand = NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
					short tileStartPosition = NumberUtils.getShortFromLong(l, TILE_OFFSET);
					
					
					if (reverseStrand) {
						int [] forwardStrandStartAndStopPositions = getForwardStrandStartAndStop(tileStartPosition, tileCount, tileLength, seqLength);
						tileStartPosition = (short) forwardStrandStartAndStopPositions[0];
						
					}
					if (doesPositionFitWithinRange(range, tileStartPosition, tileCount, bufferToUse)) {
						/*
						 * now need to check to see if the genomic coordinates have too great an overlap
						 * This happens when sequences contain repetitive regions
						 */
						long thisGenomicPositionStart = NumberUtils.getLongPositionValueFromPackedLong(l);
						long thisGenomicPositionEnd = thisGenomicPositionStart + tileCount + (tileLength - 1);
						
						int buffer = (int)Math.max((thisGenomicPositionEnd - thisGenomicPositionStart) / 2, (genomicPositionEnd - genomicPositionStart) / 2);
						
						if ( ! doGenomicPositionsOverlap(thisGenomicPositionStart, thisGenomicPositionEnd, genomicPositionStart, genomicPositionEnd, buffer)) {
							/*
							 * we have a keeper!
							 */
							results.add(new IntLongPair(sortedKeys[i], l));
						}
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * If the overlap is more than 50% of both ranges, return true
	 * 
	 * @param positionOneStart
	 * @param positionOneEnd
	 * @param positionTwoStart
	 * @param positionTwoEnd
	 * @param buffer
	 * @return
	 */
	public static boolean doGenomicPositionsOverlap(long positionOneStart, long positionOneEnd, long positionTwoStart, long positionTwoEnd, int buffer) {
		long overlap = NumberUtils.getOverlap(positionOneStart, positionOneEnd, positionTwoStart, positionTwoEnd);
		
		if ((positionOneEnd - positionOneStart) - overlap <= TILE_LENGTH) {
			return true;
		}
		
//		if ((float) overlap / (positionOneEnd - positionOneStart) < 0.5) {
//			return false;
//		}
//		if ((float) overlap / (positionTwoEnd - positionTwoStart) < 0.5) {
//			return false;
//		}
//		return true;	
				
		return overlap >= buffer;
	}
	
	public static int getRangesOverlap(int [] range, int range2Start, int range2End) {
		long overlap = NumberUtils.getOverlap(range[0], range[1], range2Start, range2End);
		return (int)overlap;
	}
	
	public static boolean doesPositionFitWithinRange(int [] range, int startPosition, int tileCount, int buffer) {
		int rangesOverlap = getRangesOverlap(range, startPosition, startPosition + tileCount + (TILE_LENGTH - 1));
		/*
		 * no point in adding this position if it is not going to increase the coverage of the pairs
		 */
		if (rangesOverlap <= TILE_LENGTH) {
			return false;
		}
		
		return startPosition >= (range[0] - buffer)
				&& (startPosition + tileCount + TILE_LENGTH - 1) <= (range[1] + buffer);
	}
	
	public static int[] getForwardStrandStartAndStop(int tileStartPositionRS, int tileCount, int tileLength, int seqLength) {
		return getForwardStrandStartAndStop(tileStartPositionRS, tileCount, tileLength, seqLength, false);
	}
	public static int[] getForwardStrandStartAndStop(int tileStartPositionRS, int tileCount, int tileLength, int seqLength, boolean forwardStrand) {
		if (forwardStrand) {
			return new int[] {tileStartPositionRS, tileStartPositionRS + (tileCount + tileLength - 1)};
		} else {
			return new int[] {seqLength - (tileStartPositionRS + tileCount + tileLength - 1), seqLength - tileStartPositionRS};
		}
	}
	
	/**
	 * returns exact match tile count AND common occurring tile count
	 * @param packedInt
	 * @return
	 */
	public static int getLengthFromPackedInt(int packedInt) {
		return getLengthFromPackedInt(packedInt, TILE_LENGTH);
	}
	public static int getLengthFromPackedInt(int packedInt, int tileLength) {
		int tileCounts = NumberUtils.minusPackedInt(packedInt);
		return tileCounts + tileLength - 1;
	}
	
	/**
	 * Only take into account exact match count
	 * @param packedInt
	 * @return
	 */
	public static int getExactMatchOnlyLengthFromPackedInt(int packedInt) {
		return getExactMatchOnlyLengthFromPackedInt(packedInt, TILE_LENGTH);
	}
	public static int getExactMatchOnlyLengthFromPackedInt(int packedInt, int tileLength) {
		int tileCounts = NumberUtils.getPartOfPackedInt(packedInt, true);
		return tileCounts + tileLength - 1;
	}
}

