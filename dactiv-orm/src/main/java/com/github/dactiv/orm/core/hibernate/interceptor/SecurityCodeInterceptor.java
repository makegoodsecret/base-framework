package com.github.dactiv.orm.core.hibernate.interceptor;

import java.io.Serializable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.dactiv.common.utils.ReflectionUtils;
import com.github.dactiv.orm.annotation.SecurityCode;
import com.github.dactiv.orm.core.exception.SecurityCodeNotEqualException;
import com.github.dactiv.orm.core.hibernate.support.BasicHibernateDao;
import com.github.dactiv.orm.interceptor.OrmInsertInterceptor;
import com.github.dactiv.orm.interceptor.OrmSaveInterceptor;
import com.github.dactiv.orm.interceptor.OrmUpdateInterceptor;

@SuppressWarnings("unchecked")
public class SecurityCodeInterceptor<E,PK extends Serializable> implements 
		OrmSaveInterceptor<E, BasicHibernateDao<E,PK>>,
		OrmInsertInterceptor<E, BasicHibernateDao<E,PK>>,
		OrmUpdateInterceptor<E, BasicHibernateDao<E,PK>> {

	@Override
	public boolean onSave(E entity, BasicHibernateDao<E, PK> persistentContext,Serializable id) {
		
		Class<?> entityClass = ReflectionUtils.getTargetClass(entity);
		SecurityCode securityCode = ReflectionUtils.getAnnotation(entityClass,SecurityCode.class);
		
		if (securityCode != null) {
			if (id == null || id.toString().equals("")) {
				String code = generateSecurityCode(entity,securityCode);
				ReflectionUtils.invokeSetterMethod(entity, securityCode.value(), code);
			} else {
				E e = persistentContext.get((PK) id);
				String originalCode = ReflectionUtils.invokeGetterMethod(e, securityCode.value());
				String currentCode = generateSecurityCode(e, securityCode);
				
				if (!StringUtils.equals(originalCode, currentCode)) {
					throw new SecurityCodeNotEqualException("安全码不正确,原始码为:" + originalCode + "当前对象的安全码为:" + currentCode);
				}
				
				ReflectionUtils.invokeSetterMethod(entity, securityCode.value(), currentCode);
			}
		}
		
		
		return Boolean.TRUE;
	}

	@Override
	public void onPostSave(E entity,BasicHibernateDao<E, PK> persistentContext, Serializable id) {
		
	}

	@Override
	public boolean onInsert(E entity, BasicHibernateDao<E, PK> persistentContext) {
		return onSave(entity,persistentContext,null);
	}

	@Override
	public void onPostInsert(E entity,BasicHibernateDao<E, PK> persistentContext, Serializable id) {
		
	}

	@Override
	public boolean onUpdate(E entity,BasicHibernateDao<E, PK> persistentContext, Serializable id) {
		return onSave(entity,persistentContext,id);
	}

	@Override
	public void onPostUpdate(E entity,BasicHibernateDao<E, PK> persistentContext, Serializable id) {
		
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
