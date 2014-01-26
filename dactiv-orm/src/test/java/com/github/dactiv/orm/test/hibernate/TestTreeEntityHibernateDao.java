package com.github.dactiv.orm.test.hibernate;

import java.util.HashMap;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.github.dactiv.common.unit.Fixtures;
import com.github.dactiv.orm.test.entity.Menu;
import com.github.dactiv.orm.test.simple.hibernate.TreeEntityHibernateDao;
import com.google.common.collect.Lists;

@Transactional
@ActiveProfiles("hibernate")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/applicationContext-test.xml")
public class TestTreeEntityHibernateDao{
	
	@Autowired
	private TreeEntityHibernateDao dao;
	
	private SessionFactory sessionFactory;
	
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	private DataSource dataSource;
	
	/**
	 * 通过表名计算出表中的总记录数
	 * 
	 * @param tableName 表名
	 * 
	 * @return int
	 */
	protected int countRowsInTable(String tableName) {
		return jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName,new HashMap<String, Object>(), Integer.class);
	}
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Before
	public void install() throws Exception {
		Fixtures.reloadData(dataSource, "/sample-data.xml");
	}
	
	@Test
	public void doTest() {
		dao.deleteAll(Lists.newArrayList("SJDK3849CKMS3849DJCK2039ZMSK0012","SJDK3849CKMS3849DJCK2039ZMSK0013","SJDK3849CKMS3849DJCK2039ZMSK0014"));
		sessionFactory.getCurrentSession().flush();
		Menu parent = dao.get("SJDK3849CKMS3849DJCK2039ZMSK0011");
		Assert.assertFalse(parent.getLeaf());
		
		Menu menu = new Menu();
		menu.setParent(parent);
		menu.setName("test_l");
		menu.setType(1);
		dao.save(menu);
		sessionFactory.getCurrentSession().flush();
		
		parent = dao.get("SJDK3849CKMS3849DJCK2039ZMSK0011");
		Assert.assertTrue(parent.getLeaf());
		
	}
}
