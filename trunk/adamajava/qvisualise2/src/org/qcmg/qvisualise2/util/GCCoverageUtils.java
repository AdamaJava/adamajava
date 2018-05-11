package org.qcmg.qvisualise2.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLongArray;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.qcmg.common.math.SimpleStat;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GCCoverageUtils {
	static private class GC{
		final int start , end; 
		final double content; 		
		GC(String str){
			String[] tmp = str.split(COLON);			
			this.start = Integer.valueOf(tmp[0]);  
			this.end = Integer.valueOf(tmp[1]);  
			this.content =  Double.valueOf(tmp[2] );  
		}
				
		/**
		 * 
		 * @param s: s match the start position 
		 * @param e: e match the end position
		 * @return  related gc percentage value eg. 0.5423; or return -1 if not match 
		 */
		double getContentIfMatch(int s, int e){
			if( s == this.start && e == this.end ) return this.content;
			return -1; 
		}

	}
	
	private static final ResourceBundle gcMessages = ResourceBundle.getBundle( "org.qcmg.qvisualise2.gcPercent" );
	static final String COLON = ":";
	
	  public static List<String> getGCPercentRefs(){
			List<String> possibles = Collections.list(gcMessages.getKeys() );		 
			possibles.sort(new ReferenceNameComparator());
			return possibles; 
	  }
	  
	  private static List<GC> getGCList(final String ref){				 
		  List<GC> list = new ArrayList<>();
		  try{ 
			  String[] lines = gcMessages.getString(ref).split(" ");			 
			  for(int i = 0; i < lines.length; i++ ) 			 				  
				  list.add(	new GC(lines[i]) ); 				   	
		  }catch(Exception e){ }	
		  
		  return list;	  		  	  
	  }
	  
	  /**
	   * 
	   * @param ref
	   * @return an string array of tart, end, and GC percent. eg. 3000000, 3999999, 0.4841 
	   */
	  public static String[][] getGCPercentByString(final String ref){  
	  	try{
	    	String[] lines = gcMessages.getString(ref).split(" ");
	    	String[][] gcs = new String[lines.length][3];		    	 
	    	for(int i = 0; i < lines.length; i++ )
	    		gcs[i] = lines[i].split(COLON);		    	
	    	return gcs;	     	  
	  	}catch( Exception e ){ return null; }		  	
	  }
	  
	  	  
 
			
	/**
	 * get gc percentage which location matches the children element both start and end; 
	 * @param element: <RangeTally ... source="chr1">, the reference name is listed on the "source" attributes. 
	 * @return a map of start position and gc percentage 
	 */  
	  public static Map<Integer, String> generateGCAnnotationIfMatch(Element element ) {
			String ref = IndelUtils.getFullChromosome(element.getAttribute(QprofilerXmlUtils.source) );
			List<GC> gcs = getGCList(ref);
				
			ConcurrentMap<Integer, String> tally = new ConcurrentHashMap<>();
			NodeList nl =element.getChildNodes();	
			for (int i = 0, size = nl.getLength() ; i < size ; i++) {
				if(! (nl.item(i) instanceof Element)) continue; 
				Element ele = (Element) nl.item(i);
				try{
					Integer start = Integer.valueOf(ele.getAttribute( QprofilerXmlUtils.start ));
					Integer end = Integer.valueOf(ele.getAttribute( QprofilerXmlUtils.end));					 
					boolean found = false; 
					for(GC gc : gcs){
						double value = gc.getContentIfMatch(start, end);
						if(   value >= 0 ){
							tally.put(start, value+""); 
							found = true;
							break; //skip from the loop which seeking gc value
						}
					}
					if(!found) tally.put( start, "0.00" );				
				}catch(Exception e){ continue; }
			}
									
			return tally;
		}		  
	  
	/**
	 * get gc percentage which location matches the children element both start and end; 
	 * @param element: <RangeTally ... source="chr1">, the reference name is listed on the "source" attributes. 
	 * @return a list of [gcValue, coverage], it contains duplicated gcValues; 
	 * here gcValue formate is, eg. 34.35 present gc percentage 34.35%
	 */
	  public static  List<Double[]> generateGCsmoothCoverage(Element element ) {
		  //array of start, end, coverage
		  Integer[][] coverage = QProfilerCollectionsUtils.populateSmoothStartCountMap(element);		  
		  List<GC> gcs = getGCList( IndelUtils.getFullChromosome(element.getAttribute(QprofilerXmlUtils.source) ));
		  
		  List<Double[]> gcCoverage = new ArrayList<>();
		  for( Integer[] sec :  coverage)  //for each [start, end, coverage]
			 for(GC gc : gcs){
				double value = gc.getContentIfMatch(sec[0], sec[1]);
				if(   value >= 0 ){ 
					gcCoverage.add(	new Double[]{ value*100, new Double( sec[2] ) } );
					break; //skip from the loop which seeking gc value
				}
			 }		  		
		  
	  	 return gcCoverage;		  
	  }	  
	  
	  /**
	   * 
	   * @param gcCoverage
	   * @return a description string with correlation value 
	   */
	  public static String getDescriptionOfCorrelation( List<Double[]> gcCoverage ){	
		
		double[] aa = new double[ gcCoverage.size() ];
		double[] bb = new double[ gcCoverage.size() ];
		double[][] cc = new double[ gcCoverage.size() ][2];
		for( int i = 0; i < gcCoverage.size(); i++ ){ 
			 aa[i] = (double) gcCoverage.get(i)[0];
			 bb[i] = (double) gcCoverage.get(i)[1];
			 cc[i] = new double[]{ aa[i], bb[i] };
		}
		
		
		double r2 = (new PearsonsCorrelation()). correlation( aa , bb );
		SimpleRegression simpleRegression = new SimpleRegression( true );				
		simpleRegression.addData( cc );		   
		String des = "correlation value between gc content and coverage is " + r2;
		des += ".\nslope = " + simpleRegression.getSlope() + "; intercept = " + simpleRegression.getIntercept();
		  		   
		return des; 		  		  		  		  
	  }
	  
	 /**
	  * 
	  * @return a list a gc percentage from whole genome, here we remove the values are very rare which are bynond mane+/-3std
	  */
	  public static List<Double> getGenomeGCWithin3STD(){
		  List<String> refs = Collections.list(gcMessages.getKeys() );	
		  List<Double> gcs = new ArrayList<>();
		  for(String ref : refs )
			 for( GC gc : getGCList(ref) )  gcs.add(gc.content);
		  
		 // double[] aa = ArrayUtils.toPrimitive( gcs.toArray( new Double[gcs.size()] ) );
		  double mean = SimpleStat.getMean(gcs);
		  double std = SimpleStat.getStd(gcs);
		  
		  List<Double> gc_std = new ArrayList<>();
		  for(double gc : gcs)
			  if(gc > mean - 3 * std && gc < mean + 3 * std)
				  gc_std.add(gc);
		  
		  return gc_std;
	  }
	  		
		
		/**
		 * 
		 * @param dat: data[rec_no][2], first element is x, and second element is y
		 */
		 
	    public static Map<Double, AtomicLongArray> solve(double[][] dat){      
	        //generate project by coverting the data by regression 
	        double[] project = regressionConvertion( dat);	        
	        List<List<Integer>> hills = createHills(  project) ;	 
	    	hills.sort( new Comparator< List<Integer> >(){
	    	    @Override
	    	    public int compare( List<Integer> t, List<Integer> t1) {
	    	        return t1.size() - t.size(); //reversed
	    	    }	    
	    	});
	                    
	        
	        //total coverage are 3000 bins. so average coverage size in each bin is 3000/numBins
	    	int	ave_size = 3000/100;
	        int top_num  = 0  ;     
	        for( List<Integer> h : hills)
	        	if(h.size() >= ave_size ) top_num  ++;
	        	else break;  
	        final int top_numHills = top_num;
	      
	        //store top bin	 
	        Map<Double, AtomicLongArray> map = new TreeMap<>();
	        for(int i = 0; i < top_numHills; i++){
	        	int freq  = 0;      	
	    		for( Integer order :  hills.get(i) ){
	    			double gc = dat[order][0] + 0.000001 * freq;
	        		long cov = (long) dat[order][1];        		
	        		AtomicLongArray array = map.computeIfAbsent(gc, k-> new AtomicLongArray( top_numHills +1  ));
	        		array.addAndGet(i, cov);
	        		freq ++;       		
	    		}
	        	 
	        }
	        
	        //add outside bins to last column top_numHills-th     
	        for(int freq  = 0, i = top_numHills; i < hills.size(); i++) 
	        	for( Integer order :  hills.get(i) ){
	        		double gc = dat[order][0] + 0.000001 * freq;
	        		long cov = (long) dat[order][1]; 
	        		AtomicLongArray array = map.computeIfAbsent(gc, k-> new AtomicLongArray( top_numHills +1  ));
	        		array.addAndGet(top_numHills, cov);
	        		freq ++;       		
	        	}
	                
	        return map;       
	    }
	    
		/**
		 * 
		 * @param dat: a two dimension array, eg. list of (x, y)
		 * @return one dimension array wich convert each element of input by same regression method
		 */
		public static double[] regressionConvertion( double[][] dat){
	        SimpleRegression simpleRegression = new SimpleRegression(true);
	        simpleRegression.addData(dat);
	        double beta = simpleRegression.getSlope();
	        double alpha = simpleRegression.getIntercept();    
	         
	        //covert the data by regression 
	        double[] project = new double[dat.length];
	        for(int i = 0; i < dat.length; i++)      	
	             project[i] =  (dat[i][1] - beta * dat[i][0] - alpha)/Math.sqrt(beta*beta + 1);
	            
	        return project;
		}	    
	    

		static private class Hill {
			final List<Integer> binFrom = new ArrayList<>();
			final List<List<Integer>> bins =   new ArrayList<>();
			
			void addBin( int index, List<Integer> bin ){
				binFrom.add(index);
				bins.add(bin);
			}
			/**
			 * 
			 * @return the number of bins merged from
			 */
			int getBinSize(){ return binFrom.size(); }
			
			void printBinNo(){	System.out.print(binFrom.toString()); }
			
			/**
			 * 
			 * @return the sum of projects from all bins
			 */
			int getProjectSize(){
				int sum = 0;
				for(List<Integer> bin: bins)
					sum += bin.size();	
				return sum;
			}
			
			/**
			 * 
			 * @return a list of project id from all bins; here the project id is identical to original input data id. 
			 */
			List<Integer> getAllProjects(){
				List<Integer> all = new ArrayList<>();
				for(List<Integer> bin: bins)
					all.addAll(bin);
				return all; 
			}
			
		}
		
	    private static List<List<Integer>> createHills(double[] project){	    	
	        //generate bins
	        int numBins = 100;
	        double max = Arrays.stream(project).max().getAsDouble();
	        double min = Arrays.stream(project).min().getAsDouble();
	        double scale = numBins / (max - min + 1);        
	        List<List<Integer>> bins = new ArrayList<List<Integer>>(numBins);
	        for (int i = 0; i < numBins; i++) bins.add(  new ArrayList<Integer>() );        
	        for (int i = 0; i < project.length; i++){
	            int bin_no = (int)((project[i] - min) * scale);            
	            bins.get(bin_no).add(i); 	//add data order to bin
	        }  
	        
	         //merging
	        int	ave_size = 2* 3000/numBins;  
	        List<Hill> hills = new ArrayList<>();
	        Hill hill = new Hill();	        
	        for (int i = 0; i < bins.size(); i++){  
	        	if(hill.getBinSize() == 0) hill.addBin(i, bins.get(i));
	        	if(i < bins.size()-1 && 
	        			(Math.abs(bins.get(i).size() - bins.get(i+1).size()) <= 0.5 * Math.max(bins.get(i).size(), bins.get(i+1).size()) 
	        						|| (bins.get(i).size() > ave_size && bins.get(i+1).size() > ave_size ) ))
	        		hill.addBin(i+1, bins.get(i+1));
	        	else{
	        		hills.add(hill);
	        		hill = new Hill();
	        	}	        		
	        }
	        
	        List<List<Integer>> result = new ArrayList<>();
	        for(Hill h : hills){
	        //	h.printBinNo();
	        //	System.out.println(   h.getBinSize() + " bins contains project number are " + h.getProjectSize());
	        	result.add( h.getAllProjects() );
	        }
	    	return result;
	    }
	  
	  
	  

  
}
