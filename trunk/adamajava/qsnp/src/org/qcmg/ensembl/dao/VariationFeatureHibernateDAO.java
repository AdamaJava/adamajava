/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.ensembl.model.SeqRegion;
import org.qcmg.ensembl.model.VariationFeature;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;

public class VariationFeatureHibernateDAO extends GenericHibernateDAO<VariationFeature, Long> implements
VariationFeatureDAO {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VariationFeatureHibernateDAO.class);

	public List<VariationFeature> findBySeqRegionPileup(SeqRegion seqRegion, QSnpRecord pileup) {
		
		String mutation = null;
		if (Classification.SOMATIC == pileup.getClassification()) {
			if (null == pileup.getMutation()) {
//				logger.info("null mutation for pileup record: " + pileup.getFormattedString());
			} else {
				mutation = pileup.getRef() + "/" + pileup.getMutation().substring(pileup.getMutation().indexOf('>')+1);
			}
		} else {	// germline
			if (pileup.getNormalGenotype().getFirstAllele() != pileup.getRef()) {
				mutation = pileup.getRef() + "/" + pileup.getNormalGenotype().getFirstAllele();
			} else {
				mutation = pileup.getRef() + "/" + pileup.getNormalGenotype().getSecondAllele();
			}
		}
		
		
		return findByCriteria(Restrictions.eq("seqRegion", seqRegion),
				Restrictions.eq("seqRegionStart", pileup.getPosition()),
				Restrictions.eq("seqRegionEnd", pileup.getPosition()),
				Restrictions.eq("allele", mutation),
//				Restrictions.eq("name", pileup.getDbSnpId()),
				Restrictions.eq("seqRegionStrand", 1),
				Restrictions.eq("mapWeight", 1));
		
	}
}
