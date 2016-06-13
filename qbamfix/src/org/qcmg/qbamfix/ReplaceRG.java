/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
 

import htsjdk.samtools.SAMReadGroupRecord;

public class ReplaceRG {
	
	private enum TAG { ID, CN, DS,DT,FO,KS,LB,PG,PI,PL,PU,SM } 
	
	static String default_CN = "QCMG";
	static String default_PL = "ILLUMINA";
	static String default_SM = "UNKNOWN";
	private SAMReadGroupRecord createdRG;
	
 	//for unit test
	public ReplaceRG(){}
	
	
	public ReplaceRG(SAMReadGroupRecord preRG, HashMap<String, String> optRG,
		String inputName)throws Exception{
		File input = new File(inputName);
		
		 
		if(optRG.containsKey( TAG.ID.toString()))
			createdRG = createRG(optRG.get(TAG.ID.toString()),preRG);
		else
			createdRG = createRG(input.lastModified() ,preRG);
	
		createdRG = replaceRG(createdRG, optRG, input);
		
	}
	
	
	
	SAMReadGroupRecord createRG(long tt,SAMReadGroupRecord preRG) throws Exception{	
		SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddhhmmss");
		String id = sdf.format( tt );	
		Random generator = new Random();
		id += Integer.toString( generator.nextInt(1000) );
		
		return createRG(id, preRG);
	}
	
	SAMReadGroupRecord createRG(String id,SAMReadGroupRecord preRG) throws Exception{		
				
		SAMReadGroupRecord newRG;
		if(preRG == null)
			newRG =   new SAMReadGroupRecord(id);
		else
			newRG =   new SAMReadGroupRecord(id, preRG);
		
		return newRG;
	}
	
	SAMReadGroupRecord replaceRG(SAMReadGroupRecord inRG, 
			HashMap<String, String> optRG, File fBAM) throws Exception{
		
		if(optRG.containsKey(TAG.CN.toString()))
			inRG.setSequencingCenter(optRG.get(TAG.CN.toString()));	
		else if(inRG.getSequencingCenter() == null) 
			inRG.setSequencingCenter(default_CN);
		
		if(optRG.containsKey(TAG.DS.toString()))
			inRG.setDescription(TAG.DS.toString());
		
		if(optRG.containsKey(TAG.DT.toString())){
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
			Date date =  formatter.parse( optRG.get(TAG.DT.toString()));  
			inRG.setRunDate(date); 
		}
				
		if(optRG.containsKey(TAG.FO.toString()))
			inRG.setFlowOrder(optRG.get(TAG.FO.toString()));
		
		if(optRG.containsKey(TAG.KS.toString()))
			inRG.setKeySequence(optRG.get(TAG.KS.toString()));
		
		if(optRG.containsKey(TAG.LB.toString()))
			inRG.setLibrary(optRG.get(TAG.LB.toString()));	
		else if(inRG.getLibrary() == null)			 
				 throw new Exception(ReplaceRG.class.getName() + ": please specify a LB value for read group");
			 		
		if(optRG.containsKey(TAG.PG.toString()))
			inRG.setAttribute(TAG.PG.toString(), optRG.get(TAG.PG.toString()));
		else if( inRG.getAttribute("PG") == null)			 
				 inRG.setAttribute(TAG.PG.toString(),Messages.getProgramName());
			 

		if(optRG.containsKey(TAG.PI.toString()))
			try{
				inRG.setPredictedMedianInsertSize(Integer.parseInt(optRG.get(TAG.PI.toString())));
			}catch(Exception e){
				System.err.println(ReplaceRG.class.getName() + ": non Integer value specified on PI tag on option: --RG PI:  " 
						+ optRG.get(TAG.PI.toString()));					   
			}
		
		if(optRG.containsKey(TAG.PL.toString()))
			inRG.setPlatform(optRG.get(TAG.PL.toString()));
		else if( inRG.getPlatform() == null)		 
				 inRG.setPlatform(default_PL);
	
		if(optRG.containsKey(TAG.PU.toString()))
			inRG.setPlatformUnit(optRG.get(TAG.PU.toString()));	
		else if(inRG.getPlatformUnit() == null ){
				String[] names = fBAM.getName().split("\\.");
				if(names.length >3)
					inRG.setPlatformUnit(names[1] + "." + names[2]);
				else if(names.length == 3) 
					inRG.setPlatformUnit(names[1]);
		}
		  		
		if(optRG.containsKey(TAG.SM.toString()))
			inRG.setSample(optRG.get(TAG.SM.toString()));
		else if(inRG.getSample() == null)		 
				inRG.setSample(default_SM);
					
		return inRG;
	}
	
	public List<SAMReadGroupRecord> getReadGroupList(){
		
		List<SAMReadGroupRecord> rglist = new ArrayList<SAMReadGroupRecord>();
		
		rglist.add(createdRG);
		return rglist;
	}
	
   public SAMReadGroupRecord getReadGroup(){
		
 
		return createdRG;
	}

}
