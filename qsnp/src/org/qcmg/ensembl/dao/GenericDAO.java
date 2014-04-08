/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import java.io.Serializable;
import java.util.List;


public interface GenericDAO<T, ID extends Serializable> {

	T findById(ID id);
	List<T> findAll();
}
