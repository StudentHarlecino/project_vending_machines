package com.example.project_vending_machines.dao

import com.example.project_vending_machines.HibernateUtil
import com.example.project_vending_machines.entity.User
import org.hibernate.Session
import org.hibernate.Transaction

class UserDAO {

    fun getAll(): List<User> {
        val session = HibernateUtil.sessionFactory.openSession()
        val users = session.createQuery("from User", User::class.java).list()
        session.close()
        return users
    }

    fun getById(id: Long): User? {
        val session = HibernateUtil.sessionFactory.openSession()
        val user = session.get(User::class.java, id)
        session.close()
        return user
    }

    fun getByEmail(email: String): User? {
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery("from User where email = :email", User::class.java)
        query.setParameter("email", email)
        val user = query.uniqueResult()
        session.close()
        return user
    }

    fun save(user: User): User {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.persist(user)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return user
    }

    fun update(user: User): User {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.merge(user)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return user
    }

    fun delete(id: Long) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            val user = session.get(User::class.java, id)
            if (user != null) {
                session.remove(user)
            }
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
    }

    fun authenticate(email: String, password: String): User? {
        // В реальном приложении здесь должна быть проверка хеша пароля
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery("from User where email = :email", User::class.java)
        query.setParameter("email", email)
        val user = query.uniqueResult()
        session.close()
        return user
    }
}