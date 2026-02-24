package com.example.project_vending_machines.dao

import com.example.project_vending_machines.HibernateUtil
import com.example.project_vending_machines.entity.Role
import org.hibernate.Session
import org.hibernate.Transaction

class RoleDAO {

    fun getAll(): List<Role> {
        val session = HibernateUtil.sessionFactory.openSession()
        val roles = session.createQuery("from Role", Role::class.java).list()
        session.close()
        return roles
    }

    fun getById(id: Int): Role? {
        val session = HibernateUtil.sessionFactory.openSession()
        val role = session.get(Role::class.java, id)
        session.close()
        return role
    }

    fun getByName(name: String): Role? {
        val session = HibernateUtil.sessionFactory.openSession()
        val query = session.createQuery("from Role where name = :name", Role::class.java)
        query.setParameter("name", name)
        val role = query.uniqueResult()
        session.close()
        return role
    }
}