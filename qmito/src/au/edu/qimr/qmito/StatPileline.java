/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.meta.QLimsMeta;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.stat.inference.*;


public class StatPileline {
	
	private QLogger logger = QLoggerFactory.getLogger(getClass());

	private String outputFile;
	private String inputControl;
	private String inputTest;
	
	private List<BaseStatRecord> control;
	private List<BaseStatRecord> test;	  

	private StatOptions options;
	public StatPileline(StatOptions options) throws Exception {
		inputTest = options.getTestMetricFileName();
		inputControl = options.getControlMetricFileName();				 
		outputFile = options.getOutputFileName();
		
		this.options = options;		
		
		//read metric
 		 control = readIn(inputControl);
 		 test = readIn(inputTest);
		
 		 if(control.size() != test.size())
 			 throw new Exception("Two input metric files contains differnt line number ( position number)");
		 
		//create
		long[] a1 = {0,1};
		long[] a2 = {1, 0};
		
		 
		
			ChiSquareTest chiTest = new ChiSquareTest();
			double out1 = 0, out2=0;
		try{
			out1 = chiTest.chiSquareTestDataSetsComparison (a1, a2);
			out2 = chiTest.chiSquareTestDataSetsComparison (a2, a1);
		}catch(ZeroException e){
			
		}
		
		System.out.println( "testing output: " + out1);	
		System.out.println( "testing output: " + out2);
	}
 
	
	public List<BaseStatRecord> readIn(String fileName) throws Exception{
		
		List<BaseStatRecord> counts = new ArrayList<BaseStatRecord>();
		//skip all comment
		BufferedReader reader = new  BufferedReader(new FileReader(fileName));
		String line, column[];
		while( (line = reader.readLine()) != null ){
			if(line.startsWith("#")) continue;
			if(line.startsWith("Reference")) continue;
			if(line.length() < 10) continue;			
			column = line.split("\t");
			BaseStatRecord record = new BaseStatRecord();
			record.ref = column[0];
			record.position = Integer.parseInt( column[1]);
			record.ref_base =  column[2].charAt(0);
			record.setForward(BaseStatRecord.Base.BaseA, Integer.parseInt( column[3]));			
			record.setForward(BaseStatRecord.Base.BaseC,  Integer.parseInt(column[4]));
			record.setForward(BaseStatRecord.Base.BaseG, Integer.parseInt(column[5]));
			record.setForward(BaseStatRecord.Base.BaseT, Integer.parseInt(column[6]));
			
			record.setReverse(BaseStatRecord.Base.BaseA, Integer.parseInt(column[33]));
			record.setReverse(BaseStatRecord.Base.BaseC, Integer.parseInt(column[34]));
			record.setReverse(BaseStatRecord.Base.BaseG, Integer.parseInt(column[35]));
			record.setReverse(BaseStatRecord.Base.BaseT, Integer.parseInt(column[36]));	
			
			counts.add(record);
		}
		
		reader.close();
		
		return counts;
	}

	
	/**
	 * it output all pileup datasets into tsv format file
	 * @param output: output file name with full path
	 * @throws Exception 
	 */
 	public void report() throws Exception{
 		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false));
		
		//headlines
 		createHeader(writer);

 		String line;
 		double controlChi, testChi, pairChi;
 		ChiSquareTest chiTest = new ChiSquareTest();
 		long[] conFor, conRsv,testFor,testRsv;
		 long[] total_control = new long[4];
		 long[] total_test = new long[4]; 		
 		int i,j;
 		for( i = 0; i < control.size(); i ++){
 			//suspending if two inputs are not in same order
 			if(! control.get(i).ref.equalsIgnoreCase(test.get(i).ref) ||
 				 control.get(i).position != test.get(i).position ||
 				 control.get(i).ref_base != test.get(i).ref_base){
 				line = "Two inputs are not in same order on lines:\n";
 				line += String.format("%s\t%d\t%s\n", control.get(i).ref, control.get(i).position, control.get(i).ref_base);
 				line += String.format("%s\t%d\t%s\n", test.get(i).ref, test.get(i).position, test.get(i).ref_base);				
 				throw new Exception(line);
 				}
  			
 			conFor = control.get(i).getForwardArray();
 			conRsv = control.get(i).getReverseArray();
 			testFor = test.get(i).getForwardArray();
 			testRsv =test.get(i).getReverseArray();
 			
  			 controlChi = chiTest.chiSquareTestDataSetsComparison (conFor, conRsv);
 			 testChi = chiTest.chiSquareTestDataSetsComparison (testFor, testRsv);
			 
 			 //compare bases from both strand
 			 for(j = 0; j < 4; j++){
 				 total_control[j] = conFor[j] + conRsv[j];
 			 	 total_test[j] = testFor[j] + testRsv[j];
 			 }
 
 			 pairChi = chiTest.chiSquareTestDataSetsComparison (total_control, total_test);
 			 
 			 //output
 			line = String.format("%s\t%d\t%s\t", control.get(i).ref, control.get(i).position, control.get(i).ref_base);			
 			line += control.get(i).getForwardString() + "\t";
 			line += control.get(i).getReverseString() + "\t";
			line += controlChi + "\t";
			line += test.get(i).getForwardString() + "\t";
 			line += test.get(i).getReverseString() + "\t";
			line += testChi + "\t";			
			line += pairChi + "\n";
			writer.write(line);
 		}
		
 	  
		writer.close();			
	}
 
 	//create header lines
 	private void createHeader(BufferedWriter writer) throws Exception{
		QExec qexec = options.getQExec();
		writer.write(qexec.getExecMetaDataToString());
		
		char[] baseName = {'A','C','G','T'};
		String line = "Reference\tPosition\tRef_base\t";
		
		//control data
		for (int i = 0; i < 4; i++)
			line += "con_" + baseName[i] + "_for\t";
		for (int i = 0; i < 4; i++)
			line += "con_" + baseName[i] + "_rev\t";
		line += "con_chi_forVSrev\t";
		
		//test data
		for (int i = 0; i < 4; i++)
			line += "test_" + baseName[i] + "_for\t";
		for (int i = 0; i < 4; i++)
			line += "test_" + baseName[i] + "_rev\t";
		line += "test_chi_forVSrev\t";
		line += "total_chi_conVStest\t";
		
		writer.write(line);
 		
 	}
 	
 }