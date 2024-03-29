package au.edu.qimr.qannotate.nanno;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;

import static org.junit.Assert.*;

public class AnnotateUtilsTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void convertAnnotationsToSingleList() {
		List<String> annos = new ArrayList<>();
		annos.add("FIELD1=1\tFIELD2=2");
		annos.add("FIELD3=3\tFIELD4=4");
		annos.add("FIELD5=5\tFIELD6=6");
		
		List<String> singleAnnoList = AnnotateUtils.convertAnnotations(annos);
		assertEquals(6, singleAnnoList.size());
        assertTrue(singleAnnoList.contains("FIELD1=1"));
		
		annos.add("FIELD7=7\tFIELD8=8\tFIELD9=9\tFIELD10=10");
		singleAnnoList = AnnotateUtils.convertAnnotations(annos);
		assertEquals(10, singleAnnoList.size());
        assertTrue(singleAnnoList.contains("FIELD10=10"));
		
		/*
		 * look at empty and null lists
		 */
		annos = new ArrayList<>();
		singleAnnoList = AnnotateUtils.convertAnnotations(annos);
		assertEquals(0, singleAnnoList.size());
		singleAnnoList = AnnotateUtils.convertAnnotations(null);
		assertEquals(0, singleAnnoList.size());
		
	}
	
	@Test
	public void getEmptyFieldValues() {
		assertEquals("", AnnotateUtils.getEmptyHeaderValues(0));
		assertEquals("\t", AnnotateUtils.getEmptyHeaderValues(1));
		assertEquals("\t\t", AnnotateUtils.getEmptyHeaderValues(2));
		assertEquals("\t\t\t\t\t\t\t\t\t\t\t\t", AnnotateUtils.getEmptyHeaderValues(12));
	}
	
	@Test
	public void getSearchTerm() {
		assertEquals("\"GENE\"+(\"277C>T\"|\"277C->T\"|\"277C-->T\"|\"277C/T\"|\"Arg93Trp\")", AnnotateUtils.getSearchTerm("c.277C>T", "p.Arg93Trp"));
		assertEquals("\"GENE\"+(\"277T>C\"|\"277T->C\"|\"277T-->C\"|\"277T/C\"|\"Arg93Trp\")", AnnotateUtils.getSearchTerm("c.277T>C", "p.Arg93Trp"));
		assertEquals("", AnnotateUtils.getSearchTerm(null, null));
		assertEquals("\"GENE\"+(\"Arg93Trp\")", AnnotateUtils.getSearchTerm("c277TC", "p.Arg93Trp"));
		assertEquals("\"GENE\"+(\"Arg93Trp\")", AnnotateUtils.getSearchTerm("", "p.Arg93Trp"));
		assertEquals("\"GENE\"+(\"Arg93Trp\")", AnnotateUtils.getSearchTerm(null, "p.Arg93Trp"));
		assertEquals("", AnnotateUtils.getSearchTerm("c277TC", null));
		assertEquals("", AnnotateUtils.getSearchTerm("", ""));
	}
	
	@Test
	public void getADForSplitVCfRecords() {
		Map<String, String> altToDPMap = AnnotateUtils.getADForSplitVcfRecords(new String[]{"A"}, "0,1");
		assertEquals(1, altToDPMap.size());
		assertEquals("0,1", altToDPMap.get("A"));
		
		altToDPMap = AnnotateUtils.getADForSplitVcfRecords(new String[]{"A","C"}, "0,1,2");
		assertEquals(2, altToDPMap.size());
		assertEquals("0,1", altToDPMap.get("A"));
		assertEquals("0,2", altToDPMap.get("C"));
		
		altToDPMap = AnnotateUtils.getADForSplitVcfRecords(new String[]{"A","C","G"}, "0,1,2,3");
		assertEquals(3, altToDPMap.size());
		assertEquals("0,1", altToDPMap.get("A"));
		assertEquals("0,2", altToDPMap.get("C"));
		assertEquals("0,3", altToDPMap.get("G"));
	}
	
	@Test
	public void countOccurences() {
		assertEquals(0, AnnotateUtils.countOccurrences("", ""));
		assertEquals(0, AnnotateUtils.countOccurrences("", ","));
		assertEquals(0, AnnotateUtils.countOccurrences("123", ","));
		assertEquals(0, AnnotateUtils.countOccurrences("XYZ", ","));
		assertEquals(0, AnnotateUtils.countOccurrences("XYZ", ","));
		assertEquals(1, AnnotateUtils.countOccurrences("ABC,XYZ", ","));
		assertEquals(2, AnnotateUtils.countOccurrences("ABC,XYZ,123", ","));
	}
	
	@Test
	public void loadJSONInputs() throws IOException {
		File inputJson = testFolder.newFile("inputs.json");
		List<String> data = Arrays.asList(
				"{",
  "\"outputFieldOrder\": \"field_1,field_2,field_3\",",
  "\"additionalEmptyFields\": \"test1,test2,test3\",",
  "\"includeSearchTerm\": true,",
  "\"annotationSourceThreadCount\": 3,",
  "\"inputs\": [{",
    "\"file\": \"/file_1.tsv\",",
    "\"chrIndex\": 1,",
    "\"positionIndex\": 2,",
    "\"refIndex\": 3,",
    "\"altIndex\": 4,",
    "\"fields\": \"field_1\"",
  "},",
  "{",
	  "\"file\": \"file_2.eff.vcf\",",
	  "\"chrIndex\": 1,",
	  "\"positionIndex\": 2,",
	  "\"refIndex\": 4,",
	  "\"altIndex\": 5,",
	  "\"snpEffVcf\": true,",
    "\"fields\": \"field_2\"",
  "},",
  "{",
	  "\"file\": \"/file_3.vcf.gz\",",
	  "\"chrIndex\": 1,",
	  "\"positionIndex\": 2,",
	  "\"refIndex\": 4,",
	  "\"altIndex\": 5,",
	  "\"fields\": \"field_3\"",
  "}]",
"}"
				);
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(inputJson))) {
			for (String line : data) {
				out.write(line + "\n");
			}
		}
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
        assertTrue(ais.isIncludeSearchTerm());
		assertEquals("test1,test2,test3", ais.getAdditionalEmptyFields());
		assertEquals(3, ais.getAnnotationSourceThreadCount());
		assertEquals("field_1,field_2,field_3", ais.getOutputFieldOrder());
		assertEquals(3, ais.getInputs().size());
	}
	
	@Test
	public void generateHeaders() throws IOException {
		assertEquals(0, AnnotateUtils.generateHeaders(null, null).size());
		assertEquals(0, AnnotateUtils.generateHeaders(new AnnotationInputs(), null).size());
		
		File inputJson = testFolder.newFile("inputs.json");
		List<String> data = Arrays.asList(
				"{",
  "\"outputFieldOrder\": \"field_1,field_2,field_3\",",
  "\"additionalEmptyFields\": \"test1,test2,test3\",",
  "\"includeSearchTerm\": true,",
  "\"annotationSourceThreadCount\": 3,",
  "\"inputs\": [{",
    "\"file\": \"/file_1.tsv\",",
    "\"chrIndex\": 1,",
    "\"positionIndex\": 2,",
    "\"refIndex\": 3,",
    "\"altIndex\": 4,",
    "\"fields\": \"field_1\"",
  "},",
  "{",
	  "\"file\": \"file_2.eff.vcf\",",
	  "\"chrIndex\": 1,",
	  "\"positionIndex\": 2,",
	  "\"refIndex\": 4,",
	  "\"altIndex\": 5,",
	  "\"snpEffVcf\": true,",
    "\"fields\": \"field_2\"",
  "},",
  "{",
	  "\"file\": \"/file_3.vcf.gz\",",
	  "\"chrIndex\": 1,",
	  "\"positionIndex\": 2,",
	  "\"refIndex\": 4,",
	  "\"altIndex\": 5,",
	  "\"fields\": \"field_3\"",
  "}]",
"}"
				);
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(inputJson))) {
			for (String line : data) {
				out.write(line + "\n");
			}
		}
		AnnotationInputs ais = AnnotateUtils.getInputs(inputJson.getAbsolutePath());
		List<String> headers =  AnnotateUtils.generateHeaders(ais, null);
		assertEquals(4, headers.size());	// 3 inputs plus header line
        assertTrue(headers.contains("##file:fields\t/file_1.tsv:field_1"));
        assertTrue(headers.contains("##file:fields\tfile_2.eff.vcf:field_2"));
        assertTrue(headers.contains("##file:fields\t/file_3.vcf.gz:field_3"));
		assertEquals("#chr\tposition\tref\talt\toriginal_alt\tGATK_GT\tGATK_AD\tfield_1\tfield_2\tfield_3\ttest1\ttest2\ttest3\tsearchTerm", headers.get(headers.size() - 1));
	}
	
	@Test
	public void isOrderedHeaderListValid() {
        assertTrue(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h1", "h2", "h3"));
        assertFalse(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h1", "h2"));
        assertFalse(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h3", "h2"));
        assertFalse(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h3", "h1"));
        assertFalse(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h3", "h1", "h4"));
        assertFalse(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h2", "h1", "h4"));
        assertTrue(AnnotateUtils.isOrderedHeaderListValid("h1,h2,h3", "h2", "h3", "h1", "h1"));
        assertTrue(AnnotateUtils.isOrderedHeaderListValid("h1,h1,h2,h3", "h2", "h3", "h1", "h1"));
	}
	
	@Test
	public void getItemFromList() {
		assertEquals(Optional.empty(), AnnotateUtils.getAnnotationFromList(null, null));
		assertEquals(Optional.empty(), AnnotateUtils.getAnnotationFromList(new ArrayList<>(), null));
		assertEquals(Optional.empty(), AnnotateUtils.getAnnotationFromList(new ArrayList<>(), ""));
		assertEquals(Optional.empty(), AnnotateUtils.getAnnotationFromList(List.of("ANNO_1=1"), ""));
		assertEquals(Optional.empty(), AnnotateUtils.getAnnotationFromList(List.of("ANNO_1=1"), "ANNO_2"));
		assertEquals(Optional.of("1"), AnnotateUtils.getAnnotationFromList(List.of("ANNO_1=1"), "ANNO_1"));
		assertEquals(Optional.of("2"), AnnotateUtils.getAnnotationFromList(Arrays.asList("ANNO_1=1","ANNO_2=2"), "ANNO_2"));
		assertEquals(Optional.of("1"), AnnotateUtils.getAnnotationFromList(Arrays.asList("ANNO_1=1","ANNO_2=2"), "ANNO_1"));
	}

}
