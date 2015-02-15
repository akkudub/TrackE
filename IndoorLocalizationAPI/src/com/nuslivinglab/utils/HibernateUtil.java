package com.nuslivinglab.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	
	private static final SessionFactory sessionFactory;
	private static final StandardServiceRegistry serviceRegistry;
//	private static final ServiceRegistry serviceRegistry;

	static{
		try{
			Configuration config = new Configuration();
			config.configure().setProperty("hibernate.show_sql", "false");
//			serviceRegistry = new ServiceRegistryBuilder().applySettings(config.getProperties()).buildServiceRegistry();
			serviceRegistry = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
			sessionFactory = config.configure().buildSessionFactory(serviceRegistry);
			
		}catch(Throwable ex){
			System.out.println("init session factory failed" + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}
	
	public static SessionFactory getSessionFactory(){
		return sessionFactory;
	}
	
}
