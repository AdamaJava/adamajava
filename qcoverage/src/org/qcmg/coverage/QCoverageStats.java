/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.eclipse.persistence.oxm.annotations.XmlNameTransformer;

import java.util.ArrayList;
import java.util.List;

@XmlType(name = "QCoverageStats", propOrder = {
    "coverageReport"
})
@XmlRootElement
@XmlNameTransformer(NameGenerator.class)
public class QCoverageStats {

	@XmlElement(required = true)
    protected List<CoverageReport> coverageReport;

    /**
     * Gets the value of the coverageReport property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore, any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the coverageReport property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCoverageReport().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CoverageReport }
     * 
     * 
     */
    public List<CoverageReport> getCoverageReport() {
        if (coverageReport == null) {
            coverageReport = new ArrayList<>();
        }
        return this.coverageReport;
    }

}
