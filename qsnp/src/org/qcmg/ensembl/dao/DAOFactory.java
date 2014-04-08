/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

public abstract class DAOFactory {
		 
    /**
     * Creates a standalone DAOFactory that returns unmanaged DAO
     * beans for use in any environment Hibernate has been configured
     * for. Uses HibernateUtil/SessionFactory and Hibernate context
     * propagation (CurrentSessionContext), thread-bound or transaction-bound,
     * and transaction scoped.
     */
    public static final Class HIBERNATE = org.qcmg.ensembl.dao.HibernateDAOFactory.class;
 
    /**
     * Factory method for instantiation of concrete factories.
     */
    public static DAOFactory instance(Class factory) {
        try {
            return (DAOFactory)factory.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't create DAOFactory: " + factory);
        }
    }
 
    // Add your DAO interfaces here
    public abstract SeqRegionDAO getSeqRegionDAO();
    public abstract VariationFeatureDAO getVariationFeatureDAO();
//    public abstract CategoryDAO getCategoryDAO();
//    public abstract CommentDAO getCommentDAO();
//    public abstract ShipmentDAO getShipmentDAO();
	 
}
