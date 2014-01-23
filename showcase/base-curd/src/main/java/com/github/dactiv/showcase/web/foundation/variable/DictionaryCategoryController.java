package com.github.dactiv.showcase.web.foundation.variable;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.github.dactiv.orm.core.PropertyFilter;
import com.github.dactiv.orm.core.PropertyFilters;
import com.github.dactiv.showcase.common.annotation.OperatingAudit;
import com.github.dactiv.showcase.entity.foundation.variable.DictionaryCategory;
import com.github.dactiv.showcase.service.foundation.SystemVariableManager;

/**
 * 字典类别管理Controller
 * 
 * @author maurice
 *
 */
@Controller
@OperatingAudit("字典类别管理")
@RequestMapping("/foundation/variable/dictionary-category")
public class DictionaryCategoryController {
	
	@Autowired
	private SystemVariableManager systemDictionaryManager;
	
	/**
	 * 获取字典类别列表,返回foundation/variable/data-dictionary/view.html页面
	 * 
	 * @return List
	 */
	@RequestMapping("view")
	public List<DictionaryCategory> view() {
		//查询所有父类的字典类别
		return systemDictionaryManager.getParentDictionaryCategories();
	}
	
	/**
	 * 
	 * 保存或更新字典类别,保存成功后重定向到:foundation/variable/dictionary-category/view
	 * 
	 * @param entity 实体信息
	 * @param parentId 所对应的父类id
	 * @param redirectAttributes spring mvc 重定向属性
	 * 
	 * @return String
	 */
	@RequestMapping("save")
	@OperatingAudit(function="保存或更新字典类别")
	public String save(@ModelAttribute("entity") @Valid DictionaryCategory 
					   entity,String parentId,
					   RedirectAttributes redirectAttributes) {
		
		if (StringUtils.isEmpty(parentId)) {
			entity.setParent(null);
		} else {
			entity.setParent(systemDictionaryManager.getDictionaryCategory(parentId));
		}
		
		systemDictionaryManager.saveDictionaryCategory(entity);
		redirectAttributes.addFlashAttribute("success", "保存成功");
		return "redirect:/foundation/variable/dictionary-category/view";
	}
	
	/**
	 * 
	 * 读取字典类别,返回foundation/variable/dictionary-category/read.html页面
	 * 
	 * @param id 主键id
	 * @param model Spring mvc的Model接口，主要是将model的属性返回到页面中
	 * 
	 * @return {@link DictionaryCategory}
	 */
	@RequestMapping("read")
	public void read(String id, Model model) {
		
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		
		if (StringUtils.isNotEmpty(id)) {
			filters.add(PropertyFilters.build("NES_id", id));
		}
		//展示父类下来框时，不要连自己也在下拉框里
		model.addAttribute("categoriesList", systemDictionaryManager.getDictionaryCategories(filters));
		
	}
	
	/**
	 * 通过主键id集合删除字典类别,删除成功后重定向到:foundation/variable/dictionary-category/view
	 * 
	 * @param ids 主键id集合
	 * @param redirectAttributes spring mvc 重定向属性
	 * 
	 * @return String
	 */
	@RequestMapping("delete")
	@OperatingAudit(function="删除字典类别")
	public String delete(@RequestParam("ids")List<String> ids,RedirectAttributes redirectAttributes) {
		systemDictionaryManager.deleteDictionaryCategory(ids);
		redirectAttributes.addFlashAttribute("success", "删除" + ids.size() + "条信息成功");
		return "redirect:/foundation/variable/dictionary-category/view";
	}
	
	/**
	 * 绑定实体数据，如果存在id时获取后从数据库获取记录，进入到相对的C后在将数据库获取的记录填充到相应的参数中
	 * 
	 * @param id 主键ID
	 * 
	 */
	@ModelAttribute("entity")
	public DictionaryCategory bindingModel(String id) {

		DictionaryCategory category = new DictionaryCategory();
		
		if (StringUtils.isNotEmpty(id)) {
			category = systemDictionaryManager.getDictionaryCategory(id);
		}

		return category;
	}
}
