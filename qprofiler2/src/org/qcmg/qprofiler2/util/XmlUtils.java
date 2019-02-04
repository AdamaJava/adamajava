package org.qcmg.qprofiler2.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.w3c.dom.Element;

import htsjdk.samtools.AbstractSAMHeaderRecord;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceRecord;

public class XmlUtils {
	public static String metrics = "Metrics";
	public static String readGroupsEle ="readGroups";
	public static String variableGroupEle = "variableGroup";
	public static String Svalue = "value";
	public static String Sbin = "closedBin";
	public static String metricsEle = "sequence" + metrics;
	public static String Sname = "name";
	public static String Sid = "id";	
	public static String Scount = "count";
	public static String Spercent = "percent";
	public static String Stally = "tally";
	public static String Sstart = "start";
	public static String Send = "end";
	public static String Stype = "Type";
	
	
   public static void bamHeaderToXml(Element parent1, SAMFileHeader header){

        if ( null == header) return;
        Element parent = QprofilerXmlUtils.createSubElement(parent1, "bamHeader");

        String cateName = "TAG"; //output bam header classified by "TAG"
        //create header line
        List<String> headers = new ArrayList<>();
        headers.add( String.format("@HD VN:%s GO:%s SO:%s", header.getVersion(), header.getGroupOrder(), header.getSortOrder()));
        createHeaderRecords(parent,cateName, "HD", "The header line", headers);

        //<HeadRecords Category="SQ" Describe="Sequence">
        //<HeadRecord>@SQ       SN:chr1 LN:249250621</HeadRecord>
        createHeaderRecords(parent,cateName,  "SQ", "sequences", header.getSequenceDictionary().getSequences());

        //RG
        createHeaderRecords(parent, cateName, "RG", "read groups", header.getReadGroups());
        //PG
        createHeaderRecords(parent, cateName, "PG", "program records", header.getProgramRecords());
        //CO
        createHeaderRecords(parent, cateName, "CO", "comment lines", header.getComments());
    }

    private static <T> void createHeaderRecords(Element parent, String cateName, String cateValue, String des, List<T > records) {
            Element element = QprofilerXmlUtils.createSubElement(parent, "headerRecords" );
            if(records != null && !records.isEmpty()) {
                    element.setAttribute( cateName, cateValue);
                    element.setAttribute( "description", des);
                    for(T re: records) {
                            Element elechild = QprofilerXmlUtils.createSubElement(parent, "record" );
                            //set txt content
                            if(re instanceof String)
                                    elechild.setTextContent((String)re);
                            else if(re instanceof AbstractSAMHeaderRecord)
                                    elechild.setTextContent(((AbstractSAMHeaderRecord)re).getSAMString());
                            else if(re instanceof VcfHeaderRecord)
                                    elechild.setTextContent(((VcfHeaderRecord)re).toString());

                            //set id
                            if (re instanceof SAMSequenceRecord) {
                                    elechild.setAttribute("id", ((SAMSequenceRecord)re).getSequenceName()  );
                            }else if (re instanceof SAMReadGroupRecord)
                                    elechild.setAttribute("id", ((SAMReadGroupRecord)re).getId()  );
                            else if(re instanceof VcfHeaderRecord) {

                                    elechild.setAttribute("id",((VcfHeaderRecord) re).getId() != null ? ((VcfHeaderRecord) re).getId(): ((VcfHeaderRecord) re).getMetaKey().replace("##", "") );
                            }
                            element.appendChild(elechild);
                    }
            }
    }

