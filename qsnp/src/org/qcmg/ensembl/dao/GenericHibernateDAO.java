/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;

public abstract class GenericHibernateDAO <T, ID extends Serializable> implements GenericDAO<T, ID>{
	
	private Class<T> persistentClass;
	private Session session;
	
	public GenericHibernateDAO() {
		this.persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
    public void setSession(Session s) {
        this.session = s;
    }
	protected Session getSession() {
        if (session == null)
            throw new IllegalStateException("Session has not been set on DAO before usage");
        return session;
    }
	public Class<T> getPersistentClass() {
        return persistentClass;
    }

	public List<T> findAll() {
		return findByCriteria();
	}
	
	@SuppressWarnings("unchecked")
	public T findById(ID id) {
		T entity = (T) getSession().load(getPersistentClass(), id);
		
		return entity;
	}
	
	/**
     * Use this inside subclasses as a convenience method.
     */
    @SuppressWarnings("unchecked")
    protected List<T> findByCriteria(Criterion... criterion) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        return crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
//        return crit.list();
   }

}
