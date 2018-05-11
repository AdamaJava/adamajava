package org.qcmg.qvisualise2.report;

import org.w3c.dom.Element;

import java.util.List;

import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qvisualise2.ChartTab;
import org.qcmg.qvisualise2.util.GCCoverageUtils;

public class ChartTabBuilder implements Cloneable{
	
	public static final String BAR_CHART = "BarChart";
	public static final String COLUMN_CHART = "ColumnChart";
	public static final String PIE_CHART = "PieChart";
	public static final String SCATTER_CHART = "ScatterChart";
	public static final String AREA_CHART = "AreaChart";
	public static final String HEAT_MAP_CHART = "HeatMap";
	public static final String TABLE_CHART = "Table";
	public static final String LINE_CHART = "LineChart";
	public static final String COMBO_CHART = "ComboChart";
	public static final String AXIS_FORMAT_SHORT = "short";
		
    public static String defaultLegend = "{ position: 'right', textStyle: { color: 'blue' }}" ; //kmers and MAPQ
    public static String windowWidth = "$(window).width()";
    public static String windowHeight = "$(window).height()";
    public static String defaultZoomInExplorer = "maxZoomIn:0.01, keepInBounds: true, actions: ['dragToZoom', 'rightClickToReset'] ";
    public static String defaultgcPercen_vAxis = "{ viewWindow: { max:2 }, color: '#333', count: 1, format: \"#%\" }";
    public static String defaultgcBin_vAxis = "{ viewWindow: { max:5 }, color: '#333', count: 1 }";
    public static String defaultSeriees_bar = "{ targetAxisIndex:1, type: 'bars', color: '#333',  dataOpacity: 0.2,}";
      
        protected final Element baseElement;
        protected final String tagName;  
        protected final String tabTitle;
        protected final String id;
        protected String chartTitle; //default to tabTitle
        protected String widthString = null; 
        protected String heightString = null; 
        protected String hx_title = "Value";
        protected String vx_title = "Value";
        protected String hx_format = null; 
        protected String series = null; 
         
        protected String legend = "'none'"; 
        protected String explorer = null;      
        protected String description = null;
        protected Boolean descriptionPopup = false; 
        protected String chartType = null;   
        protected String vAxis_2nd = null; //set second vaxis if requires
                
        protected int width = ReportBuilder.MIN_REPORT_WIDTH; //800
        protected int height = ReportBuilder.MIN_REPORT_HEIGHT;  //540
        protected Integer hx_minValue = null;
        protected Integer vx_minValue = null;
        protected Integer hx_maxValue = null;
        protected Integer vx_maxValue = null; 
              
        protected Double pointSize = null;
        protected Double lineWidth = null;
        protected Double dataOpacity = null;
             
        protected boolean isValueInt = false;
        protected boolean needPercentage = false; 
        protected boolean islogScale = false;
        protected boolean isStacked = false;   
        protected boolean isSortMapByValue = false;
        protected boolean isChartLeftRight = false; 
        protected boolean showCombinedLine = false;
        protected boolean isValueRange = false;
        protected boolean isDataContinue = false; 
                
        protected String[] colours = null;
 
        public ChartTabBuilder(Element baseElement, String tagName, String tabTitle , String id){
        	this.baseElement = baseElement;
        	this.tagName = tagName; 
        	this.tabTitle = tabTitle;
        	this.id = id; 
        	this.chartTitle = tabTitle; //default
        }
        
        public ChartTabBuilder(String tabTitle, String id){ this(null, null, tabTitle, id); }       
        
        @Override
        public ChartTabBuilder clone() throws CloneNotSupportedException{  return (ChartTabBuilder) super.clone();  }
                
        public ChartTabBuilder setValueInt(){ this.isValueInt = true; return this; }
        public ChartTabBuilder setlogScale(){ this.islogScale = true; return this; } 
        public ChartTabBuilder setStacked(){ this.isStacked = true; return this; }
        public ChartTabBuilder setSortMapByVaue(){ this.isSortMapByValue = true; return this; }
        public ChartTabBuilder setPercentage(){ this.needPercentage = true; return this; }
        public ChartTabBuilder setChartLeftRight(){ this.isChartLeftRight = true; return this; }
        public ChartTabBuilder setShowCombinedLine(){ this.showCombinedLine = true; return this; }
        public ChartTabBuilder setValueRange(){ this.isValueRange = true; return this; }
        public ChartTabBuilder setDataContinue(){ this.isDataContinue = true; return this; }               
		public ChartTabBuilder description(String str, boolean isPopUp){ 
			this.descriptionPopup = isPopUp;  this.description = str; return this; 
		}
		public ChartTabBuilder description(String str ){ this.description = str; return this; }
		public ChartTabBuilder chartTitle(String str){ this.chartTitle = str;    return this;  }
        public ChartTabBuilder chartType(String str){ this.chartType = str;    return this;  }
        public ChartTabBuilder colours(String[] colour){ this.colours = colour; return this; }
        public ChartTabBuilder width(int num){ this.width = num; return this; } 
        public ChartTabBuilder width(String str){ this.widthString = str; return this;}
        public ChartTabBuilder height(String str){ this.heightString = str; return this;}
        public ChartTabBuilder height(int num){ this.height = num; return this; } 
        public ChartTabBuilder HAxisMinValue(int num){ this.hx_minValue = num; return this; }
        public ChartTabBuilder VAxisMinValue(int num){ this.vx_minValue = num; return this; }
        public ChartTabBuilder HAxisMaxValue(int num){ this.hx_maxValue = num; return this; }
        public ChartTabBuilder VAxisMaxValue(int num){ this.vx_maxValue = num; return this; }
        public ChartTabBuilder vAxis_2nd(String str){ this.vAxis_2nd = str; return this; }
        
