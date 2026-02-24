package com.example.project_vending_machines

import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

object HibernateUtil {
    val sessionFactory: SessionFactory = run {
        try {
            Configuration().configure().buildSessionFactory()
        } catch (e: Exception) {
            throw ExceptionInInitializerError(e)
        }
    }

    fun shutdown() {
        sessionFactory.close()
    }
}