/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.qcmg.ensembl.model.SeqRegion;

public class SeqRegionHibernateDAO extends GenericHibernateDAO<SeqRegion, Long> implements
		SeqRegionDAO {

	public List<SeqRegion> findByCoordId(Long coordId) {
		return findByCriteria(Restrictions.eq("coordsystemId", coordId));
	}

}
