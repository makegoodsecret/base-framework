package com.github.dactiv.showcase.dao.account;

import org.springframework.stereotype.Repository;

import com.github.dactiv.orm.core.hibernate.support.HibernateSupportDao;
import com.github.dactiv.showcase.entity.account.Group;

/**
 * 组数据访问
 * 
 * @author maurice
 *
 */
@Repository
public class GroupDao extends HibernateSupportDao<Group, String>{

	
}
