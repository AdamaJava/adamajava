/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.qcmg.common.string.StringUtils;

public class FileUtils {
	
	private static final File[] EMPTY_FILE_ARRAY = new File[] {};
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	public static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
		}
	};
	
	
	public static boolean validOutputFile(final String file) {
		if (StringUtils.isNullOrEmpty(file)) return false;
		return validOutputFile(file, null);
	}
	
	public static boolean validOutputFile(final String file, final String extension) {
		if (null != extension && ! isFileTypeValid(file, extension))
			return false;
		
		return canFileBeWrittenTo(file); 
	}
	
	
	public static boolean validInputFile(final String file) {
		return validInputFile(file, null);
	}
	
	public static boolean validInputFile(final String file, final String extension) {
		if (null != extension && ! isFileTypeValid(file, extension))
			return false;
		
		return canFileBeRead(file); 
	}
	
	public static boolean isFileTypeValid(final File f, final String matchExtension) {
		if (null == f)
			throw new IllegalArgumentException("Invalid File!");
		if (null == matchExtension)
			throw new IllegalArgumentException("Invalid Extension!");
		
	        String ext = null;
	        String s = f.getName();
	    int i = s.lastIndexOf('.');
	    
	    if ( ! f.isDirectory() && i > 0 &&  i < s.length() - 1)
		    	ext = s.substring(i+1).toLowerCase();
	    
	   	return matchExtension.equals(ext);
	  }
	
	public static boolean isFileTypeValid(final String fileName, final String matchExtension){
		return isFileTypeValid(new File(fileName), matchExtension);
	}
	
	public static boolean canFileBeRead(final File file) {
		return file.canRead();
	}
	public static boolean canFileBeRead(final String file) {
		if (null == file) throw new IllegalArgumentException("Null string passed to canFileBeRead");
		return canFileBeRead(new File(file));
	}
	
	public static boolean canFileBeWrittenTo(final File file) {
		// check that file exists
		// if not, check that parent directory exists and is writable
		if (file.exists()) {
			return file.canWrite();
		} else {
			// get parent directory
			File parentDir = file.getParentFile();		
			
			if (null == parentDir) parentDir = new File(System.getProperty("user.dir") );		
			return null != parentDir ? parentDir.canWrite() : false;
//			return parentDir.canWrite();			
		}
	}
	public static boolean canFileBeWrittenTo(final String file) {
		if (StringUtils.isNullOrEmpty(file)) return false;
		return canFileBeWrittenTo(new File(file));
	}
	
	public static boolean isFileEmpty(final String file) {
		return isFileEmpty(new File(file));
	}
	public static boolean isFileEmpty(final File file) {
		return file.length() == 0l;
	}
	
	/**
	 * Returns the index File corresponding to the supplied bam file name (if it exists)
	 * <p>
	 * Specifically, return the file with the same name as the supplied bam file name and 
	 * the additional ".bai" extention, or if the ".bam" extention has been replaced with ".bai"
	 * If no such file exists, return <code>null</code>
	 * 
	 * @param bamFile String bam file for which the corresponding index file is being sought
	 * @return File relating to the index file, <code>null</code> if not found (or doens't exist)
	 */
	public static File getBamIndexFile(final String bamFile) {
		if (null == bamFile) return null;
		
		File index = new File( bamFile.replace("bam", "bai"));
		
		if (null != index && index.exists()) {
			return index;
		} else {
			index = new File( bamFile + ".bai");
			return null != index && index.exists() ? index : null;
		}
	}
	
	public static boolean isFileGZip(final File file) {
		return isFileTypeValid(file, "gz") || isFileTypeValid(file, "gzip");
	}
	
	/**
	 * Returns a collection of Files from a string representing a directory that passes the supplied name filter
	 * If the string representing a directory turns out in fact to be a file, return the file (wrapped in an array)
	 * If the string representing a directory turns out not to be a directory, thrown an exception
	 * 
	 * 
	 * @param directoryOrFile String corresponding to a directory structure to be searched, or a file to be returned
	 * @param filter String containing the filefilter to be applied
	 * @return File[] containing matching files in the supplied directory
	 */
	public static File [] findFilesEndingWithFilter(final String directoryOrFile, final String filter, final boolean recurse) {
		if (StringUtils.isNullOrEmpty(directoryOrFile))
			throw new IllegalArgumentException("Empty directory or filename passed to findFilesEndingWithFilter");
		if (StringUtils.isNullOrEmpty(filter))
			throw new IllegalArgumentException("Empty filter passed to findFilesEndingWithFilter");
		
		final File dir = new File(directoryOrFile);
		
		// if directoryOrFile is a file - just return the file in a File array
		if (dir.isFile()) {
			if (dir.getName().contains(filter)) return new File[] {dir};
			else return EMPTY_FILE_ARRAY;
		}
		
		// if directoryOrFile is not a directory - throw exception
		if (! dir.isDirectory()) throw new IllegalArgumentException("Supplied name is not a directory or a file: " + directoryOrFile);
		
		List<File> files = new ArrayList<File>();
		File[] mafFiles = dir.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File file, String name) {
				File tmpFile = new File(file + FILE_SEPARATOR + name);
				return (tmpFile.isFile() && name.endsWith(filter)) ;
			}
		});
		
		if (null != mafFiles)
			files.addAll(Arrays.asList(mafFiles));
		
		if (recurse) {
			
			File[] potentialMafDirectories = dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}
			});
			
			if (null != potentialMafDirectories) {
				for (File f : potentialMafDirectories) {
					files.addAll(Arrays.asList(findFilesEndingWithFilter(f.getAbsolutePath(), filter, true)));
				}
			}
		}
		
		return files.toArray(EMPTY_FILE_ARRAY);
	}
	public static File [] findFilesEndingWithFilter(final String directoryOrFile, final String filter) {
		return findFilesEndingWithFilter(directoryOrFile, filter, false);
	}
	
	public static File [] findDirectories(final String directoryOrFile, final String filter, final boolean recurse) {
		if (StringUtils.isNullOrEmpty(directoryOrFile))
			throw new IllegalArgumentException("Empty directory passed to findDirectories");
		if (StringUtils.isNullOrEmpty(filter))
			throw new IllegalArgumentException("Empty filter string passed to findDirectories");
		
		
		final File dir = new File(directoryOrFile);
		
		// if directoryOrFile is a file - just return the file in a File array
		if (dir.isFile()) return new File[] {dir};
		
		// if directoryOrFile is not a directory - throw exception
		if (! dir.isDirectory()) throw new IllegalArgumentException("Supplied name is not a directory or a file: " + directoryOrFile);
		
		List<File> files = new ArrayList<File>();
		File[] mafFiles = dir.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File file, String name) {
//				System.out.println("file: " + file.getAbsolutePath() + ", name: " + name);
				File tmpFile = new File(file + FILE_SEPARATOR + name);
				return (tmpFile.isDirectory() && name.equals(filter)) ;
			}
		});
		
		files.addAll(Arrays.asList(mafFiles));
		
		if (recurse) {
			
			File[] potentialMafDirectories = dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}
			});
			
			for (File f : potentialMafDirectories) {
//				System.out.println("about to recurse in...");
				files.addAll(Arrays.asList(findDirectories(f.getAbsolutePath(), filter, true)));
			}
		}
		
		return files.toArray(EMPTY_FILE_ARRAY);
	}
	
	public static List<File> findFilesEndingWithFilterNIO(final String path, final String filter) throws IOException {
		if (StringUtils.isNullOrEmpty(path)) throw new IllegalArgumentException("Path argument to findFilesEndingWithFilterNIO is null or empty");
		if (StringUtils.isNullOrEmpty(filter)) throw new IllegalArgumentException("Filter argument to findFilesEndingWithFilterNIO is null or empty");
		
		final List<File> foundFiles = new ArrayList<File>();
		
		Path startingDir = Paths.get(path);
		Files.walkFileTree(startingDir, new SimpleFileVisitor<Path>() {
			
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException ioe) {
//				System.out.println("Could not visit file: " + file.toString() + " due to " + ioe.getClass().getSimpleName());
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (Files.isReadable(file) && (file.toString().endsWith(filter) || file.toString().endsWith(filter + ".gz"))) {
//					if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && file.toString().endsWith(filter)) {
					foundFiles.add(file.toFile());
				}
				return FileVisitResult.CONTINUE;
			}
		});
		
		return foundFiles;
	}
	
	/**
	 * 
	 * @param directory
	 * @param filter
	 * @return
	 */
	public static File [] findFiles(final String directory, FilenameFilter filter) {
		if (StringUtils.isNullOrEmpty(directory))
			throw new IllegalArgumentException("Empty directory passed to findFiles");
		if (null == filter)
			throw new IllegalArgumentException("Empty filter passed to findFiles");
		
		final File dir = new File(directory);
		
		// if directoryOrFile is a file - just return the file in a File array
		if (dir.isFile()) return new File[] {dir};
		
		// if directoryOrFile is not a directory - throw exception
		if (! dir.isDirectory()) throw new IllegalArgumentException("Supplied name is not a valid directory or file: " + directory);
		
		return dir.listFiles(filter);
	}

}
