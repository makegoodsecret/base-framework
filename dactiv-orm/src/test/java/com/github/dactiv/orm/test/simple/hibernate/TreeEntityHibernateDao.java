package com.github.dactiv.orm.test.simple.hibernate;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.github.dactiv.orm.core.hibernate.support.BasicHibernateDao;
import com.github.dactiv.orm.test.entity.Menu;

@Repository
@Transactional
public class TreeEntityHibernateDao extends BasicHibernateDao<Menu, String>{

}
