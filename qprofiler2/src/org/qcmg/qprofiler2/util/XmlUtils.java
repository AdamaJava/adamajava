package org.qcmg.qprofiler2.util;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.util.XmlElementUtils;
import org.w3c.dom.Element;

import htsjdk.samtools.AbstractSAMHeaderRecord;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceRecord;

public class XmlUtils {
	public static final String OTHER = "others";
	public static final String UNKNOWN_READGROUP = "unkown_readgroup_id";	
	
	public static final String METRICS = "Metrics";
	public static final String READGROUPS ="readGroups";
	public static final String VARIABLE_GROUP = "variableGroup";
	public static final String VALUE = "value";
	public static final String CLOSED_BIN = "closedBin";
	public static final String SEQUENCE_METRICS = "sequence" + METRICS;
	public static final String NAME = "name";
	public static final String COUNT = "count";
	public static final String PERCENT = "percent";
	public static final String TALLY = "tally";
	public static final String TALLY_COUNT = "tallyCount";
	public static final String START = "start";
	public static final String END = "end";
	public static final String CYCLE = "cycle";
	public static final String BASE_CYCLE = "baseCycle";
	public static final String RECORD ="record";
	public static final String DISCARD_READS = "discardedReads";		
	public static final String SEQ_BASE = "seqBase";
	public static final String SEQ_LENGTH = "seqLength";
	public static final String BAD_READ = "badBase";
	public static final String QUAL_BASE = "qualBase";
	public static final String QUAL_LENGTH = "qualLength";	
	public static final String FIRST_PAIR = "firstReadInPair"; 
	public static final String SECOND_PAIR = "secondReadInPair";

	public static final String BAM_SUMMARY = "bamSummary";
	public static final String OVERALL = "Overall";
	public static final String ALL_BASE_LOST = "OverallBasesLost";
	 	
	//commly used on fastq bam
	public static final String QNAME = "QNAME";
	public static final String FLAG = "FLAG";	
	public static final String RNAME = "RNAME";
	public static final String POS = "POS";
	public static final String MAPQ = "MAPQ";
	public static final String CIGAR = "CIGAR";
	public static final String TLEN = "TLEN";
	public static final String SEQ = "SEQ"; 
	public static final String QUAL = "QUAL";
	public static final String TAG = "TAG";	
	
   public static void bamHeaderToXml(Element parent1, SAMFileHeader header, boolean isFullBamHeader){

        if ( null == header) return;
        Element parent = XmlElementUtils.createSubElement(parent1, "bamHeader");

        String cateName = "TAG"; //output bam header classified by "TAG"
        //create header line
        List<String> headers = new ArrayList<>();
        headers.add( String.format("@HD VN:%s GO:%s SO:%s", header.getVersion(), header.getGroupOrder(), header.getSortOrder()));
        createHeaderRecords(parent,cateName, "HD", "The header line", headers);

        //<HeadRecords Category="SQ" Describe="Sequence">
        //<HeadRecord>@SQ       SN:chr1 LN:249250621</HeadRecord>
        createHeaderRecords(parent,cateName,  "SQ", "Reference sequence dictionary", header.getSequenceDictionary().getSequences());
      
        if(  isFullBamHeader) {
	        //RG
	        createHeaderRecords(parent, cateName, "RG", "Read group", header.getReadGroups());
	        //PG
	        createHeaderRecords(parent, cateName, "PG", "Program", header.getProgramRecords());
	        //CO
	        createHeaderRecords(parent, cateName, "CO", "Text comment", header.getComments());
        }
    }

    private static <T> void createHeaderRecords(Element parent, String cateName, String cateValue, String des, List<T > records) {
            Element element = XmlElementUtils.createSubElement(parent, "headerRecords" );
            if(records != null && !records.isEmpty()) {
                    element.setAttribute( cateName, cateValue);
                    element.setAttribute( "description", des);
                    for(T re: records) {
                            Element elechild = XmlElementUtils.createSubElement(parent, RECORD );
                            //set txt content
                            if(re instanceof String)
                                    elechild.setTextContent((String)re);
                            else if(re instanceof AbstractSAMHeaderRecord)
                                    elechild.setTextContent(((AbstractSAMHeaderRecord)re).getSAMString());
                            else if(re instanceof VcfHeaderRecord)
                                    elechild.setTextContent(((VcfHeaderRecord)re).toString());

                            //set id
                            if (re instanceof SAMSequenceRecord) {
                                    elechild.setAttribute(NAME, ((SAMSequenceRecord)re).getSequenceName()  );
                            }else if (re instanceof SAMReadGroupRecord) {
                                    elechild.setAttribute(NAME, ((SAMReadGroupRecord)re).getId()  );
                            }else if (re instanceof SAMProgramRecord) {
                                elechild.setAttribute(NAME, ((SAMProgramRecord)re).getId()  );
                             //   elechild.setAttribute(Sname, ((SAMProgramRecord)re).getProgramName()  );
                                
                            }else if(re instanceof VcfHeaderRecord) {
                                    elechild.setAttribute(NAME,((VcfHeaderRecord) re).getId() != null ? ((VcfHeaderRecord) re).getId(): ((VcfHeaderRecord) re).getMetaKey().replace("##", "") );
                            }
                            element.appendChild(elechild);
                    }
            }
    }

