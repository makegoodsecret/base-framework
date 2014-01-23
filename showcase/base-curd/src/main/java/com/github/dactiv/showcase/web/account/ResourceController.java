package com.github.dactiv.showcase.web.account;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.NamedFilterList;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.github.dactiv.common.utils.CollectionUtils;
import com.github.dactiv.common.utils.ReflectionUtils;
import com.github.dactiv.showcase.common.SystemVariableUtils;
import com.github.dactiv.showcase.common.annotation.OperatingAudit;
import com.github.dactiv.showcase.common.enumeration.entity.ResourceType;
import com.github.dactiv.showcase.entity.account.Resource;
import com.github.dactiv.showcase.service.account.AccountManager;
import com.google.common.collect.Lists;

/**
 * 资源管理Controller
 * 
 * @author maurice
 *
 */
@Controller
@OperatingAudit("资源管理")
@RequestMapping(value="/account/resource")
public class ResourceController {

	@Autowired
	private AccountManager accountManager;
	
	@Autowired
	private AbstractShiroFilter shiroFilter;
	
	private static Logger logger = LoggerFactory.getLogger(ResourceController.class);
	
	/**
	 * 获取资源列表,返回account/resource/view.html页面
	 * 
	 * @return List
	 */
	@RequestMapping("view")
	public List<Resource> view() {
		return accountManager.getParentResources();
	}
	
	/**
	 * 
	 * 保存或更新资源,保存成功后重定向到:account/resource/view
	 * 
	 * @param entity 实体信息
	 * @param parentId 所对应的父类id
	 * @param redirectAttributes spring mvc 重定向属性
	 * 
	 * @return String
	 */
	@RequestMapping("save")
	@OperatingAudit(function="保存或更新资源")
	public String save(@ModelAttribute("entity") @Valid Resource entity,
					   String parentId,
					   RedirectAttributes redirectAttributes) {
		
		if (StringUtils.isEmpty(parentId)) {
			entity.setParent(null);
		} else {
			Resource parent = accountManager.getResource(parentId);
			entity.setParent(parent);
		}
		
		accountManager.saveResource(entity);
		redirectAttributes.addFlashAttribute("success", "保存成功");
		
		try {
			refreshShiroFilterChain(Lists.newArrayList(entity),false);
		} catch(Exception ex) {
			logger.warn("在保存资源后更新shiro finter chain不成功:" + ex.getMessage());
		}
		
		return "redirect:/account/resource/view";
	}
	
	/**
	 * 辅助save和delete方法的刷新shiro的filter chain,因为本次保存资源或
	 * 删除资源操作很有可能添加了新的链接或者更新了链接的filter，所以立即
	 * 更新shiro的chain信息让shiro立即知道如何控制权限问题。
	 * 
	 * @param resources 资源集合
	 * @param delete 是否删除shiro filter chain的
	 */
	private void refreshShiroFilterChain(List<Resource> resources,boolean delete) {
		
		if (CollectionUtils.isEmpty(resources)) {
			return ;
		}
		
		for (Resource entity : resources) {
			
			if (StringUtils.isEmpty(entity.getValue()) && StringUtils.isEmpty(entity.getPermission())) {
				continue;
			}
			
			//获取当前shiro的filterChain
			PathMatchingFilterChainResolver filterChainResolver = null;
			filterChainResolver = (PathMatchingFilterChainResolver) shiroFilter.getFilterChainResolver();
			//获取chainManager
			FilterChainManager chainManager = filterChainResolver.getFilterChainManager();
			
			if(delete) {//如果是删除shiro的filter chain
				
				//获取chainManager的filterChains
				Map<String, NamedFilterList> map = ReflectionUtils.getFieldValue(chainManager, "filterChains");
				//如果存在就删除
				if (map.containsKey(entity.getValue())) {
					map.remove(entity.getValue());
				}
				
			} else {//如果不是删除shiro的filter chain
				
				//通过resource的value获取对应的shirofilter
				NamedFilterList namedFilterList = chainManager.getChain(entity.getValue());
				//如果存在了对应的filter将当前filter直接删除
				if (CollectionUtils.isNotEmpty(namedFilterList)) {
					namedFilterList.removeAll(namedFilterList);
				} 
				//在创建一个新的filter到filterChains中
				chainManager.createChain(entity.getValue(), entity.getPermission());
				
			}
			
		}
	}
	
	/**
	 * 
	 * 读取资源信息,返回account/resource/read.html页面
	 * 
	 * @param id 主键id
	 * @param model Spring mvc的Model接口，主要是将model的属性返回到页面中
	 * 
	 * @return {@link Resource}
	 */
	@RequestMapping("read")
	public void read(String id, Model model) {
		model.addAttribute("resourceType", SystemVariableUtils.getVariables(ResourceType.class));
		model.addAttribute("resourcesList", accountManager.getResources(id));
	}
	
	/**
	 * 通过主键id集合删除资源,删除成功后重定向到:account/resource/view
	 * 
	 * @param ids 主键id集合
	 * @param redirectAttributes spring mvc 重定向属性
	 * 
	 * @return String
	 */
	@RequestMapping("delete")
	@OperatingAudit(function="删除资源")
	public String delete(@RequestParam("ids")List<String> ids,RedirectAttributes redirectAttributes) {
		
		List<Resource> resources = accountManager.getResources(ids);
		accountManager.deleteResources(resources);
		redirectAttributes.addFlashAttribute("success", "删除" + ids.size() + "条信息成功");
		
		try {
			refreshShiroFilterChain(resources,true);
		} catch(Exception ex) {
			logger.warn("在删除资源后更新shiro finter chain不成功:" + ex.getMessage());
		}
		
		return "redirect:/account/resource/view";
	}
	
	/**
	 * 绑定实体数据，如果存在id时获取后从数据库获取记录，进入到相对的C后在将数据库获取的记录填充到相应的参数中
	 * 
	 * @param id 主键ID
	 * 
	 */
	@ModelAttribute("entity")
	public Resource bindingModel(String id) {

		Resource resource = new Resource();
		if (StringUtils.isNotEmpty(id)) {
			resource = accountManager.getResource(id);
		} else {
			resource.setSort(accountManager.getResourceCount() + 1);
		}

		return resource;
	}
	
}
