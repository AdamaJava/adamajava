package org.qcmg.qvisualise2.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.math.SimpleStat;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qvisualise2.ChartTab;
import org.qcmg.qvisualise2.util.GCCoverageUtils;
import org.qcmg.qvisualise2.util.QProfilerCollectionsUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ChartTabBuilderUtils {		    
	   //SEQ, QUAL, Kmers, ISize, RNAME_POS
	   static ChartTab createTabFromPossibleValue( ChartTabBuilder para ) {
		   ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){
				@Override
				public String[] getDataAndInfo( ChartTabBuilder para, Element element, String dataName){	
					if(element.getAttribute(QprofilerXmlUtils.possibles  ) == null ) return null;
					List<String> possibles = Arrays.asList(  element.getAttribute(QprofilerXmlUtils.possibles).split(",")) ;	
									
					Map<Integer, AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();
					QProfilerCollectionsUtils.populateIntegerMap(element, map, para.isValueRange, para.isDataContinue, para.dataFilter);		
					
					final Map<Integer, String> cyclePercentages = ( para.needPercentage) ?
						 QProfilerCollectionsUtils.generatePercentagesMapFromElement( element ) : 
							 ((para.isValueRange)? QProfilerCollectionsUtils.generateRangesMapFromElement( element ) : null);	 
						 
					String s1 = HTMLReportUtils.generateGoogleaArraywithAnnotation(map, cyclePercentages, dataName, false, possibles, para.showCombinedLine);	
					
					//create new parameter
					String source = element.getAttribute("source"); 
					String chartTitle = source + (source.isEmpty() ? "" : " " ) +  para.chartTitle;		
									
					try {
						ChartTabBuilder subPara = (ChartTabBuilder) para.clone();
						subPara = subPara.chartTitle( chartTitle );														
						String s2 = generateGoogleChart( subPara, dataName );	
						return new String[]{ s1, s2 };					
					} catch (CloneNotSupportedException e) { e.printStackTrace(); }
					
					return null; 				
				}  			 
		   };
		   
		   return ChartTabBuilder.createTallyTab(para,data );	   
	   }
	   

	   static ChartTab createTabSmoothy( ChartTabBuilder para ) {	   
		   ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){
				@Override
				public String[] getDataAndInfo( ChartTabBuilder para, Element element, String dataName){	
	 				List<String> possibles = QProfilerCollectionsUtils.getPossibleReference(element);			
					Map<Integer, AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();				
					//QProfilerCollectionsUtils.populateCoverageMapByChromosome(element, map, possibles);
					QProfilerCollectionsUtils.populateSmoothCoverageMapByChromosome(element, map, possibles);
					
					String s1 = HTMLReportUtils.generateGoogleaArraywithAnnotation(map, null, dataName, false, possibles, para.showCombinedLine, true);	
					//create new parameter
					String s2 = generateGoogleChart( para, dataName ) ;					
					return new String[]{s1, s2};				 				
				}	   			 
		   };
		   
		   return ChartTabBuilder.createTallyTab(para,data );	   
	   }
	   
	   
		public static ChartTab createTabGCBinCoverage(ChartTabBuilder para) {
			final List<Double[]> gcCoverage = new ArrayList<>();
			//check all chr element
			for(Element ele: QprofilerXmlUtils.getOffspringElementByTagName(para.baseElement, QprofilerXmlUtils.rangeTally)) 
				//record coverage and related gc percentage
				for(Double[] cov: GCCoverageUtils.generateGCsmoothCoverage( ele ))
					gcCoverage.add(cov);						
			para.description( GCCoverageUtils.getDescriptionOfCorrelation(gcCoverage) );
			
		   ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){
				@Override
				public String[] getDataAndInfo( ChartTabBuilder para, Element element, String dataName ){											
									
					//convert to AtomicLongArray and calculate average
					Map<Double, AtomicLongArray> map = new TreeMap<Double, AtomicLongArray>();
					double weight = 0.0000001; //there r 3m coveragebin, and gcpercent is up to %.2f
					int freq = 0;
					for(Double[] cov : gcCoverage){
						double gc = cov[0] + freq * weight; 
						map.computeIfAbsent(gc, k-> new AtomicLongArray( new long[]{ cov[1].longValue()} ));						 
						freq ++;
					}
					List<String> label = new ArrayList<String>();
					label.add("coverage");
					String s1 = HTMLReportUtils.generateGoogleArray( map, dataName, false, label, para.showCombinedLine, true );	
					String s2 = generateGoogleChart( para, dataName ) ;					
					return new String[]{ s1, s2 };					
				}  			 
		   };	
				   
		   return ChartTabBuilder.createTallyTab( para,data );
		} 

			
	   //debug...
		public static ChartTab createTabGCBinCoverage1(ChartTabBuilder para) {	
			
			List<Double[]> gcCoverage_tmp = new ArrayList<>();
			//check all chr element
			for(Element ele: QprofilerXmlUtils.getOffspringElementByTagName(para.baseElement, QprofilerXmlUtils.rangeTally)) 
				//record coverage and related gc percentage
				for(Double[] cov: GCCoverageUtils.generateGCsmoothCoverage( ele ))
					gcCoverage_tmp.add(cov);			
			final List<Double[]> gcCoverage = SimpleStat.getWithin3STD( gcCoverage_tmp, 0 );
			para.description( GCCoverageUtils.getDescriptionOfCorrelation(gcCoverage) );
			
			 ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){
				@Override
				public String[] getDataAndInfo( ChartTabBuilder para, Element element, String dataName ){							
					
					//clusterring the covrage and then parse to map									
					double[][] cc = new double[ gcCoverage.size() ][2];
					for( int i = 0; i < gcCoverage.size(); i++ ) 
						cc[i] = new double[]{gcCoverage.get(i)[0], gcCoverage.get(i)[1]};
					Map<Double, AtomicLongArray> map = GCCoverageUtils.solve( cc );
				
					List<String> label = new ArrayList<>();
					for(int i = 0; i < map.entrySet().iterator().next().getValue().length(); i++)
						label.add( i + "th group" );
					
					String s1 = HTMLReportUtils.generateGoogleArray( map,  dataName, false, label, para.showCombinedLine, true );	
					String s2 = generateGoogleChart( para, dataName ) ;					
					return new String[]{ s1, s2 };	
				}					
			 };
			 return ChartTabBuilder.createTallyTab( para,data );
			
		}	

		
		/**
		 * create tab where one chart list coverage for each chromosome 
		 * @param para
		 * @return
		 */
	   static ChartTab createTabCoverageByChr( ChartTabBuilder para ){
		   ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){
				@Override
				public String[] getDataAndInfo( ChartTabBuilder para, Element element, String dataName){
					List<String> possibles = QProfilerCollectionsUtils.getPossibleReference(element);			
					Map<Integer, AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();				 
					QProfilerCollectionsUtils.populateSmoothCoverageMapByChromosome(element, map, possibles);
					String s1 = HTMLReportUtils.generateGoogleaArraywithAnnotation(map, null, dataName, false, possibles, para.showCombinedLine, true);	
					//create new parameter
					String s2 = generateGoogleChart( para, dataName ) ;					
					return new String[]{s1, s2};					
				}			
		   };	
				
		   return ChartTabBuilder.createTallyTab(para,data );
	   }
	  
	   static ChartTab createTabWithGCPercent( ChartTabBuilder para ) {
		   ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){
				@Override
				public String[] getDataAndInfo( ChartTabBuilder para, Element element, String dataName ){	
					
					//get data column name 
					if(element.getAttribute(QprofilerXmlUtils.possibles  ) == null ) return null;
					List<String> possibles = Arrays.asList(  element.getAttribute(QprofilerXmlUtils.possibles).split(",")) ;	
									
					Map<Integer, AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();
					QProfilerCollectionsUtils.populateIntegerMap(element, map, para.isValueRange, para.isDataContinue, para.dataFilter);		
					
					//get GC percent
					final Map<Integer, String> gcPercentages = GCCoverageUtils.generateGCAnnotationIfMatch(element) ;
					//String s1 = HTMLReportUtils.generateGoogleaArraywithAnnotation(map, gcPercentages, dataName, false, possibles, para.showCombinedLine);				
					String s1 = HTMLReportUtils.generateGoogleaArraywith2ndVAxis( map, gcPercentages, dataName, false, possibles,"GC%", para.showCombinedLine, true );	
					//String s1 = HTMLReportUtils.generateGoogleaArraywithDuplicatedKey( map, gcPercentages, dataName, false, possibles,"GC%", para.showCombinedLine, true );		
					//create new parameter
					String source = element.getAttribute("source"); 
					String chartTitle = source + (source.isEmpty() ? "" : " " ) +  para.chartTitle;		
									
					try {
						ChartTabBuilder subPara = (ChartTabBuilder) para.clone();
						subPara = subPara.chartTitle( chartTitle );
						int lastColumn = 	(para.showCombinedLine)? possibles.size() + 1 : possibles.size();
						if(gcPercentages != null)						
							subPara = subPara.vAxis_2nd(ChartTabBuilder.defaultgcPercen_vAxis).series( "{ " + lastColumn + ": " + ChartTabBuilder. defaultSeriees_bar +" }");
									 											
						String s2 = generateGoogleChart( subPara, dataName );	
		
						return new String[]{ s1, s2 };					
					} catch ( CloneNotSupportedException e ) { e.printStackTrace(); }
					
					return null; 				
				}  			 
		   };	   
		
		   return ChartTabBuilder.createTallyTab(para,data );	    
	   }   

		//seq, qual length , RNEXT, MD Mutation Forward Strand, 
		static ChartTab createTabFromValueTally( ChartTabBuilder para ) {		
			   ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){			    
				   public String[] getDataAndInfo(ChartTabBuilder par, Element element, String dataName){
					   Map< ? , AtomicLong> tally =  (par.isValueInt ) ? new LinkedHashMap<Integer, AtomicLong>()
							   : new LinkedHashMap<String, AtomicLong>();
					    QProfilerCollectionsUtils.populateTallyItemMap(element, tally, para.isValueInt, para.dataFilter); 					
						if(para.isSortMapByValue) tally = QProfilerCollectionsUtils.sortTallyItemMapByValue(tally);					 
					    String s1 = HTMLReportUtils.generateGoogleData(tally, dataName, !para.isValueInt);	
					    				    
	 				    String source = element.getAttribute("source"); 
					    String chartTitle = source + (source.isEmpty() ? "" : " " ) +  para.chartTitle;					   				    
					    
						try {
							ChartTabBuilder subPara = (ChartTabBuilder) para.clone().chartTitle(chartTitle);
							String s2 = generateGoogleChart( subPara, dataName ) ;	
							return new String[]{s1,s2};
						} catch (CloneNotSupportedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return null;
				   }
			   };
			   
			   return ChartTabBuilder.createTallyTab(para,data );			
		}		
			
		static ChartTab createMDPercentageTab( ChartTabBuilder para ){
			ChartTabBuilder.DataAndInfo data = new ChartTabBuilder.DataAndInfo(){			    
			   public String[] getDataAndInfo(ChartTabBuilder par, Element element, String dataName){
					final Map<Integer, String> cyclePercentages = 
							QProfilerCollectionsUtils.generatePercentagesMapFromElement( element );				
						Map<Integer, Double> sortedPercentageMap = new TreeMap<>();			
						for (Entry<Integer, String> entry : cyclePercentages.entrySet()) {										
							String value = entry.getValue();
							double percentage = Double.parseDouble(value.substring(0, value.length() - 1));					
							if (percentage > 1.0) sortedPercentageMap.put(entry.getKey(), percentage);				 
						}	
										
					    String source = element.getAttribute("source"); 
					    String chartTitle = source + (source.isEmpty() ? "" : " " ) +  "<BR>Error Percentage";					
						String s1 = HTMLReportUtils.generateGoogleData(sortedPercentageMap, dataName ,false, "Cycle", chartTitle);				    				    	   
					    Integer height = (sortedPercentageMap.size() > 26 )? null : 0;					
						String s2 = HTMLReportUtils.generateGoogleSingleTable(dataName, height , 200 );							
						return new String[]{s1,s2};
				   }
			   };
			   
			   return ChartTabBuilder.createTallyTab(para,data);					
		}

		//not sure whether should be deprecated since can't find related tag from xml
		static ChartTab createMatrixChartTab( ChartTabBuilder para ){
			Map<MAPQMiniMatrix, AtomicLong> tallys = generateMatrixCollection(para.baseElement, para.tagName);
			ChartTab ct = null; 
			if ( ! tallys.isEmpty()) {
				ct = new ChartTab(para.chartTitle, para.id);
				ct.setData(HTMLReportUtils.generateGoogleMatrixData(tallys, ct.getName(), true));
				ct.setChartInfo(HTMLReportUtils.generateGoogleBioHeatMap(ct.getName(),
						para.chartTitle + " BioHeatMap", tallys.size() > 1000 ? 12 : 18));
			}
			return ct;
		}

		static <T> ChartTab getChartTabFromMap( ChartTabBuilder para, Map<T, AtomicLong> cycleCount){	
			ChartTab ct = new ChartTab(para.tabTitle, para.id);		 
			int width = ReportBuilder.MIN_REPORT_WIDTH;
			int height = ReportBuilder.MIN_REPORT_HEIGHT;
			// set width and height depending on dataset size
			if (cycleCount.size() > 25) {
				width = ReportBuilder.MAX_REPORT_WIDTH;
				height = ReportBuilder.MAX_REPORT_HEIGHT;
				if (HTMLReportUtils.BAR_CHART.equals(para.chartType)) {
					width = ReportBuilder.MAX_REPORT_WIDTH - 100;
					height = ReportBuilder.MAX_REPORT_HEIGHT + 300;
				}
			}	
		
			ct.setData( HTMLReportUtils.generateGoogleData(cycleCount, ct.getName(), !para.isValueInt) );		
			if (HTMLReportUtils.SCATTER_CHART.equals(para.chartType)) 			
				ct.setChartInfo( HTMLReportUtils.generateGoogleScatterChart( ct.getName(), para.tabTitle + " Tally", width, height,  para.islogScale) );			
			else  			
				ct.setChartInfo( HTMLReportUtils.generateGoogleChart( ct.getName(), para.tabTitle + " Tally", width,height, para.chartType, para.islogScale, para.isStacked ) );
			 		
			return ct;
		}

		static String generateGoogleChart(ChartTabBuilder para, String dataName) {
			   StringBuilder sb = new StringBuilder();
			   
			   String chartName =  dataName + "Chart";	
			   final String endChart = "});";
			   			   
			   sb.append( String.format("\nvar %s = new google.visualization.%s(document.getElementById('%s'));", chartName, para.chartType, chartName+"_div"));			   
			   String width = (para.widthString != null)? para.widthString : para.width + "";
			   String height = (para.heightString != null)? para.heightString : para.height + "";
			   
			   sb.append(chartName)
			   		.append(String.format(".draw(%s, {width: %s, height: %s, title: '%s'",dataName, width, height, para.chartTitle))
					.append(", chartArea:{ left:100, top:40, width:\"75%\", height:\"75%\"} ");
			   
			   if( para.pointSize != null) sb.append(", pointSize: ").append(para.pointSize);
			   if( para.lineWidth != null) sb.append(", lineWidth: ").append(para.lineWidth);
			   if( para.dataOpacity != null) sb.append(", dataOpacity: ").append(para.dataOpacity);
			   			
			   if( para.isStacked) sb.append(", isStacked: true");
			   if( para.explorer != null) sb.append(", explorer: { " + para.explorer + " }"); 
				
			   if (ChartTabBuilder.PIE_CHART.equals( para.chartType))  
					return sb.append(", is3D: 'true'").append(endChart).toString();
			   else if (ChartTabBuilder.SCATTER_CHART.equals(para.chartType)) 
				   sb.append(", pointSize: 1 ");
				   		   
			  //color and font
			   if(para.colours != null && para.colours.length > 0){		  
				   sb.append(", colors:[");
				   for (String colour : para.colours)  
						sb.append("'").append(colour).append("',");	
				   sb.delete(sb.length()-1, sb.length()).append("]");
				   if(para.colours.length > 10)
						sb.append(", legendFontSize: 10" );
			   }
			   	 sb.append(", fontSize:12");
			   		
			   String countTitle = ( para.islogScale ? "'Log(Count)' ": "'Count'" ) + ", titleColor: 'blue', logScale: " + para.islogScale ;
			   		   		   
			   String hview = "";
			   String join = (para.hx_minValue != null && para.hx_maxValue != null)	? "," : "";		  
			   if(  para.hx_minValue != null  || para.hx_maxValue != null)	 		 
				   hview = String.format(" viewWindow: {%s%s%s},", (para.hx_minValue == null)? "" : "min: " + para.hx_minValue ,
						   join,   (para.hx_maxValue == null)? "" : "max: " + para.hx_maxValue);
			   		   		   
			   String vview = "";
			   join = (para.vx_minValue != null && para.vx_maxValue != null)? "," : "";		  
			   if(  para.vx_minValue != null  || para.vx_maxValue != null)	
				   vview = String.format(" viewWindow: {%s%s%s},",  (para.vx_minValue == null)? "" : "min: " + para.vx_minValue  ,
						   	join, (para.vx_maxValue == null)? "" : "max: " + para.vx_maxValue);
			   
			   String hformat = "";
			   if( para.hx_format != null)
				   hformat = String.format("format: '%s', ", para.hx_format );
				
			   String hAxis = null, vAxis = null;
 			  if (ChartTabBuilder.BAR_CHART.equals(para.chartType)) {
					//SEQ QUAL MD-mismatch v-axid: cycle h-axid: count
					//MD v-axid: value(A->G), h-axid:count
					//flag v-axid: value(flag:p1,0001,0000), h-axid log(count)
					hAxis = String.format("{ %s %s title: %s }", hformat, hview,  countTitle);
					vAxis = String.format("{title: '%s', titleColor: 'blue' , gridlines: {color: 'transparent'}}", para.vx_title );
				
			  } else{
					//COLUMN_CHART :SEQ QUAL length bad reads h:value v:logcount
					//line chart: kmers h:cycle v:count
					//MAPD: h: value v: logcount
					//RNAM (coverage) h: value(chr position); v:logcount
					//scater: isize(TLENG) h: value v:logcount
					String style = (para.widthString == null && para.width > 1000 )? ", textStyle:{fontSize:9}" : "";
					hAxis = String.format("{ %s title: '%s', titleColor: 'blue' %s, gridlines: {color: 'transparent'} }", hformat, para.hx_title, style );	 
					vAxis = String.format("{ %s title: %s }",vview, countTitle);										
				}			   
			    sb.append(", hAxis: ").append(hAxis); //single hAxis
			    			    
			    //The default line type for any series not specified in the series property.
			    //Available values are 'line', 'area', 'bars', 'candlesticks', and 'steppedArea'.
			    //default is 'line' 
			    if(ChartTabBuilder.COMBO_CHART.equals(para.chartType) ) 
			    	 sb.append(", seriesType: 'scatter'  "); 
			    
			   //dural y use vAxes, single use vAxis   		   
			   if(para.vAxis_2nd != null)
				   sb.append(", vAxes: ").append( String.format("{ 0: %s, 1: %s  }", vAxis, para.vAxis_2nd) );
			   else
				   sb.append(", vAxis: ").append(vAxis);
			   
			   if(para.series != null)
				   sb.append(", series: " + para.series );
			   			   
			  return sb.append(", legend: " + para.legend).append(endChart).toString();			   
		   }  	
			
		static Map<Integer, AtomicLong> createRangeTallyMap(Element element) {
			Map<Integer, AtomicLong> map = new TreeMap<Integer, AtomicLong>();
			final NodeList nl = element.getElementsByTagName("RangeTallyItem");
			for (int i = 0, size = nl.getLength() ; i < size ; i++) {
				Element e = (Element) nl.item(i);			
				int start = Integer.parseInt(e.getAttribute("start"));
				int end = Integer.parseInt(e.getAttribute("end"));
				Integer key = Math.round((float)(start + end) / 2);			
				map.put(key, new AtomicLong(Integer.parseInt(e.getAttribute("count"))));
			}
			
			return map;
		}
			
		static Map< Integer, AtomicLongArray > createRgCountMap(Element element) {	
			Map<Integer,AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();
			final NodeList nl = element.getElementsByTagName("RangeTallyItem");
			for (int i = 0, size = nl.getLength() ; i < size ; i++) {
				Element e = (Element) nl.item(i);
				
				int start = Integer.parseInt(e.getAttribute("start"));
				int end = Integer.parseInt(e.getAttribute("end"));
				Integer key = Math.round((float)(start + end) / 2);
				
				String[] sValues = e.getAttribute("counts").split(",");
				long[] nValues = new long[sValues.length+1];
				for(int j = 0; j < sValues.length-1; j ++)
					nValues[j+1] = Long.parseLong(sValues[j].replace(" ", ""));
				nValues[0] = Long.parseLong(e.getAttribute("count")); 			//total coverage
					
				map.put(key, new AtomicLongArray( nValues));
			}
			
			return map;
		}

		static Map<Integer, AtomicLong> generateLengthsTally(Element tabElement,String name) {
			final Map<Integer, AtomicLong> lengths = new LinkedHashMap<>();		
			if (null == tabElement) return lengths;
					 
			NodeList nl = tabElement.getElementsByTagName(name);
			if (nl.getLength() == 0) 
				nl = tabElement.getElementsByTagName(name + "NEW");		 
			Element nameElement = (Element) nl.item(0);
			QProfilerCollectionsUtils.populateTallyItemMap( nameElement, lengths,  true, null );
			 
			return lengths;	 
		}
			
		static Map<MAPQMiniMatrix, AtomicLong> generateMatrixCollection(Element tabElement, String name) {
			
			final Map<MAPQMiniMatrix, AtomicLong> lengths = new LinkedHashMap<MAPQMiniMatrix, AtomicLong>();		
			if (null != tabElement) {
				NodeList nl = tabElement.getElementsByTagName(name);
				//FIXME hack to get around the "NEW" tag added to lengths
				if (nl.getLength() == 0) {
					nl = tabElement.getElementsByTagName(name + "NEW");
				}
				Element nameElement = (Element) nl.item(0);
				
				QProfilerCollectionsUtils.populateMatrixMap(nameElement, lengths);
			}
			return lengths;
		}

		static Map<Integer, AtomicLong> generateValueTally(Element tabElement,String name) {
			final NodeList nl = tabElement.getElementsByTagName(name);
			final Element nameElement = (Element) nl.item(0);
			return generateLengthsTally(nameElement, "ValueTally");
		}

}