    public static void vcfHeaderToXml(Element parent1, VcfHeader header){

        if ( null == header) return;
        String cateName = "FIELD"; //output vcf header classified by "FIELD"
        
        Element parent = XmlElementUtils.createSubElement(parent1, "vcfHeader");
         
        //the last header line with #CHROM
        List<String> headers = new ArrayList<>();
        headers.add( header.getChrom().toString() );
        createHeaderRecords(parent, cateName , "headerline", "The header line", headers );
                        
        //information line with key=value pair
        createHeaderRecords(parent, cateName , "MetaInformation", "Meta-information lines", header.getAllMetaRecords());

        //get all structure record
        HashMap<String, List<VcfHeaderRecord>> others = new HashMap<>();
        for(final VcfHeaderRecord re: header ){
            if(re.getId() == null) continue;
            String cate = re.getMetaKey().replace("##", "");
            List<VcfHeaderRecord> children = (others.containsKey(cate))? others.get(cate) : others.getOrDefault(cate, new ArrayList<VcfHeaderRecord>());
            children.add(re);
            others.put(cate, children);
        }

        for(Entry<String, List<VcfHeaderRecord>>  entry: others.entrySet())
            createHeaderRecords(parent, cateName, entry.getKey(), entry.getKey() + " field", entry.getValue());
    }      
                       
	/**
	 * <sequenceMetric count="100"  rgid="id">... </category>
	 * @param parent
	 * @param name category name
	 * @param id readgroup id. set to null if not exists
	 * @return
	 */       
    public static Element createMetricsNode(Element parent,  String name, Pair<?, ?> totalCounts ) {	
    	Element ele = XmlElementUtils.createSubElement( parent,  XmlUtils.SEQUENCE_METRICS );
    						
		if( totalCounts != null ) ele.setAttribute((String)totalCounts.getLeft(), String.valueOf( totalCounts.getRight()));		 
		if(name != null) ele.setAttribute( NAME, name );
				
		return ele;        	
    }      
        
	/**
	 * <variableGroup name="name" >... </category>
	 * @param parent
	 * @param name category name
	 * @param id readgroup id. set to null if not exists
	 * @return
	 */
    public static Element createGroupNode(Element parent, String name) {
    	Element ele = XmlElementUtils.createSubElement( parent,  XmlUtils.VARIABLE_GROUP );	
    	ele.setAttribute( NAME, name); 
    	return ele;
    }
    public static Element createGroupNode(Element parent, String name, Number totalcount) {
    	Element ele = createGroupNode(  parent,   name);
    	 ele.setAttribute( COUNT, String.valueOf(totalcount));  
    	 return ele; 
    }
    
	/**
	 * <variableGroup name="name" >... </category>
	 * @param parent
	 * @param name category name
	 * @param id readgroup id. set to null if not exists
	 * @return
	 */
    public static Element createCycleNode(Element parent, String name) {
    	Element ele = XmlElementUtils.createSubElement( parent,  BASE_CYCLE );	
    	ele.setAttribute( CYCLE, name); 
    	return ele;
    }
        
    /**
     * output <value name="name">value</value>
     * @param parent
     * @param name
     * @param value
     */
    public static <T> Element outputValueNode(Element parent, String name, Number value) {        	
    	Element ele = XmlElementUtils.createSubElement(parent, VALUE);
    	ele.setAttribute(NAME, name);
    	String v = String.valueOf(value);

    	if( value instanceof Double ) {
    		v = String.format("%,.2f", (double)value);
    	}
    	
    	ele.setTextContent( v );    
    	return ele;
    }
    
