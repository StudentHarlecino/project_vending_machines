package com.example.project_vending_machines.dao

import com.example.project_vending_machines.HibernateUtil
import com.example.project_vending_machines.entity.Maintenance
import org.hibernate.Session
import org.hibernate.Transaction

class MaintenanceDAO {

    fun getAll(): List<Maintenance> {
        val session = HibernateUtil.sessionFactory.openSession()
        val maintenances = session.createQuery("from Maintenance", Maintenance::class.java).list()
        session.close()
        return maintenances
    }

    fun getById(id: Long): Maintenance? {
        val session = HibernateUtil.sessionFactory.openSession()
        val maintenance = session.get(Maintenance::class.java, id)
        session.close()
        return maintenance
    }

    fun save(maintenance: Maintenance): Maintenance {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.persist(maintenance)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return maintenance
    }

    fun update(maintenance: Maintenance): Maintenance {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.merge(maintenance)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return maintenance
    }

    fun delete(id: Long) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            val maintenance = session.get(Maintenance::class.java, id)
            if (maintenance != null) {
                session.remove(maintenance)
            }
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
    }
}