package com.github.dactiv.showcase.dao.account;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.github.dactiv.orm.core.hibernate.support.HibernateSupportDao;
import com.github.dactiv.showcase.entity.account.Group;

/**
 * 部门数据访问
 * 
 * @author maurice
 *
 */
@Repository
public class GroupDao extends HibernateSupportDao<Group, String>{

	/**
	 * 刷新一次Group的leaf字段，如果该leaf = 1 并且该组没有子类，把该组的leaf改成0
	 */
	public void refreshAllLeaf() {
		List<Group> list = findByQuery(Group.LeafTureNotAssociated);
		for (Group entity : list) {
			entity.setLeaf(false);
			save(entity);
		}
		
	}
}
