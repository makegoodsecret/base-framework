package com.github.dactiv.orm.core.spring.data.jpa.interceptor;

import java.io.Serializable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.dactiv.common.utils.ReflectionUtils;
import com.github.dactiv.orm.annotation.SecurityCode;
import com.github.dactiv.orm.core.exception.SecurityCodeNotEqualException;
import com.github.dactiv.orm.core.spring.data.jpa.repository.support.JpaSupportRepository;
import com.github.dactiv.orm.interceptor.OrmSaveInterceptor;

/**
 * 安全码拦截器
 * 
 * @author maurice
 *
 * @param <E> 持久化对象类型
 * @param <ID> id主键类型
 */
@SuppressWarnings("unchecked")
public class SecurityCodeInterceptor<E,ID extends Serializable> implements 
		OrmSaveInterceptor<E, JpaSupportRepository<E,ID>> {

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmSaveInterceptor#onSave(java.lang.Object, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public boolean onSave(E entity, JpaSupportRepository<E, ID> persistentContext,Serializable id) {
		
		Class<?> entityClass = ReflectionUtils.getTargetClass(entity);
		SecurityCode securityCode = ReflectionUtils.getAnnotation(entityClass,SecurityCode.class);
		
		if (persistentContext.getEntityInformation().isNew(entity)) {
			String code = generateSecurityCode(entity,securityCode);
			ReflectionUtils.invokeSetterMethod(entity, securityCode.value(), code);
		} else {
			E e = persistentContext.findOne((ID) id);
			String originalCode = ReflectionUtils.invokeGetterMethod(e, securityCode.value());
			String currentCode = generateSecurityCode(e, securityCode);
			
			if (!StringUtils.equals(originalCode, currentCode)) {
				throw new SecurityCodeNotEqualException("安全码不正确,原始码为:" + originalCode + "当前对象的安全码为:" + currentCode);
			}
			
			ReflectionUtils.invokeSetterMethod(entity, securityCode.value(), currentCode);
		}
		
		return Boolean.TRUE;
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmSaveInterceptor#onPostSave(java.lang.Object, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public void onPostSave(E entity,JpaSupportRepository<E, ID> persistentContext, Serializable id) {
		
	}

	/**
	 * 生成安全码,返回一个md5字符串
	 * 
	 * @param entity 实体
	 * @param securityCode 安全码注解
	 * 
	 * @return String
	 */
	private String generateSecurityCode(E entity, SecurityCode securityCode) {
		StringBuffer sb = new StringBuffer();
		
		String idProerty = securityCode.idProperty();
		Object idValue = ReflectionUtils.invokeGetterMethod(entity, idProerty);
		
		sb.append(idValue);
		
		for (String s : securityCode.properties()) {
			Object value = ReflectionUtils.invokeGetterMethod(entity, s);
			sb.append(value);
		}
		
		return DigestUtils.md5Hex(sb.toString().getBytes());
	}
	
}
