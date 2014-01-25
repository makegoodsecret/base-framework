package com.github.dactiv.showcase.test.founction.foundation.variable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.github.dactiv.showcase.test.founction.FunctionTestCaseSupport;

/**
 * 测试字典类别管理功能
 * 
 * @author maurice
 *
 */
public class TestDictionaryCategoryManagerFunction extends FunctionTestCaseSupport{
	
	@Test
	public void doTest() {
		s.open("/");
		//通过点击，进入功能模块
		s.click(By.id("SJDK3849CKMS3849DJCK2039ZMSK0017"));
		s.click(By.id("SJDK3849CKMS3849DJCK2039ZMSK0019"));
		
		//获取table中的所有操作前的tr
		List<WebElement> beforeLi = s.findElements(By.xpath("//div[@class='tree']//*//li"));
		//断言所有tr是否等于期望值
		assertEquals(beforeLi.size(), 5);
		
		//打开添加页面
		s.click(By.xpath("//a[@href='/dactiv-base-curd/foundation/variable/dictionary-category/read']"));
		//填写表单
		s.type(By.xpath("//form[@id='save-dictionary-category-form']//input[@name='name']"), "test_dictionary_category");
		s.type(By.xpath("//form[@id='save-dictionary-category-form']//input[@name='code']"), "test-code");
		
		s.type(By.xpath("//form[@id='save-dictionary-category-form']//textarea[@name='remark']"),"这是一个测试添加的字典类别记录");
		//提交表单，页面验证不通过
		s.click(By.xpath("//div[@class='panel-footer']//button[@type='submit']"));
		
		//返回成功信息
		String message = s.findElement(By.className("alert")).getText();
		assertTrue(message.contains("保存成功"));
		
		//获取tree中的所有操作后的li节点
		List<WebElement> afterLi = s.findElements(By.xpath("//div[@class='tree']//*//li"));
		//断言所有li是否等于期望值
		assertEquals(afterLi.size(), beforeLi.size() + 1);
		
		//点击编辑功能
		s.findElement(By.xpath("//*//div//*[contains(text(),'test_dictionary_category')]//..//..//..//a[@href!='']")).click();
		//填写表单
		s.type(By.xpath("//form[@id='save-dictionary-category-form']//input[@name='name']"), "test_dictionary_category_modify");
		//提交表单
		s.click(By.xpath("//div[@class='panel-footer']//button[@type='submit']"));
		
		//返回成功信息
		message = s.findElement(By.className("alert")).getText();
		assertTrue(message.contains("保存成功"));
		
		//获取tree中的所有操作后的li节点
		afterLi = s.findElements(By.xpath("//div[@class='tree']//*//li"));
		//断言所有li是否等于期望值
		assertEquals(afterLi.size(), beforeLi.size() + 1);
		
		//选中删除的记录
		s.check(By.xpath("//*//div//*[contains(text(),'test_dictionary_category_modify')]//..//input"));
		//提交删除表单
		s.click(By.xpath("//div[@class='panel-footer']//*[@type='button']"));
		s.click(By.xpath("//div[@class='bootbox modal fade bootbox-confirm in']//*[@data-bb-handler='confirm']"));
		
		//返回成功信息
		message = s.findElement(By.className("alert")).getText();
		assertTrue(message.contains("删除1条信息成功"));
		
		//获取tree中的所有操作后的li节点
		afterLi = s.findElements(By.xpath("//div[@class='tree']//*//li"));
		//断言所有li是否等于期望值
		assertEquals(afterLi.size(), beforeLi.size() + 1);
		
	}
	
}
