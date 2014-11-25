package au.edu.qimr.qannotate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import ca.mcgill.mcb.pcingola.snpEffect.commandLine.*;

public class OptionsTest {

	@Test
	public void test1(){
		String[] args = {"-i", "input", "-m", "mode", "-o", "output"};
		
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(args));
	//	List<String> list = ;
		//list.remove(2);
		//System.out.println("remove 2 :" + Arrays.toString(list.toArray()));
		
		for(int i = 0 ; i < list.size(); i ++)
			if(list.get(i).equalsIgnoreCase("-m")){
				System.out.println(i +" find: " + list.get(i));
				list.remove(i);
				System.out.println(i +" find: " + list.get(i));
				list.remove(i);
			}
		
		System.out.println("bf:" + Arrays.toString(args));
		System.out.println("af:" + Arrays.toString(list.toArray()));
		
		int x = Integer.parseInt("1100110", 2);
		int y = 0b1100110;
	//	System.out.println( y + " af parse" + x);
		
	}
	
	@Test
	public void test2(){
		String[] args = {"eff", "-dataDir", "/Users/christix/Documents/tools/snpEff/data1",
				"-config","/Users/christix/Documents/tools/snpEff/snpEff.config", 
				"-stats", "/Users/christix/Documents/Eclipse/data/TP53_snpeff.uniq.output.vcf.snpEff_summary.html",
				"GRCh37.70", "/Users/christix/Documents/Eclipse/data/TP53_snpeff.uniq.vcf"};
		
//		SnpEff.main(args);
		
		String filter = "OK;PASS;YES;PASS";
		filter = "PASS";
		filter = filter.replaceAll("PASS;|;?PASS$", "");
		System.out.println("after replace: " + filter);
 
	}
	
}
