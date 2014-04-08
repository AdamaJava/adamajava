/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import java.util.List;

import org.qcmg.ensembl.model.SeqRegion;
import org.qcmg.ensembl.model.VariationFeature;
import org.qcmg.pileup.QSnpRecord;



public interface VariationFeatureDAO extends GenericDAO<VariationFeature, Long> {
	List<VariationFeature> findBySeqRegionPileup(SeqRegion sr, QSnpRecord pileup);
}
