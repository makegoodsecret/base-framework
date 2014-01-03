package com.github.dactiv.showcase.dao.foundation.variable;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Repository;

import com.github.dactiv.orm.core.hibernate.support.HibernateSupportDao;
import com.github.dactiv.showcase.common.enumeration.SystemDictionaryCode;
import com.github.dactiv.showcase.entity.foundation.variable.DataDictionary;
import com.google.common.collect.Lists;

/**
 * 数据字典数据访问
 * 
 * @author maurice
 *
 */
@Repository
public class DataDictionaryDao extends HibernateSupportDao<DataDictionary, String>{
	
	//通过字典类别代码获取数据字典集合的hql
	private final String GET_BY_CATEGORY_CODE = "from DataDictionary dd where dd.category.code = ?";
	
	/**
	 * 通过字典类别代码获取数据字典集合
	 * 
	 * @param code 字典列别
	 * @param ignoreValue 忽略字典的值
	 * 
	 * @return List
	 */
	public List<DataDictionary> getByCategoryCode(SystemDictionaryCode code,String... ignoreValue) {
		StringBuffer hql = new StringBuffer(GET_BY_CATEGORY_CODE);
		
		List<String> args = Lists.newArrayList(code.getCode());
		
		if (ArrayUtils.isNotEmpty(ignoreValue)) {
			for (int i = 0; i < ignoreValue.length; i++) {
				hql.append(" and dd.value <> ?");
				args.add(ignoreValue[i]);
			}
		}
		
		return findByQuery(hql.toString(), args.toArray());
	}
	
}