    public static void vcfHeaderToXml(Element parent1, VcfHeader header){

        if ( null == header) return;
        String cateName = "FIELD"; //output vcf header classified by "FIELD"
        
        Element parent = QprofilerXmlUtils.createSubElement(parent1, "vcfHeader");
         
        //the last header line with #CHROM
        List<String> headers = new ArrayList<>();
        headers.add( header.getChrom().toString() );
        createHeaderRecords(parent, cateName , "headerline", "header line", headers );
                        
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
	 * @param name: category name
	 * @param id: readgroup id. set to null if not exists
	 * @return
	 */       
    public static Element createMetricsNode(Element parent,  String name, Number totalcount ) {
    	
    	Element ele = QprofilerXmlUtils.createSubElement( parent,  XmlUtils.metricsEle );
					
		if( totalcount != null ) ele.setAttribute( Scount, String.valueOf(totalcount));  
		 
		if(name != null) ele.setAttribute( Sname, name );
		
		return ele;        	
    }      
        
	/**
	 * <category name="name" >... </category>
	 * @param parent
	 * @param name: category name
	 * @param id: readgroup id. set to null if not exists
	 * @return
	 */
    public static Element createGroupNode(Element parent, String name) {
    	Element ele = QprofilerXmlUtils.createSubElement( parent,  XmlUtils.variableGroupEle );	
    	ele.setAttribute( Sname, name); 
    	return ele;
    }
        
    /**
     * output <value name="name">value</value>
     * @param parent
     * @param name
     * @param value
     */
    public static <T> Element outputValueNode(Element parent, String name, Number value) {        	
    	Element ele = QprofilerXmlUtils.createSubElement(parent, Svalue);
    	ele.setAttribute(Sname, name);
    	String v = String.valueOf(value);

    	if( value instanceof Double ) {
    		v = String.format("%,.2f", (double)value);
    	}
    	
    	ele.setTextContent( v );    
    	return ele;
    }
    /**
     * utput <value name="name" note="comment">value</value>
     * @param parent
     * @param name
     * @param value
     * @param comment
     */
    public static <T> void outputValueNode(Element parent, String name, Number value, String comment) {   
    	parent.insertBefore( parent.getOwnerDocument().createComment( comment ), parent.getFirstChild() ); 
    	outputValueNode( parent,  name,  value);
    	
    	//parent.appendChild( parent.getOwnerDocument().createComment( name + ": " + comment ) );
 	//	Text element = parent.getOwnerDocument().createCDATASection(comment);
	//	parent.appendChild(element);
    	    	 
    }
            
    public static void outputBinNode(Element parent, Number start, Number end, Number count ) {
    	Element ele = QprofilerXmlUtils.createSubElement(parent, Sbin);
    	ele.setAttribute(Sstart,  String.valueOf(start));
    	ele.setAttribute(Send,  String.valueOf(end));
    	ele.setAttribute(Scount, String.valueOf(count)); 
    }
   
    public static <T> void outputTallyGroup( Element parent, String name, Map<T, AtomicLong> tallys, boolean hasPercent ) {
    	if(tallys == null || tallys.isEmpty()) return;
    	       	
    	Element ele = createGroupNode( parent, name);	//<category>       	
		double sum = hasPercent ? tallys.values().stream().mapToDouble( x -> (double) x.get() ).sum() : 0;	
		
		for(Entry<T,  AtomicLong> entry : tallys.entrySet()) { 
			//skip zero value for output
			if(entry.getValue().get() == 0 ) continue;
			double percent = (sum == 0)? 0 : 100 * (double)entry.getValue().get() / sum;
			Element ele1 = QprofilerXmlUtils.createSubElement( ele, Stally );
			ele1.setAttribute( Svalue, String.valueOf( entry.getKey() ));
			ele1.setAttribute( Scount, String.valueOf( entry.getValue().get() )); 
			if( hasPercent == true) {
				ele1.setAttribute(Spercent, String.format("%,.2f", percent));	
			}
		}        	
    }
    
    public static void addCommentChild(Element ele, String comment) {
    	if(ele.getFirstChild() == null) return;
    	ele.insertBefore( ele.getOwnerDocument().createComment( comment), ele.getFirstChild());        	
    }

    public static Element createReadGroupNode( Element parent, String rgid) {
     	Element ele = QprofilerXmlUtils.createSubElement( parent, "readGroup" );
    	ele.setAttribute(Sid, rgid);
    	return ele;
    }
        
        
}
