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
        val savedMachine = session.merge(machine)
        transaction.commit()
        session.close()
        return savedMachine
    }

    fun update(machine: VendingMachine) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        session.merge(machine)
        transaction.commit()
        session.close()
    }

    fun delete(machine: VendingMachine) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        session.remove(machine)
        transaction.commit()
        session.close()
    }

    fun deleteById(id: Long) {
        val session = HibernateUtil.sessionFactory.openSession()
        val transaction: Transaction = session.beginTransaction()
        val machine = session.get(VendingMachine::class.java, id)
        if (machine != null) {
            session.remove(machine)
        }
        transaction.commit()
        session.close()
    }
}