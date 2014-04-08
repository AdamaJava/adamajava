/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import java.util.List;

import org.qcmg.ensembl.model.SeqRegion;


public interface SeqRegionDAO extends GenericDAO<SeqRegion, Long> {
	List<SeqRegion> findByCoordId(Long coordId);
}
