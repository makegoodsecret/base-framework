package com.github.dactiv.showcase.service.account;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.github.dactiv.common.utils.CollectionUtils;
import com.github.dactiv.showcase.common.SessionVariable;
import com.github.dactiv.showcase.common.enumeration.entity.ResourceType;
import com.github.dactiv.showcase.entity.account.Resource;
import com.google.common.collect.Lists;

/**
 * apache shiro 的公用授权类
 * 
 * @author maurice
 *
 */
public abstract class AuthorizationRealm extends AuthorizingRealm{

	@Autowired
	private AccountManager accountManager;
	
	private List<String> defaultPermission = Lists.newArrayList();
	
	private List<String> defaultRole = Lists.newArrayList();
	
	/**
	 * 设置默认permission
	 * 
	 * @param defaultPermissionString permission 如果存在多个值，使用逗号","分割
	 */
	public void setDefaultPermissionString(String defaultPermissionString) {
		String[] perms = StringUtils.split(defaultPermissionString,",");
		CollectionUtils.addAll(defaultPermission, perms);
	}
	
	/**
	 * 设置默认role
	 * 
	 * @param defaultRoleString role 如果存在多个值，使用逗号","分割
	 */
	public void setDefaultRoleString(String defaultRoleString) {
		String[] roles = StringUtils.split(defaultRoleString,",");
		CollectionUtils.addAll(defaultRole, roles);
	}
	
	/**
	 * 设置默认permission
	 * 
	 * @param defaultPermission permission
	 */
	public void setDefaultPermission(List<String> defaultPermission) {
		this.defaultPermission = defaultPermission;
	}
	
	/**
	 * 设置默认role
	 * 
	 * @param defaultRole role
	 */
	public void setDefaultRole(List<String> defaultRole) {
		this.defaultRole = defaultRole;
	}

	/**
	 * 
	 * 当用户进行访问链接时的授权方法
	 * 
	 */
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        SessionVariable model = principals.oneByType(SessionVariable.class);
        
        Assert.notNull(model, "找不到principals中的SessionVariable");
        
        String id = model.getUser().getId();
        
        //加载用户资源信息
        List<Resource> authorizationInfo = accountManager.getUserResources(id);
        List<Resource> resourcesList = mergeResourcesToParent(authorizationInfo, ResourceType.Security);
        
        model.setAuthorizationInfo(authorizationInfo);
        model.setMenusList(resourcesList);
        
        //添加用户拥有的permission
        addPermissions(info,authorizationInfo);
        
        return info;
	}
	
	/**
	 * 并合子类资源到父类中，返回一个新的资源集合
	 * 
	 * @param list 资源集合
	 * @param resourceType 不需要并合的资源类型
	 */
	public List<Resource> mergeResourcesToParent(List<Resource> list,ResourceType ignoreType) {
		List<Resource> result = new ArrayList<Resource>();
		
		for (Resource r : list) {
			if (r.getParent() == null && !StringUtils.equals(ignoreType.getValue(),r.getType())) {
				mergeResourcesToParent(list,r,ignoreType);
				result.add(r);
			}
		}
		
		return result;
	}
	
	/**
	 * 遍历list中的数据,如果数据的父类与parent相等，将数据加入到parent的children中
	 * 
	 * @param list 资源集合
	 * @param parent 父类对象
	 * @param ignoreType 不需要加入到parent的资源类型
	 */
	private void mergeResourcesToParent(List<Resource> list, Resource parent,ResourceType ignoreType) {
		if (!parent.getLeaf()) {
			return ;
		}
		
		parent.setChildren(new ArrayList<Resource>());
		parent.setLeaf(false);
		
		for (Resource r: list) {
			//这是一个递归过程，如果当前遍历的r资源的parentId等于parent父类对象的id，将会在次递归r对象。通过遍历list是否也存在r对象的子级。
			if (!StringUtils.equals(r.getType(), ignoreType.getValue()) && StringUtils.equals(r.getParentId(),parent.getId()) ) {
				r.setChildren(null);
				mergeResourcesToParent(list,r,ignoreType);
				parent.getChildren().add(r);
				parent.setLeaf(true);
			}
			
		}
	}
	
	/**
	 * 通过资源集合，将集合中的permission字段内容解析后添加到SimpleAuthorizationInfo授权信息中
	 * 
	 * @param info SimpleAuthorizationInfo
	 * @param authorizationInfo 资源集合
	 */
	private void addPermissions(SimpleAuthorizationInfo info,List<Resource> authorizationInfo) {
		//解析当前用户资源中的permissions
        List<String> temp = CollectionUtils.extractToList(authorizationInfo, "permission", true);
        List<String> permissions = getValue(temp,"perms\\[(.*?)\\]");
       
        //添加默认的permissions到permissions
        if (CollectionUtils.isNotEmpty(defaultPermission)) {
        	CollectionUtils.addAll(permissions, defaultPermission.iterator());
        }
        
        //将当前用户拥有的permissions设置到SimpleAuthorizationInfo中
        info.addStringPermissions(permissions);
		
	}
	
	/**
	 * 通过正则表达式获取字符串集合的值
	 * 
	 * @param obj 字符串集合
	 * @param regex 表达式
	 * 
	 * @return List
	 */
	private List<String> getValue(List<String> obj,String regex){

        List<String> result = new ArrayList<String>();
        
		if (CollectionUtils.isEmpty(obj)) {
			return result;
		}
		
		Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(StringUtils.join(obj, ","));
        
        while(matcher.find()){
        	result.add(matcher.group(1));
        }
        
		return result;
	}
}