        public ChartTabBuilder PointSize(double value){ this.pointSize = value; return this; }
        public ChartTabBuilder LineWidth(double value){ this.lineWidth = value; return this; }
        public ChartTabBuilder DataOpacity(double value){ this.dataOpacity = value; return this; }
                
        public ChartTabBuilder HAxisTitle(String str){ this.hx_title = str; return this; }
        public ChartTabBuilder VAxisTitle(String str){ this.vx_title = str; return this; }
        public ChartTabBuilder HAxisFormat(String str){ this.hx_format = str; return this; }               
        public ChartTabBuilder legend(String str) {this.legend = str; return this; }
        public ChartTabBuilder explorer(String str) {this.explorer = str; return this; }
        public ChartTabBuilder series(String str){this.series = str; return this; }
       
                 
        protected Filter dataFilter = null;
        public ChartTabBuilder setDataFilter(Filter filter){ this.dataFilter = filter;  return this; }    
        
        public Filter parentTagFilter = null;
        public ChartTabBuilder setParentTagFilter(Filter filter){ this.parentTagFilter = filter;  return this; }    
    
    
   public interface Filter{	 public  boolean result(Element element);  }   
      
   public static Filter filterOfGCRef = ( Element element ) -> {
		//only check <TallyItem>; return true if not, that means skip the filter process
		if( !element.getNodeName().equalsIgnoreCase("RangeTally")){ return false; }
		//report all reference start with chr, or convert1-23 to chr1-chr23, which listed on gcPercent.properties
		String ref = IndelUtils.getFullChromosome(element.getAttribute("source"));
		if( GCCoverageUtils.getGCPercentByString(ref) != null ) return true; 
		return false;					
	};
   
	   protected interface DataAndInfo {
		   /**
		    * 
		    * @param para
		    * @param elemtn
		    * @param dataName
		    * @return two dimension array of chart data and chart setting
		    */ 
		   public String[]  getDataAndInfo (ChartTabBuilder para, Element element, String dataName);	  
	   }
	   
	   protected static ChartTab createTallyTab( ChartTabBuilder para, DataAndInfo cInfo ){
			//add loop for first and second read
			final ChartTab cotTab = new ChartTab( para.tabTitle, para.id );
			String sData = "", sChart = "";
			
			String renderInfo = "\n<div id=\"" +  cotTab.getId() +  "\" class=\"pane\">";
			if( para.description != null){ 
				cotTab.setDescritionButton("Description", para.description, para.descriptionPopup);
				renderInfo += cotTab.getDescritionButtonHtml();
			}
			
			List<Element> nodes = QprofilerXmlUtils.getChildElementByTagName(para.baseElement, para.tagName);  
			if(nodes == null || nodes.size() == 0) return null; 
			
			if( para.isChartLeftRight){ 	//resize width if aligned multi Chart horizontally 
				para = para.width( para.width / nodes.size() );	
				renderInfo += "<p>";
			}

			for(int t = 0; t < nodes.size(); t ++){
				Element tallyElement = nodes.get(t);				
				if(para.parentTagFilter != null  && !para.parentTagFilter.result(tallyElement) ) 
						continue; 
				
				String dataName = cotTab.getName() + t;
				String[] datas = cInfo.getDataAndInfo( para, tallyElement, dataName );
				if( datas == null || datas.length < 2 ) continue; 
				sData += datas[0];
				sChart += datas[1];
				 
				String classStr = "";
				if( para.isChartLeftRight )
					classStr = ((t%2)==0)? "class=\"left\"" : (((t%2)==1 )? "class=\"right\"" : "");
				renderInfo += "<p "+ classStr + " id=\"" + dataName + "Chart_div\"></p>";		
			}
			
			cotTab.setData( sData );
			cotTab.setChartInfo(sChart );
			if(para.isChartLeftRight)  renderInfo += "</p>";
			cotTab.setRenderingInfo(  renderInfo + "</div>" );	//make left and right at same panel
			return cotTab; 			   	   
	   } 		
}
