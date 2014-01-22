package com.github.dactiv.showcase.test.manager.foundation.variable;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.github.dactiv.showcase.entity.foundation.variable.DictionaryCategory;
import com.github.dactiv.showcase.service.foundation.SystemVariableManager;
import com.github.dactiv.showcase.test.manager.ManagerTestCaseSupport;
import com.google.common.collect.Lists;

/**
 * 测试字典类别管理所有方法
 * 
 * @author maurice
 *
 */
public class TestDictionaryCategoryManager extends ManagerTestCaseSupport {

	@Autowired
	private SystemVariableManager systemVariableManager;
	
	@Test
	@Transactional
	public void testSaveDictionaryCategory() {
		DictionaryCategory category = new DictionaryCategory();
		category.setCode("test");
		category.setName("测试");
		category.setRemark("*");
		
		int beforeRow = countRowsInTable("TB_DICTIONARY_CATEGORY");
		systemVariableManager.saveDictionaryCategory(category);
		int afterRow = countRowsInTable("TB_DICTIONARY_CATEGORY");
		
		assertEquals(afterRow, beforeRow + 1);
		assertFalse(category.getLeaf());
		
		DictionaryCategory parent = systemVariableManager.getDictionaryCategory("402881e437d47b250137d485274b0005");
		category.setParent(parent);
		systemVariableManager.saveDictionaryCategory(category);
		assertTrue(parent.getLeaf());
		category.setParent(null);
		systemVariableManager.saveDictionaryCategory(category);
		assertFalse(parent.getLeaf());
	}

	@Test
	public void testDeleteDictionaryCategory() {
		
		int beforeRow = countRowsInTable("TB_DICTIONARY_CATEGORY");
		List<String> ids = Lists.newArrayList("402881e437d47b250137d485274b0005");
		systemVariableManager.deleteDictionaryCategory(ids);
		int afterRow = countRowsInTable("TB_DICTIONARY_CATEGORY");
		
		assertEquals(beforeRow, afterRow + 1);
		//TODO 实现删除后自动关联leaf单元测试
	}

	@Test
	public void testGetAllDictionaryCategories() {
		List<DictionaryCategory> result = systemVariableManager.getDictionaryCategories();
		assertEquals(5, result.size());
	}

}
