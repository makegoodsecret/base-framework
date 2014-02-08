#### 2 dactiv orm 使用说明 ####

dactiv orm 是对持久化层所使用到的框架进行封装，让使用起来不在写如此多的繁琐代码，目前dactiv orm 只支持了 hibernate 4 和 spring data jpa。

dactiv orm 对 hibernate 和 spring data jpa 的修改并不多，常用的方法和往常一样使用，除了 hibernate 的 save 方法改名为insert(其实save也是起到了insert的作用，从字面上，insert更加形容了hibernate save方法的作用)其他都和往常一样使用。主要是添加了一些注解和一些简单的执行某些方法前后做了一个拦截处理，以及添加一个灵活的属性查询功能。

##### 2.1 使用 hibernate #####

在使用 hibernate 时，主要关注几个类：

1. BasicHibernateDao
1. HibernateSupportDao

**BasicHibernateDao**：是对 hibernate 封装的基础类,包含对 hibernate 的基本CURD和其他 hibernate 的查询操作，该类是一个可以支持泛型的CURD类，要是在写dao时的继承体，泛型参数中的 T 为 orm 对象实体类型，PK为实体的主键类型。在类中的 sessionFactory 已经使用了自动写入：

	@Autowired(required = false)
	public void setSessionFactory(SessionFactory sessionFactory) {
	    this.sessionFactory = sessionFactory;
	}

只要用spring配置好sessionFactory就可以继承使用。

**applicationContext.xml:**

	<!-- Hibernate配置 -->
	<bean id="sessionFactory" class="org.springframework.orm.hibernate4.LocalSessionFactoryBean">
		<property name="dataSource" ref="dataSource" />
		<property name="namingStrategy">
			<bean class="org.hibernate.cfg.ImprovedNamingStrategy" />
		</property>
		<property name="hibernateProperties">
			<props>
				<prop key="hibernate.dialect">${hibernate.dialect}</prop>
				<prop key="hibernate.show_sql">${hibernate.show_sql}</prop>
				<prop key="hibernate.format_sql">${hibernate.format_sql}</prop>
			</props>
		</property>
		<property name="packagesToScan" value="com.github.dactiv.showcase.entity" />
	</bean>

**UserDao**

	public class UserDao extends BasicHibernateDao<User, String> {
	
	}

##### 2.2 使用 spring data jpa #####
在使用 spring data jpa 时，
