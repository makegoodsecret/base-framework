package com.github.dactiv.orm.core.spring.data.jpa.interceptor;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import com.github.dactiv.common.utils.ConvertUtils;
import com.github.dactiv.common.utils.ReflectionUtils;
import com.github.dactiv.orm.annotation.TreeEntity;
import com.github.dactiv.orm.core.spring.data.jpa.repository.support.JpaSupportRepository;
import com.github.dactiv.orm.interceptor.OrmDeleteInterceptor;
import com.github.dactiv.orm.interceptor.OrmSaveInterceptor;

/**
 * 树形实体拦截器
 * 
 * @author maurice
 * 
 * @param <E>
 *            持久化对象类型
 * @param <ID>
 *            id主键类型
 */
public class TreeEntityInterceptor<E, ID extends Serializable> implements
		OrmSaveInterceptor<E, JpaSupportRepository<E, ID>>,
		OrmDeleteInterceptor<E, JpaSupportRepository<E, ID>>{

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmSaveInterceptor#onSave(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	public boolean onSave(E entity, JpaSupportRepository<E, ID> persistentContext,Serializable id) {

		Class<?> entityClass = ReflectionUtils.getTargetClass(entity);
		TreeEntity treeEntity = ReflectionUtils.getAnnotation(entityClass,TreeEntity.class);
		
		if (treeEntity != null) {
			E parent = ReflectionUtils.invokeGetterMethod(entity,treeEntity.parentProperty());
			// 如果对象父类不为null，将父类的leaf设置成true，表示父类下存在子节点
			if (parent != null) {
				Object value = ConvertUtils.convertToObject(treeEntity.hasLeafValue(), treeEntity.leafClass());
				ReflectionUtils.invokeSetterMethod(parent,treeEntity.leafProperty(), value);
			}
		}

		return Boolean.TRUE;
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmSaveInterceptor#onPostSave(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void onPostSave(E entity,JpaSupportRepository<E, ID> persistentContext,Serializable id) {
		
		Class<?> entityClass = ReflectionUtils.getTargetClass(entity);
		TreeEntity treeEntity = ReflectionUtils.getAnnotation(entityClass,TreeEntity.class);

		if (treeEntity != null) {
			//from {0} tree where tree.{1} = {2} and (select count(c) from {0} c where c.{3}.{4} = r.{4}) = {5}
			String hql = MessageFormat.format(treeEntity.refreshHql(),
					persistentContext.getEntityName(),
					treeEntity.leafProperty(),
					treeEntity.hasLeafValue(),
					treeEntity.parentProperty(), 
					persistentContext.getIdName(), 
					treeEntity.notHasLeafValue());
			
			List<E> list = persistentContext.getEntityManager().createQuery(hql).getResultList();

			for (E e : list) {
				Object value = ConvertUtils.convertToObject(treeEntity.notHasLeafValue(), treeEntity.leafClass());
				ReflectionUtils.invokeSetterMethod(e,treeEntity.leafProperty(), value);
				persistentContext.save(e);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmDeleteInterceptor#onDelete(java.io.Serializable, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean onDelete(Serializable id, E entity, JpaSupportRepository<E, ID> persistentContext) {
		
		return Boolean.TRUE;
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmDeleteInterceptor#onPostDelete(java.io.Serializable, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onPostDelete(Serializable id, E entity, JpaSupportRepository<E, ID> persistentContext) {
		onPostSave(entity, persistentContext, id);
		
	}

}