    public static Element outputBins( Element parent, String name, Map<Integer, AtomicLong> bins, int binSize) {
    	
    	Element cateEle = XmlUtils.createGroupNode(parent, name);
    	cateEle.setAttribute("binSize", binSize+"" );
    	Element ele;
    	for (Entry<Integer, AtomicLong> entry : bins.entrySet() ) {
    		if(entry.getValue().get() > 0) {
    			ele = XmlElementUtils.createSubElement(cateEle, CLOSED_BIN);
    	    	ele.setAttribute(START,  String.valueOf(entry.getKey() * binSize));   	    	
    	    	ele.setAttribute(COUNT, String.valueOf(entry.getValue().get())); 			
    		}  	    		
    	}
    	
    	return cateEle;   	
    }
	
	public static <T> void outputTallyGroupWithSize(Element parent, String name, Map<T, AtomicLong> tallys, int sizeLimits) {
		boolean hasPercent = tallys.size() > sizeLimits ? false : true;		
		Element ele = outputTallyGroup(parent, name, tallys, hasPercent, true) ;		
		
		if( ele != null && tallys.size() > sizeLimits) { 			
			ele.appendChild( ele.getOwnerDocument().createComment( "here only list top "+ sizeLimits + " tally element" ) );	
			ele.setAttribute(XmlUtils.TALLY_COUNT, sizeLimits + "+");		
		}
	}
    
    
    private static <T> void outputTallys( Element ele, String name, Map<T, AtomicLong> tallys, boolean hasPercent) {
    	
    	double sum = hasPercent ? tallys.values().stream().mapToDouble( x -> (double) x.get() ).sum() : 0;	   	   	
    	for(T t: tallys.keySet()) {
			//skip zero value for output
			if(tallys.get(t).get() == 0 ) continue;
			double percent = (sum == 0)? 0 : 100 * (double)tallys.get(t).get() / sum;
			Element ele1 = XmlElementUtils.createSubElement( ele, TALLY );
			ele1.setAttribute( VALUE, String.valueOf( t ));
			ele1.setAttribute( COUNT, String.valueOf( tallys.get(t).get() )); 
			if( hasPercent == true) {
				ele1.setAttribute(PERCENT, String.format("%,.2f", percent));	
			}					
		}  	    	
    }
   
    public static  <T> Element outputTallyGroup( Element parent, String name, Map< T, AtomicLong> tallys, boolean hasPercent, boolean outputSum ) {
    	if(tallys == null || tallys.isEmpty()) return null;
    	
    	Element ele = createGroupNode( parent, name);	//<category> 
    	outputTallys( ele, name, tallys, hasPercent);
    	
    	if(outputSum) {
	    	long counts = tallys.values().stream().mapToLong( x -> (long) x.get() ).sum() ;	
	    	ele.setAttribute(COUNT, counts+"");
    	}
    	
    	return ele;   	
    }
    
    
    public static <T>  void outputCycleTallyGroup( Element parent, String name, Map<T, AtomicLong> tallys, boolean hasPercent ) {
    	if(tallys == null || tallys.isEmpty()) return;
    	       	
    	Element ele = XmlElementUtils.createSubElement( parent,  BASE_CYCLE );	
    	ele.setAttribute( CYCLE, name); 
 
    	outputTallys(  ele,  name,  tallys,  hasPercent );  
    	long counts = tallys.values().stream().mapToLong( x -> (long) x.get() ).sum() ;	
    	ele.setAttribute(COUNT, counts+"");
    }
    
    public static void addCommentChild(Element ele, String comment) {
    	if(ele.getFirstChild() == null) return;
    	ele.insertBefore( ele.getOwnerDocument().createComment( comment), ele.getFirstChild());        	
    }

    public static Element createReadGroupNode( Element parent, String rgid) {
     	Element ele = XmlElementUtils.createSubElement( parent, "readGroup" );
    	ele.setAttribute(NAME, rgid);
    	return ele;
    }
    
	/**
	 *  
	 * @param map Map<String, AtomicLong>
	 * @param key key string of map
	 * @return true if it is a new key added
	 */
	public static boolean updateMapWithLimit(Map<String, AtomicLong> map , String key, int limitSize) {
		
		if(map == null) return false; 
		
		boolean isNew = false;
		String key1 = key;	 	
		if(!map.containsKey(key1)) { 
			if(map.size() >= limitSize ) {key1 = XmlUtils.OTHER;}
			else{isNew = true; }
		} 
		
		map.computeIfAbsent(key1, k -> new AtomicLong()).incrementAndGet();
		 
		return isNew; 
	}	     

}
