package com.github.dactiv.showcase.dao.foundation.variable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
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
	 * 并合子类到父类中，返回一个新的集合
	 * 
	 * @param list 数据集合
	 */
	public List<DictionaryCategory> mergeToParent(List<DictionaryCategory> list) {
		
		List<DictionaryCategory> result = new ArrayList<DictionaryCategory>();
		
		for (DictionaryCategory r : list) {
			if (r.getParent() == null) {
				DictionaryCategory temp = new DictionaryCategory();
				BeanUtils.copyProperties(r, temp);
				mergeToParent(list,temp);
				result.add(temp);
			}
		}
		
		return result;
	}
	
	/**
	 * 遍历list中的数据,如果数据的父类与parent相等，将数据加入到parent的children中
	 * 
	 * @param list 资源集合
	 * @param parent 父类对象
	 */
	private void mergeToParent(List<DictionaryCategory> list, DictionaryCategory parent) {
		if (!parent.getLeaf()) {
			return ;
		}
		
		parent.setChildren(new ArrayList<DictionaryCategory>());
		parent.setLeaf(false);
		
		for (DictionaryCategory r: list) {
			
			if (StringUtils.equals(r.getParentId(),parent.getId())) {
				DictionaryCategory temp = new DictionaryCategory();
				BeanUtils.copyProperties(r, temp);
				temp.setChildren(null);
				mergeToParent(list,temp);
				parent.getChildren().add(temp);
				parent.setLeaf(true);
			}
			
		}
	}
}
