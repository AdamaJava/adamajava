package org.qcmg.sig.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.sig.model.Comparison;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SignatureXmlOutputTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void preservesEscapedNamesCountsCutoffsAndComparisonOrder() throws Exception {
        String first = "a<&\"\t\n\r.vcf";
        String second = "b-é.vcf";
        String third = "c.vcf";
        Map<String, int[]> files = Map.of(first, new int[]{1, 10, 12},
                second, new int[]{2, 20, -1}, third, new int[]{3, 0, -1});
        List<Comparison> comparisons = new ArrayList<>(List.of(
                new Comparison(second, 20, third, 0, 0, 0),
                new Comparison(first, 10, third, 0, 0, 0),
                new Comparison(first, 10, second, 20, 4, 5)));
        File output = folder.newFile();
        SignatureUtil.writeXmlOutput(files, comparisons, output.getAbsolutePath(), 0.95f, 0.6f, 0.4f);
        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(output);
        Element cutoffs = (Element) xml.getElementsByTagName("cutoffs").item(0);
        assertEquals("0.95", cutoffs.getAttribute("hom"));
        assertEquals("0.6", cutoffs.getAttribute("upper_het"));
        assertEquals("0.4", cutoffs.getAttribute("lower_het"));
        Element file = (Element) xml.getElementsByTagName("file").item(0);
        assertEquals(first, file.getAttribute("name"));
        assertEquals("10", file.getAttribute("coverage"));
        assertEquals("12", file.getAttribute("average_coverage_at_positions"));
        Element comparison = (Element) xml.getElementsByTagName("comparison").item(0);
        assertEquals("1", comparison.getAttribute("file1"));
        assertEquals("2", comparison.getAttribute("file2"));
        assertEquals("5", comparison.getAttribute("overlap"));
        assertEquals("0.8", comparison.getAttribute("score"));
        comparison = (Element) xml.getElementsByTagName("comparison").item(2);
        assertEquals("2", comparison.getAttribute("file1"));
        assertEquals("3", comparison.getAttribute("file2"));
        assertEquals("NaN", comparison.getAttribute("score"));
    }

    @Test
    public void writesEmptyResults() throws Exception {
        File output = folder.newFile();
        SignatureUtil.writeXmlOutput(Map.of(), new ArrayList<>(), output.getAbsolutePath());
        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(output);
        assertEquals("qsignature", xml.getDocumentElement().getTagName());
        assertEquals(1, xml.getElementsByTagName("files").getLength());
        assertEquals(1, xml.getElementsByTagName("comparisons").getLength());
        assertEquals(0, xml.getElementsByTagName("file").getLength());
        assertEquals(0, xml.getElementsByTagName("comparison").getLength());
    }
}
