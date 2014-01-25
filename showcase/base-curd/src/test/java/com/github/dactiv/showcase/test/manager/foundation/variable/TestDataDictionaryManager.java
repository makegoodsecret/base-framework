package com.github.dactiv.showcase.test.manager.foundation.variable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.dactiv.common.type.FieldType;
import com.github.dactiv.orm.core.Page;
import com.github.dactiv.orm.core.PageRequest;
import com.github.dactiv.orm.core.PropertyFilter;
import com.github.dactiv.orm.core.PropertyFilters;
import com.github.dactiv.showcase.common.enumeration.SystemDictionaryCode;
import com.github.dactiv.showcase.entity.foundation.variable.DataDictionary;
import com.github.dactiv.showcase.entity.foundation.variable.DictionaryCategory;
import com.github.dactiv.showcase.service.foundation.SystemVariableManager;
import com.github.dactiv.showcase.test.manager.ManagerTestCaseSupport;
import com.google.common.collect.Lists;

/**
 * 测试数据字典管理所有方法
 * 
 * @author maurice
 *
 */
public class TestDataDictionaryManager extends ManagerTestCaseSupport{
	
	@Autowired
	private SystemVariableManager systemVariableManager;
	
	@Test
	public void testGetDataDictionariesByCategoryCode() {
		List<DataDictionary> list = Lists.newArrayList();
		list = systemVariableManager.getDataDictionariesByCategoryCode(SystemDictionaryCode.State);
		assertEquals(list.size(),3);
		list = systemVariableManager.getDataDictionariesByCategoryCode(SystemDictionaryCode.State, "1");
		assertEquals(list.size(),2);
		list = systemVariableManager.getDataDictionariesByCategoryCode(SystemDictionaryCode.State, "1","2","3");
		assertEquals(list.size(),0);
		
	}
	
	@Test
	public void testDeleteDataDictionary() {
		
		List<String> ids = new ArrayList<String>();
		CollectionUtils.addAll(ids, new String[]{"402881e437d47b250137d481b6920001","402881e437d47b250137d481dda30002"});
		
		int beforeRow = countRowsInTable("TB_DATA_DICTIONARY");
		systemVariableManager.deleteDataDictionary(ids);
		int afterRow = countRowsInTable("TB_DATA_DICTIONARY");
		
		assertEquals(afterRow, beforeRow - 2);
	}
	
	@Test
	public void testSearchDataDictionary() {
		PageRequest request = new PageRequest();
		
		List<PropertyFilter> filters = Lists.newArrayList(
				PropertyFilters.build("LIKES_type", "S")
		);
		
		Page<DataDictionary> page = systemVariableManager.searchDataDictionaryPage(request, filters);
		
		assertEquals(page.getTotalItems(), 11);
		assertEquals(page.getTotalPages(), 2);
	}
	
	@Test
	public void testSaveDataDictionary() {
		
		DictionaryCategory category = systemVariableManager.getDictionaryCategory("402881e437d467d80137d46fc0e50001");
		
		DataDictionary dataDictionary = new DataDictionary();
		dataDictionary.setCategory(category);
		dataDictionary.setName("test");
		dataDictionary.setValue("4");
		dataDictionary.setType(FieldType.I.toString());
		dataDictionary.setRemark("*");
		
		int beforeRow = countRowsInTable("TB_DATA_DICTIONARY");
		systemVariableManager.saveDataDictionary(dataDictionary);
		int afterRow = countRowsInTable("TB_DATA_DICTIONARY");
		
		assertEquals(afterRow, beforeRow + 1);
		
	}
	
	
	
}
