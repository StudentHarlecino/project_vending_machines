package com.example.project_vending_machines

import java.lang.module.Configuration

object HibernateUtil {
    val sessionFactory: SessionFactory = try {
        Configuration().configure().buildSessionFactory() as SessionFactory
    } catch (ex: Throwable){
        println("Inital SessionFactory creation failed. $ex")
        throw ExceptionInInitializerError(ex)
    }

    fun shutdown() = sessionFactory.close()
}