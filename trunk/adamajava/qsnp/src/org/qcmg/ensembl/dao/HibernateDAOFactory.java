/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import org.hibernate.Session;
import org.qcmg.ensembl.util.HibernateUtil;

public class HibernateDAOFactory extends DAOFactory {

	@Override
	public SeqRegionDAO getSeqRegionDAO() {
		return (SeqRegionDAO) instantiateDAO(SeqRegionHibernateDAO.class);
	}
	@Override
	public VariationFeatureDAO getVariationFeatureDAO() {
		return (VariationFeatureDAO) instantiateDAO(VariationFeatureHibernateDAO.class);
	}
	
	@SuppressWarnings("unchecked")
	private GenericHibernateDAO instantiateDAO(Class daoClass) {
        try {
            GenericHibernateDAO dao = (GenericHibernateDAO)daoClass.newInstance();
            dao.setSession(getCurrentSession());
            return dao;
        } catch (Exception ex) {
            throw new RuntimeException("Can not instantiate DAO: " + daoClass, ex);
        }
    }
	
	// You could override this if you don't want HibernateUtil for lookup
    protected Session getCurrentSession() {
        return HibernateUtil.getSession();
    }


}
