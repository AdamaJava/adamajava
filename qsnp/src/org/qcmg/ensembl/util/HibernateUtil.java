/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.util;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.qcmg.ensembl.model.Gene;
import org.qcmg.ensembl.model.GeneStableId;
import org.qcmg.ensembl.model.SeqRegion;
import org.qcmg.ensembl.model.Transcript;
import org.qcmg.ensembl.model.TranscriptStable;
import org.qcmg.ensembl.model.TranscriptVariation;
import org.qcmg.ensembl.model.VariationFeature;

public class HibernateUtil {
	private static final SessionFactory sessionFactory;
//	private static final String HIBERNATE_CONFIG_FILE = "hibernate.cfg.xml";

    static {
    	
//    	String classPath = System.getProperty("java.class.path","."); 
//    	System.out.println("classpath = " + classPath);
//    	
//    	File hibernateFile = new File(HIBERNATE_CONFIG_FILE);
//    	System.out.println("hibernateFile = " + hibernateFile.getAbsolutePath());
    	
        try {
            sessionFactory = new AnnotationConfiguration()
            .addAnnotatedClass(VariationFeature.class)
            .addAnnotatedClass(TranscriptVariation.class)
            .addAnnotatedClass(TranscriptStable.class)
            .addAnnotatedClass(Transcript.class)
            .addAnnotatedClass(Gene.class)
            .addAnnotatedClass(GeneStableId.class)
            .addAnnotatedClass(SeqRegion.class)
            .configure().buildSessionFactory();
//            .configure(new File(HIBERNATE_CONFIG_FILE)).buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }

    }
//	static {
//		try {
//			sessionFactory = new AnnotationConfiguration()
//			.addAnnotatedClass(VariationFeature.class)
//			.addAnnotatedClass(TranscriptVariation.class)
//			.addAnnotatedClass(TranscriptStable.class)
//			.addAnnotatedClass(SeqRegion.class)
//			.configure(new File("hibernate.cfg.xml")).buildSessionFactory();
//		} catch (Throwable ex) {
//			throw new ExceptionInInitializerError(ex);
//		}
//		
//	}
    public static void main(String[] args) {
		System.out.println("HibernateUtil.main");
	}


    public static Session getSession() throws HibernateException {
        return sessionFactory.openSession();
    }
//    public static Session getCurrentSession() throws HibernateException {
//    	if (null == sessionFactory.getCurrentSession())
//    		return sessionFactory.openSession();
//    	return sessionFactory.getCurrentSession();
//    }

}

