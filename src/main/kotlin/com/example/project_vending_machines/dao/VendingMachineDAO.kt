package com.example.project_vending_machines.dao

import com.example.project_vending_machines.HibernateUtil
import com.example.project_vending_machines.entity.VendingMachine
import org.hibernate.Session
import org.hibernate.Transaction

class VendingMachineDAO {

    fun getAll(): List<VendingMachine> {
        val session = HibernateUtil.sessionFactory.openSession()
        val machines = session.createQuery("from VendingMachine", VendingMachine::class.java).list()
        session.close()
        return machines
    }

    fun getById(id: Long): VendingMachine? {
        val session = HibernateUtil.sessionFactory.openSession()
        val machine = session.get(VendingMachine::class.java, id)
        session.close()
        return machine
    }

    fun save(machine: VendingMachine): VendingMachine {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.persist(machine)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return machine
    }

    fun update(machine: VendingMachine): VendingMachine {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            session.merge(machine)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
        return machine
    }

    fun delete(id: Long) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        try {
            val machine = session.get(VendingMachine::class.java, id)
            if (machine != null) {
                session.remove(machine)
            }
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            session.close()
        }
    }

    fun findByName(name: String): List<VendingMachine> {
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery("from VendingMachine where name like :name", VendingMachine::class.java)
        query.setParameter("name", "%$name%")
        val machines = query.list()
        session.close()
        return machines
    }

    fun findByLocation(location: String): List<VendingMachine> {
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery("from VendingMachine where location = :location", VendingMachine::class.java)
        query.setParameter("location", location)
        val machines = query.list()
        session.close()
        return machines
    }
}