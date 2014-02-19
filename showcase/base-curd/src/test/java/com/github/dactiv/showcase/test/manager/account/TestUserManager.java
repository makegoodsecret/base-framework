package com.github.dactiv.showcase.test.manager.account;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.github.dactiv.orm.core.Page;
import com.github.dactiv.orm.core.PageRequest;
import com.github.dactiv.orm.core.PropertyFilter;
import com.github.dactiv.orm.core.PropertyFilters;
import com.github.dactiv.showcase.common.enumeration.entity.State;
import com.github.dactiv.showcase.entity.account.User;
import com.github.dactiv.showcase.service.account.AccountManager;
import com.github.dactiv.showcase.test.manager.ManagerTestCaseSupport;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * 测试用户管理所有方法
 * 
 * @author maurice
 *
 */
public class TestUserManager extends ManagerTestCaseSupport{

	@Autowired
	private AccountManager accountManager;

	@Test
	@Transactional(readOnly=true)
	public void testGetUser() {
		User user = accountManager.getUser("SJDK3849CKMS3849DJCK2039ZMSK0001");
		assertEquals(user.getUsername(),"maurice");
	}

	@Test
	public void testSearchUserPage() {
		PageRequest request = new PageRequest();
		
		List<PropertyFilter> filters = Lists.newArrayList(
				PropertyFilters.get("LIKES_username", "es"),
				PropertyFilters.get("EQI_state", "1")
		);
		
		Page<User> page = accountManager.searchUserPage(request, filters);
		
		assertEquals(page.getTotalItems(), 4);
		assertEquals(page.getTotalPages(), 1);
	}

	@Test
	public void testInsertUser() {
		User entity = new User();
		
		entity.setEmail("test@test.com");
		entity.setPassword("123456");
		entity.setRealname("一个测试用户");
		entity.setUsername("test_maurice");
		entity.setState(State.Enable.getValue());
		
		int before = countRowsInTable("tb_user");
		accountManager.insertUser(entity);
		int after = countRowsInTable("tb_user");
		
		assertEquals(before + 1, after);
	}

	@Test
	@Transactional
	public void testUpdateUser() {
		User entity = accountManager.getUser("SJDK3849CKMS3849DJCK2039ZMSK0001");
		entity.setUsername("modify");
		entity.setPassword("123456");
		entity.setRealname("maurice");
		
		accountManager.updateUser(entity);
		
		getSessionFactory().getCurrentSession().flush();
		getSessionFactory().getCurrentSession().clear();
		
		entity = accountManager.getUser("SJDK3849CKMS3849DJCK2039ZMSK0001");
		
		assertEquals(entity.getUsername(), "maurice");
		assertEquals(entity.getPassword(), "e10adc3949ba59abbe56e057f20f883e");
		assertEquals(entity.getRealname(), "maurice");
	}

	@Test
	public void testIsUsernameUnique() {
		assertEquals(accountManager.isUsernameUnique("maurice"), false);
	}

	@Test
	public void testDeleteUsers() {
		int before = countRowsInTable("tb_user");
		accountManager.deleteUsers(Lists.newArrayList("SJDK3849CKMS3849DJCK2039ZMSK0001"));
		int after = countRowsInTable("tb_user");
		
		assertEquals(before - 1, after);
	}

	@Test
	public void testGetUserByUsername() {
		User entity = accountManager.getUserByUsername("maurice");
		assertEquals(entity.getUsername(), "maurice");
		assertEquals(entity.getRealname(), "maurice.chen");
	}
	
}
