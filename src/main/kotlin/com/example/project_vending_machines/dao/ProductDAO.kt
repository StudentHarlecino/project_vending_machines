package com.example.project_vending_machines.dao

import com.example.project_vending_machines.HibernateUtil
import com.example.project_vending_machines.entity.Product
import org.hibernate.Session
import org.hibernate.Transaction
import java.math.BigDecimal

class ProductDAO {

    fun getAll(): List<Product> {
        val session = HibernateUtil.sessionFactory.openSession()
        val products = session.createQuery("from Product", Product::class.java).list()
        session.close()
        return products
    }

    fun getById(id: Int): Product? {
        val session = HibernateUtil.sessionFactory.openSession()
        val product = session.get(Product::class.java, id)
        session.close()
        return product
    }

    fun save(product: Product): Product {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.persist(product)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return product
    }

    fun update(product: Product): Product {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.merge(product)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return product
    }

    fun delete(id: Int) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            val product = session.get(Product::class.java, id)
            if (product != null) {
                session.remove(product)
            }
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
    }

    fun findLowStock(): List<Product> {
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery(
            "from Product where quantityInStock <= minimalStock",
            Product::class.java
        )
        val products = query.list()
        session.close()
        return products
    }

    fun findByName(name: String): List<Product> {
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery("from Product where name like :name", Product::class.java)
        query.setParameter("name", "%$name%")
        val products = query.list()
        session.close()
        return products
    }
}