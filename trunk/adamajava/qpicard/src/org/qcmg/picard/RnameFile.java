/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import htsjdk.samtools.BAMIndex;

public class RnameFile {
	/**
	 * picard index name created by replacing .bam to .bai. But we want to have index name end with .bam.bai
	 * @param bamFile
	 * @throws Exception 
	 */
	public static void renameIndex(File bamFile) throws Exception{
		String path = bamFile.getPath();
	   	String indexFileBase = bamFile.getPath().endsWith(".bam") ? bamFile.getPath().substring(0, path.lastIndexOf(".")) : path;
	   	
	   	if(! indexFileBase.equals(path)){
	        File indexpicard = new File(indexFileBase + BAMIndex.BAMIndexSuffix);
	        File indexqcmg = new File(path + BAMIndex.BAMIndexSuffix);
			
	        rename(indexpicard, indexqcmg);
        }
	}
	
	public static void rename(File org, File des) throws Exception{
		Path Porg = Paths.get(org.getPath());
		Path Pdes = Paths.get(des.getPath());
		if( ! org.exists()){
			throw new Exception( "can't rename file, since file not exist: " +org.getPath() );
		}
		//do nothing if both File are instance of same real file
		if( org.getPath().equals(des.getPath())) return;
		
		//rename files
		try{
			Files.move(Porg, Pdes, StandardCopyOption.REPLACE_EXISTING);			
			org.delete();			 
		}catch(Exception e){
			throw new Exception("Exception occured during deleting file: " + org.getPath());
		}
	}
}
