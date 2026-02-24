package com.example.project_vending_machines.dao

import com.example.project_vending_machines.HibernateUtil
import com.example.project_vending_machines.entity.Sale
import org.hibernate.Session
import org.hibernate.Transaction

class SaleDAO {

    fun getAll(): List<Sale> {
        val session = HibernateUtil.sessionFactory.openSession()
        val sales = session.createQuery("from Sale", Sale::class.java).list()
        session.close()
        return sales
    }

    fun getById(id: Long): Sale? {
        val session = HibernateUtil.sessionFactory.openSession()
        val sale = session.get(Sale::class.java, id)
        session.close()
        return sale
    }

    fun save(sale: Sale): Sale {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.persist(sale)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return sale
    }

    fun update(sale: Sale): Sale {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.merge(sale)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return sale
    }

    fun delete(id: Long) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            val sale = session.get(Sale::class.java, id)
            if (sale != null) {
                session.remove(sale)
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