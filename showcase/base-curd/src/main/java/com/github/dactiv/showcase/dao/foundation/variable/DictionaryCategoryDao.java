package com.github.dactiv.showcase.dao.foundation.variable;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.github.dactiv.orm.core.hibernate.support.HibernateSupportDao;
import com.github.dactiv.showcase.entity.foundation.variable.DictionaryCategory;

/**
 * 字典类别数据访问
 * 
 * @author maurice
 *
 */
@Repository
public class DictionaryCategoryDao extends HibernateSupportDao<DictionaryCategory, String>{
	
	/**
	 * 刷新一次DictionaryCategory的leaf字段，如果该leaf = 1 并且该组没有子类，把该组的leaf改成0
	 */
	public void refreshAllLeaf() {
		List<DictionaryCategory> list = findByQuery(DictionaryCategory.LeafTureNotAssociated);
		for (DictionaryCategory entity : list) {
			entity.setLeaf(false);
			save(entity);
		}
		
	}
	
}
