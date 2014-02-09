### 2 dactiv orm 使用说明 ###

dactiv orm 是对持久化层所使用到的框架进行封装，让使用起来不在写如此多的繁琐代码，目前dactiv orm 只支持了 hibernate 4 和 spring data jpa。

dactiv orm 对 hibernate 和 spring data jpa 的修改并不多，常用的方法和往常一样使用，除了 hibernate 的 save 方法改名为insert(其实save也是起到了insert的作用，从字面上，insert更加形容了hibernate save方法的作用)其他都和往常一样使用。主要是添加了一些注解和一些简单的执行某些方法前后做了一个拦截处理，以及添加一个灵活的属性查询功能。

#### 2.1 使用 hibernate ####

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

**UserDao：**

	public class UserDao extends BasicHibernateDao<User, String> {
	
	}

#### 2.2 使用 spring data jpa ####

在使用 spring data jpa 时，主要关注BasicJpaRepository这个接口，该接口添加了支持PropertyFilter的方法，可以直接使用，但需要添加配置，要使用到BasicJpaRepository需要在spring data jpa配置文件中对jpa:repositories的factory-class属性添加一个类:org.exitsoft.orm.core.spring.data.jpa.factory.BasicRepositoryFactoryBean:

	<jpa:repositories base-package="你的repository包路径" 
	                              transaction-manager-ref="transactionManager" 
	                              factory-class="org.exitsoft.orm.core.spring.data.jpa.factory.BasicRepositoryFactoryBean"
	                              entity-manager-factory-ref="entityManagerFactory"  />

如果觉得麻烦，不配置一样能够使用PropertyFilter来做查询操作：

	Specifications.get(Lists.newArrayList(
		PropertyFilters.get("LIKES_loginName", "m"),
		PropertyFilters.get("EQI_state", "1")
	));

该方法会返回一个Specification接口，使用spring data jpa 原生的api findAll方法可以直接使用,执行查询操作:

	repository.findAll(Specifications.get(Lists.newArrayList(
		PropertyFilters.get("LIKES_loginName", "m"),
		PropertyFilters.get("EQI_state", "1")
	)));

#### 2.3 PropertyFilter 查询表达式说明 ####

在 dactiv orm 里，对 hibernate 和 spring data jpa 都扩展了一套**查询表达式**，是专门用来应付一些比较简单的查询而不用写语句的功能。通过该表达式，dactiv orm 能够解析出最终的查询语句去让 hibernate 或 spring data jpa 去执行，需要使用该表达式，如果是用 hibernate 需要集成 **HibernateSupportDao** 类，如果使用 spring data jpa 的话需要使用到 **Specifications.get()** 方法去构造 spring data jpa 的 **Specification** 后才能执行查询，或者根据 **2.2 使用 spring data jpa** 配置完成后，集成BasicJpaRepository 接口，里面就提供了支持 PropertyFilter 的查询方法。

该表达式的规则为：**<约束名称><属性类型>_<属性名称>**，例如现在有个用户实体：

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	public class User implements Serializable {
		private String id;//主键id
	    private String username;//登录名称
	    private String password;//登录密码
	    private String realname;//真实名称
	    private Integer state;//状态 
	    private String email;//邮件
	
	    //----------GETTER/SETTER-----------//
	}

通过查询表达式来查询username等于a的用户可以这样写：

**hibernate**：

	public class UserDao extends HibernateSupportDao<User, String>{
	
	}

***

	List<PropertyFilter> filters = Lists.newArrayList(
	    //<约束名称><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a")
	);
	userDao.findByPropertyFilter(filters);

**spring data jpa**：

	public interface UserRepository extends JpaRepository<User, String>,JpaSpecificationExecutor<User>{
	
	}
	
***
	userRepository.findAll(Specifications.get(Lists.newArrayList(
		//<约束名称><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a")
	)));

查询username等于a的并且realname等于c的用户可以通过多个条件进行and关系查询：

**hibernate**：

	public class UserDao extends HibernateSupportDao<User, String>{
	
	}

***

	List<PropertyFilter> filters = Lists.newArrayList(
	    //<约束名称><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a"),
		PropertyFilters.get("EQS_realname", "c")
	);
	userDao.findByPropertyFilter(filters);

**spring data jpa**：

	public interface UserRepository extends JpaRepository<User, String>,JpaSpecificationExecutor<User>{
	
	}
	
***
	userRepository.findAll(Specifications.get(Lists.newArrayList(
		//<约束名称><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a"),
		PropertyFilters.get("EQS_realname", "c")
	)));

了解一些功能后，解释一下 **<约束名称><属性类型>_<属性名称>** 应该怎么写。

**约束名称**：约束名称是表达式第一个参数的必须条件,在这里的约束名称是指通过什么条件去做查询，如等于、不等于、包含(in)、大于、小于...等等。

约束名称描述列表:
<table>
	<tr>
		<th>
		约束名称
		</th>
		<th>
		描述
		</th>
	</tr>
	<tr>
		<td>
			EQ
		</td>
		<td>
			等于约束 (from object o where o.value = ?)如果为"null"就是 (from object o where o.value is null)
		</td>
	</tr>
	<tr>
		<td>
			NE
		</td>
		<td>
			不等于约束 (from object o where o.value &lt;&gt; ?) 如果为"null"就是 (from object o where o.value is not null)
		</td>
	</tr>
	<tr>
		<td>
			IN
		</td>
		<td>
			包含约束 (from object o where o.value in (?,?,?,?,?))
		</td>
	</tr>
	<tr>
		<td>
			NIN
		</td>
		<td>
			不包含约束 (from object o where o.value not in (?,?,?,?,?))
		</td>
	</tr>
	<tr>
		<td>
			GE
		</td>
		<td>
			大于等于约束 (from object o where o.value &gt;= ?)
		</td>
	</tr>
	<tr>
		<td>
			GT
		</td>
		<td>
			大于约束 (from object o where o.value &gt; ?)
		</td>
	</tr>
	<tr>
		<td>
			LE
		</td>
		<td>
			小于等于约束 ( from object o where o.value &lt;= ?)
		</td>
	</tr>
	<tr>
		<td>
			LT
		</td>
		<td>
			小于约束 ( from object o where o.value &lt; ?)
		</td>
	</tr>
	<tr>
		<td>
			LIKE
		</td>
		<td>
			模糊约束 ( from object o where o.value like '%?%') 
		</td>
	</tr>
	<tr>
		<td>
			LLIKE
		</td>
		<td>
			左模糊约束 ( from object o where o.value like '%?')
		</td>
	</tr>
	<tr>
		<td>
			RLIKE
		</td>
		<td>
			右模糊约束 ( from object o where o.value like '?%')
		</td>
	</tr>
</table>

**属性类型**：属性类型是表达式第二个参数的必须条件，表示表达式的属性值是什么类型的值。因为在使用表达式查询时，参数都是String类型的参数，所以必须根据你指定的类型才能自动转为该类型的值,属性类型的描述用一个枚举类来表示，就是 dactiv common 下的 FieldType枚举：

	/**
	 * 属性数据类型
	 * S代表String,I代表Integer,L代表Long, N代表Double, D代表Date,B代表Boolean
	 * 
	 * @author calvin
	 * 
	 */
	public enum FieldType {
		
		/**
		 * String
		 */
		S(String.class),
		/**
		 * Integer
		 */
		I(Integer.class),
		/**
		 * Long
		 */
		L(Long.class),
		/**
		 * Double
		 */
		N(Double.class), 
		/**
		 * Date
		 */
		D(Date.class), 
		/**
		 * Boolean
		 */
		B(Boolean.class);
	
		//类型Class
		private Class<?> fieldClass;
	
		private FieldType(Class<?> fieldClass) {
			this.fieldClass = fieldClass;
		}
		
		/**
		 * 获取类型Class
		 * 
		 * @return Class
		 */
		public Class<?> getValue() {
			return fieldClass;
		}
	}

如，用户对象中实体描述：

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	public class User implements Serializable {
		private String id;//主键id
		private String username;//登录名称
		private String password;//登录密码
		private String realname;//真实名称
		private Integer state;//状态
		private String email;//邮件
	
		//----------GETTER/SETTER-----------//
	}


假如我想查用户状态不等于3的可以写成:
	
	PropertyFilters.get("NEI_state", "3")

假如我想查用户的登录名称等于a的可以写成：

	PropertyFilters.get("EQS_username", "a")
	
**属性名称**：属性名称就是实体的属性名，但是要注意的是，通过表达式不能支持别名查询如：

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	public class User implements Serializable {
		private String id;//主键id
	    private String username;//登录名称
	    private String password;//登录密码
	    private String realname;//真实名称
	    private Integer state;//状态
	    private String email;//邮件
	    private List<Group> groupsList = new ArrayList<Group>();//用户所在的组
	    //----------GETTER/SETTER-----------//
	}

想通过PropertyFilters.get("EQS_groupsList.name", "a")就报错，但如果是一对多并且“多”这方的实体在User里面可以通过id查询出来，如：

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	public class User implements Serializable {
		private String id;//主键id
	    private String username;//登录名称
	    private String password;//登录密码
	    private String realname;//真实名称
	    private Integer state;//状态
	    private String email;//邮件
	    private Group group;//用户所在的组
	    //----------GETTER/SETTER-----------//
	}

***
	PropertyFilters.get("EQS_group.id", "1")

如果想使用别名的查询，可以通过重写的方法来实现该功能，以 hibernate 为例子，HibernateSupportDao的表达式查询方法其实都是在用QBC形式的查询，就是Criteria。在该类中都是通过:**createCriteria**创建Criteria的,所以。在UserDao中重写该方法就可以实现别名查询了,如:

	public class UserDao extends HibernateSupportDao<User, String>{
	
	    protected Criteria createCriteria(List<PropertyFilter> filters,Order ...orders) {
	        Criteria criteria = super.createCriteria(filters, orders);
	        criteria.createAlias("groupsList", "gl");
	        return criteria;
	    }
	}

***
	PropertyFilters.get("EQS_groupsList.name", "mm")

**表达式and与or的多值写法**：有时候会通过表达式去查询某个属性等于多个值或者多个属性等于某个值的需要，在某个属性等于多个值时，如果用and查询的话把值用逗号","分割，如果用or查询的话把值用横杠"|"分割。如:

**and**：

	PropertyFilters.get("EQS_username", "1,2,3")

**or**:

    PropertyFilters.get("EQS_username", "1|2|3")

在需要查询多个属性等于某个值时，使用**OR**分隔符隔开这些属性：

	PropertyFilters.get("EQS_username_OR_email", "xxx@xxx.xx");

#### 2.4 页面多条件查询 ####

多条件以及分页查询，可能每个项目中都会使用，但是查询条件千变万化，当某时客户要求添加多一个查询条件时，繁琐的工作会很多，但使用 PropertyFilter 会为你减少一些复制粘贴的动作。

以用户实体类例：

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	public class User implements Serializable {
		private String id;//主键id
	    private String username;//登录名称
	    private String password;//登录密码
	    private String realname;//真实名称
	    private Integer state;//状态
	    private String email;//邮件
	    //----------GETTER/SETTER-----------//
	}

先在的查询表单如下：

	<form id="search_form" action="account/user/view" method="post">
        <label for="filter_RLIKES_username">
            登录帐号:
        </label>
        <input type="text" id="filter_RLIKES_username" name="filter_RLIKES_username" />
        <label for="filter_RLIKES_realname">
            真实姓名:
        </label>
        <input type="text" id="filter_RLIKES_realname" name="filter_RLIKES_realname" />
        <label for="filter_RLIKES_email">
            电子邮件:
        </label>
        <input type="text" id="filter_RLIKES_email" name="filter_RLIKES_email" />
        <label for="filter_EQS_state">
            状态:
        </label>
	</form>

注意看每个input的name属性，在input的name里通过filter_做前缀加查询表达式，当表单提交过来时通过一下代码完成查询：

	public class UserDao extends HibernateSupportDao<User, String>{

	}

***

	@RequestMapping("view")
	public Page<User> view(PageRequest pageRequest,HttpServletRequest request) {
		
		List<PropertyFilter> filters = PropertyFilters.get(request, true);
		
		if (!pageRequest.isOrderBySetted()) {
			pageRequest.setOrderBy("id");
			pageRequest.setOrderDir(Sort.DESC);
		}
		
		return userDao.findPage(pageRequest, filters);
	}

当客户在某时想添加一个通过状态查询时，只需要在表单中添加多一个select即可完成查询。

	<form id="search_form" action="account/user/view" method="post">
        <...>
        <select name="filter_EQI_state" id="filter_EQS_state" size="25">
            <option value="">
                全部
            </option>
            <option value="1">
                禁用
            </option>
            <option value="2">
                启用
            </option>
        </select>
	</form>


#### 2.5 扩展表达式的约束名称 ####

如果你在项目开发时觉得表达式里面的约束名称不够用，可以对表达式做扩展处理。扩展约束名称时 spring data jpa 和 hibernate 所关注的类不同。

##### 2.5.1 hiberante扩展表达式的约束名称 #####

要扩展 hibernate 查询表达式的约束名主要关注的类有 **HibernateRestrictionBuilder**, **CriterionBuilder**, **CriterionSingleValueSupport** 以及 **CriterionMultipleValueSupport**。

**HibernateRestrictionBuilder**：HibernateRestrictionBuilder 是装载所有 CriterionBuilder 实现类的包装类，该类有一块静态局域。去初始化所有的约束条件。并提供两个方法去创建 Hibernate 的 Criterion，该类是 HibernateSupportDao 查询表达式查询的关键类。所有通过条件创建的 Criterion 都是通过该类创建。

**CriterionBuilder**：CriterionBuilder是一个构造Hibernate Criterion的类，该类有一个方法专门提供根据 PropertyFilter 该如何创建 hibernate 的 Criterion，而该类有一个抽象类实现了部分方法，就是 CriterionSingleValueSupport，具体 CriterionBuilder 的接口如下：

	public interface CriterionBuilder {
	
		/**
		 * 获取Hibernate的约束标准
		 * 
		 * @param filter 属性过滤器
		 * 
		 * @return {@link Criterion}
		 * 
		 */
		public Criterion build(PropertyFilter filter);
		
		/**
		 * 获取Criterion标准的约束名称
		 * 
		 * @return String
		 */
		public String getRestrictionName();
		
		/**
		 * 获取Hibernate的约束标准
		 * 
		 * @param propertyName 属性名
		 * @param value 值
		 * 
		 * @return {@link Criterion}
		 * 
		 */
		public  Criterion build(String propertyName,Object value);
	}

**CriterionSingleValueSupport**：该类是CriterionBuilder的子类，实现了public Criterion build(PropertyFilter filter)实现体主要是对PropertyFilter的值模型做处理。并且逐个循环调用public Criterion build(String propertyName,Object value)方法给 CriterionSingleValueSupport 实现体做处理。

	public abstract class CriterionSingleValueSupport implements CriterionBuilder{
		
		//or值分隔符
		private String orValueSeparator = "|";
		//and值分隔符
		private String andValueSeparator = ",";
		
		public CriterionSingleValueSupport() {
			
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.hibernate.CriterionBuilder#build(com.github.dactiv.orm.core.PropertyFilter)
		 */
		public Criterion build(PropertyFilter filter) {
			String matchValue = filter.getMatchValue();
			Class<?> FieldType = filter.getFieldType();
			
			MatchValue matchValueModel = getMatchValue(matchValue, FieldType);
			
			Junction criterion = null;
			
			if (matchValueModel.hasOrOperate()) {
				criterion = Restrictions.disjunction();
			} else {
				criterion = Restrictions.conjunction();
			}
			
			for (Object value : matchValueModel.getValues()) {
				
				if (filter.hasMultiplePropertyNames()) {
					List<Criterion> disjunction = new ArrayList<Criterion>();
					for (String propertyName:filter.getPropertyNames()) {
						disjunction.add(build(propertyName,value));
					}
					criterion.add(Restrictions.or(disjunction.toArray(new Criterion[disjunction.size()])));
				} else {
					criterion.add(build(filter.getSinglePropertyName(),value));
				}
				
			}
			
			return criterion;
		}
		
		
		/**
		 * 获取值对比模型
		 * 
		 * @param matchValue 值
		 * @param FieldType 值类型
		 * 
		 * @return {@link MatchValue}
		 */
		public MatchValue getMatchValue(String matchValue,Class<?> FieldType) {
			return MatchValue.createMatchValueModel(matchValue, FieldType,andValueSeparator,orValueSeparator);
		}
	
		/**
		 * 获取and值分隔符
		 * 
		 * @return String
		 */
		public String getAndValueSeparator() {
			return andValueSeparator;
		}
	
		/**
		 * 设置and值分隔符
		 * @param andValueSeparator and值分隔符
		 */
		public void setAndValueSeparator(String andValueSeparator) {
			this.andValueSeparator = andValueSeparator;
		}
		
	}

**CriterionMultipleValueSupport**：该类是 CriterionSingleValueSupport 的子类。重写了CriterionSingleValueSupport类的**public Criterion build(PropertyFilter filter)** 和 **public Criterion build(String propertyName, Object value)** 方法。并且添加了一个抽象方法 **public abstract Criterion buildRestriction(String propertyName,Object[] values)** 。该类主要作用是在多值的情况不逐个循环，而是将所有的参数组合成一个数组传递给抽象方法buildRestriction(String propertyName,Object[] values)中。这种情况在in或not in约束中就用得到。

	public abstract class CriterionMultipleValueSupport extends CriterionSingleValueSupport{
		
		/**
		 * 将得到值与指定分割符号,分割,得到数组
		 *  
		 * @param value 值
		 * @param type 值类型
		 * 
		 * @return Object
		 */
		public Object convertMatchValue(String value, Class<?> type) {
			Assert.notNull(value,"值不能为空");
			String[] result = StringUtils.splitByWholeSeparator(value, getAndValueSeparator());
			
			return  ConvertUtils.convertToObject(result,type);
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.hibernate.restriction.CriterionSingleValueSupport#build(com.github.dactiv.orm.core.PropertyFilter)
		 */
		public Criterion build(PropertyFilter filter) {
			Object value = convertMatchValue(filter.getMatchValue(), filter.getFieldType());
			Criterion criterion = null;
			if (filter.hasMultiplePropertyNames()) {
				Disjunction disjunction = Restrictions.disjunction();
				for (String propertyName:filter.getPropertyNames()) {
					disjunction.add(build(propertyName,value));
				}
				criterion = disjunction;
			} else {
				criterion = build(filter.getSinglePropertyName(),value);
			}
			return criterion;
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.hibernate.CriterionBuilder#build(java.lang.String, java.lang.Object)
		 */
		public Criterion build(String propertyName, Object value) {
			
			return buildRestriction(propertyName, (Object[])value);
		}
		
		
		/**
		 * 
		 * 获取Hibernate的约束标准
		 * 
		 * @param propertyName 属性名
		 * @param values 值
		 * 
		 * @return {@link Criterion}
		 */
		public abstract Criterion buildRestriction(String propertyName,Object[] values);
	}

了解完以上几个类。那么假设现在有个需求。要写一个模糊非约束 (from object o where o.value not like '%?%')来判断一些值，可以通过继承CriterionSingleValueSupport类，实现public Criterion build(String propertyName, Object value)，如：

	/**
	 * 模糊非约束 ( from object o where o.value not like '%?%') RestrictionName:NLIKE
	 * 表达式:NLIKE_属性类型_属性名称[|属性名称...]
	 * 
	 * @author vincent
	 *
	 */
	public class NlikeRestriction extends CriterionSingleValueSupport{
	
	    public final static String RestrictionName = "NLIKE";
	
	    @Override
	    public String getRestrictionName() {
	        return RestrictionName;
	    }
	
	    @Override
	    public Criterion build(String propertyName, Object value) {
	
	        return Restrictions.not(Restrictions.like(propertyName, value.toString(), MatchMode.ANYWHERE));
	    }
	
	}

通过某种方式(如spring的InitializingBean，或serlvet)将该类添加到HibernateRestrictionBuilder的CriterionBuilder中。就可以使用约束名了。

	CriterionBuilder nlikeRestriction= new NlikeRestriction();
	HibernateRestrictionBuilder.getCriterionMap().put(nlikeRestriction.getRestrictionName(), nlikeRestriction);

##### 2.5.2 spring data jpa扩展表达式的约束名称 #####

如果使用 spring data jpa 做 orm 支持时，要扩展查询表达式的约束名主要关注的类有 **JpaRestrictionBuilder**, **PredicateBuilder**, **PredicateSingleValueSupport** 以及 **PredicateMultipleValueSupport**。

**JpaRestrictionBuilder**：JpaRestrictionBuilder 是装载所有 PredicateBuilder 实现的包装类，该类有一块静态局域。去初始化所有的约束条件。并提供两个方法去创建 jpa 的 **Predicate**，该类是 BasicJpaRepository 查询表达式查询的关键类。所有通过 PropertyFilter 创建的 Predicate 都是通过该类创建。

**PredicateBuilder**：PredicateBuilder 是一个构造 jpa Predicate 的类，该类有一个方法专门提供根据 PropertyFilter 该如何创建 jpa 的 Predicate，而该类有一个抽象类实现了部分方法，就是 PredicateSingleValueSupport，具体 PredicateBuilder 的接口如下：

	public interface PredicateBuilder {
	
		/**
		 * 获取Jpa的约束标准
		 * 
		 * @param filter 属性过滤器
		 * @param entity jpa查询绑定载体
		 * 
		 * @return {@link Predicate}
		 * 
		 */
		public Predicate build(PropertyFilter filter,SpecificationEntity entity);
		
		/**
		 * 获取Predicate标准的约束名称
		 * 
		 * @return String
		 */
		public String getRestrictionName();
		
		/**
		 * 获取Jpa的约束标准
		 * 
		 * @param propertyName 属性名
		 * @param value 值
		 * @param entity jpa查询绑定载体
		 * 
		 * @return {@link Predicate}
		 * 
		 */
		public Predicate build(String propertyName, Object value,SpecificationEntity entity);
	}

**PredicateSingleValueSupport**：该类是PredicateBuilder的子类，实现了 **public Predicate build(PropertyFilter filter,SpecificationEntity entity)** 方法，实现体主要是对 PropertyFilter 的值模型做处理。并且逐个循环调用 **public Predicate build(String propertyName, Object value,SpecificationEntity entity)** 方法给实现体做处理：

	public abstract class PredicateSingleValueSupport implements PredicateBuilder{
		
		//or值分隔符
		private String orValueSeparator = "|";
		//and值分隔符
		private String andValueSeparator = ",";
		
		public PredicateSingleValueSupport() {
			
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.spring.data.jpa.PredicateBuilder#build(com.github.dactiv.orm.core.PropertyFilter, javax.persistence.criteria.Root, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.CriteriaBuilder)
		 */
		public Predicate build(PropertyFilter filter,SpecificationEntity entity) {
	
			String matchValue = filter.getMatchValue();
			Class<?> FieldType = filter.getFieldType();
			
			MatchValue matchValueModel = getMatchValue(matchValue, FieldType);
			
			Predicate predicate = null;
			
			if (matchValueModel.hasOrOperate()) {
				predicate = entity.getBuilder().disjunction();
			} else {
				predicate = entity.getBuilder().conjunction();
			}
			
			for (Object value : matchValueModel.getValues()) {
				if (filter.hasMultiplePropertyNames()) {
					for (String propertyName:filter.getPropertyNames()) {
						predicate.getExpressions().add(build(propertyName, value, entity));
					}
				} else {
					predicate.getExpressions().add(build(filter.getSinglePropertyName(), value, entity));
				}
			}
			
			return predicate;
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.spring.data.jpa.PredicateBuilder#build(java.lang.String, java.lang.Object, com.github.dactiv.orm.core.spring.data.jpa.JpaBuilderModel)
		 */
		public Predicate build(String propertyName, Object value,SpecificationEntity entity) {
			
			return build(Specifications.getPath(propertyName, entity.getRoot()),value,entity.getBuilder());
		}
		
		/**
		 * 
		 * 获取Jpa的约束标准
		 * 
		 * @param expression 属性路径表达式
		 * @param value 值
		 * @param builder CriteriaBuilder
		 * 
		 * @return {@link Predicate}
		 */
		public abstract Predicate build(Path<?> expression,Object value,CriteriaBuilder builder);
		
		/**
		 * 获取值对比模型
		 * 
		 * @param matchValue 值
		 * @param FieldType 值类型
		 * 
		 * @return {@link MatchValue}
		 */
		public MatchValue getMatchValue(String matchValue,Class<?> FieldType) {
			return MatchValue.createMatchValueModel(matchValue, FieldType,andValueSeparator,orValueSeparator);
		}
	
		/**
		 * 获取and值分隔符
		 * 
		 * @return String
		 */
		public String getAndValueSeparator() {
			return andValueSeparator;
		}
	
		/**
		 * 设置and值分隔符
		 * @param andValueSeparator and值分隔符
		 */
		public void setAndValueSeparator(String andValueSeparator) {
			this.andValueSeparator = andValueSeparator;
		}
	}

**PredicateMultipleValueSupport**：该类是 PredicateSingleValueSupport 的子类。重写了 PredicateSingleValueSupport 类的**public Predicate build(PropertyFilter filter, SpecificationEntity entity)** 和 **public Predicate build(Path<?> expression, Object value,CriteriaBuilder builder)**。并且添加了一个抽象方法 **public abstract Predicate buildRestriction(Path<?> expression,Object[] values,CriteriaBuilder builder)**。该类主要作用是在多值的情况不逐个循环，而是全部都将参数组合成一个数组传递给抽象方法 **buildRestriction(Path<?> expression,Object[] values,CriteriaBuilder builder)** 中。这种情况在in或not in约束中就用得到。

	public abstract class PredicateMultipleValueSupport extends PredicateSingleValueSupport{
		
		/**
		 * 将得到值与指定分割符号,分割,得到数组
		 *  
		 * @param value 值
		 * @param type 值类型
		 * 
		 * @return Object
		 */
		public Object convertMatchValue(String value, Class<?> type) {
			Assert.notNull(value,"值不能为空");
			String[] result = StringUtils.splitByWholeSeparator(value, getAndValueSeparator());
			
			return  ConvertUtils.convertToObject(result,type);
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.spring.data.jpa.restriction.PredicateSingleValueSupport#build(com.github.dactiv.orm.core.PropertyFilter, com.github.dactiv.orm.core.spring.data.jpa.JpaBuilderModel)
		 */
		public Predicate build(PropertyFilter filter, SpecificationEntity entity) {
			Object value = convertMatchValue(filter.getMatchValue(), filter.getFieldType());
			Predicate predicate = null;
			
			if (filter.hasMultiplePropertyNames()) {
				Predicate orDisjunction = entity.getBuilder().disjunction();
				for (String propertyName:filter.getPropertyNames()) {
					orDisjunction.getExpressions().add(build(propertyName,value,entity));
				}
				predicate = orDisjunction;
			} else {
				predicate = build(filter.getSinglePropertyName(),value,entity);
			}
			
			return predicate;
		}
		
		/*
		 * (non-Javadoc)
		 * @see com.github.dactiv.orm.core.spring.data.jpa.restriction.PredicateSingleValueSupport#build(javax.persistence.criteria.Path, java.lang.Object, javax.persistence.criteria.CriteriaBuilder)
		 */
		public Predicate build(Path<?> expression, Object value,CriteriaBuilder builder) {
			return buildRestriction(expression,(Object[])value,builder);
		}
		
		/**
		 * 获取Jpa的约束标准
		 * 
		 * @param expression root路径
		 * @param values 值
		 * @param builder CriteriaBuilder
		 * 
		 * @return {@link Predicate}
		 */
		public abstract Predicate buildRestriction(Path<?> expression,Object[] values,CriteriaBuilder builder);
	}


了解完以上几个类。那么假设现在有个需求。要写一个模糊约束 (from object o where o.value like '%?%')来判断一些值，可以通过继承PredicateSingleValueSupport类，实现build(Path<?> expression,Object value,CriteriaBuilder builder)，如：

	/**
	 * 模糊约束 ( from object o where o.value like '%?%') RestrictionName:LIKE
	 * <p>
	 * 表达式:LIKE属性类型_属性名称[_OR_属性名称...]
	 * </p>
	 * 
	 * @author vincent
	 *
	 */
	public class LikeRestriction extends PredicateSingleValueSupport{
	
	    public String getRestrictionName() {
	        return RestrictionNames.LIKE;
	    }
	
	    @SuppressWarnings({ "rawtypes", "unchecked" })
	    public Predicate build(Path expression, Object value,CriteriaBuilder builder) {
	
	        return builder.like(expression, "%" + value + "%");
	    }
	
	
	
	}

通过某种方式(如spring的InitializingBean，或serlvet)将该类添加到JpaRestrictionBuilder的PredicateBuilder中。就可以使用约束名了。

	PredicateBuilder nlikeRestriction= new NlikeRestriction();
	JpaRestrictionBuilder.getCriterionMap().put(nlikeRestriction.getRestrictionName(), nlikeRestriction);


对于 hibernate 和 spring data jpa 的约束名在[base-framework](https://github.com/dactiv/base-framework "base-framework")的 dactiv orm 都会有例子。如果不明白。可以看例子理解。

#### 2.6 注解 ####

在 dactiv orm 中，扩展一些比较常用的注解，如：状态删除、防伪安全码等。当需要执行某些特定的需求时，无需在写繁琐的代码。而是直接加上注解即可。

**状态删除**：在大部分的业务系统中有某些表对于 delete 操作都有这么一个需求，就是不物理删除，只把状态改成修改状态。dactiv orm提供了这种功能。只要在实体类加上 **@StateDelete** 注解后调用 orm 框架的删除方法，将会改变实体某个字段的值做已删除的状态值。例如有一个User实体，在删除时要改变 state 值为3，当作已删除记录。可以这样写实体：

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	@StateDelete(propertyName = "state", value = "3")
	public class User implements Serializable {
	    //主键id
	    private Integer id;
	    //状态 
	    private Integer state;
	    //----------GETTER/SETTER-----------//
	}

**hibernate**：

	public class UserDao extends BasicHibernateDao<User, Integer> {
	
	}

调用 deleteByEntity 方法后会生成以下sql语句：

	User user = userDao.load(1);
	userDao.deleteByEntity(user);
	
	sql:update tb_account_user set state = ? where id = ?

**spring data jpa**：

	public interface UserDao extends BasicJpaRepository<User, Integer> {
	
	}

调用 delete 方法后会生成以下sql语句：

	userDao.delete(1);
	sql:update tb_account_user set state = ? where id = ?

如果 orm 框架为 spring data jpa 并且要用到这个功能,记得把.BasicRepositoryFactoryBean类配置到jpa:repositories标签的factory-class属性中。

**安全码**：在某些业务系统中，有某些表涉及到钱的敏感字段时，为了防止数据被串改，提到了安全码功能。就是在表里面加一个字段，该字段的值是表中的主键id和其他比较敏感的字段值并合起来加密存储到安全码字段的值。当下次更新时记录时，会先用id获取数据库的记录，并再次将数据加密与安全吗字段做对比，如果安全码不相同。表示该记录有问题。将丢出异常。

dactiv orm 提供了这种功能。只要在实体类加上 **@SecurityCode** 注解后调用 orm 框架的update方法时，将根据id获取一次记录并加密去对比安全吗。如果相同执行 **update** 操作，否则丢出 **SecurityCodeNotEqualException** 异常。

记录用上面的用户实体

	@Entity
	@Table(name = "TB_ACCOUNT_USER")
	@SecurityCode(value="securityCode",properties={"money"})
	@StateDelete(propertyName = "state", value = "3")
	public class User implements Serializable {
	    //主键id
	    private Integer id;
	    //状态 
	    private Integer state;
		//账户余额
		private Double money;
		//安全吗
		private String securityCode;
	    //----------GETTER/SETTER-----------//
	}

通过以上代码，在更新时，会将 id + money 的值连接起来，并使用md5加密的方式加密一个值，赋值到 securityCode 中：

**hibernate**：

	public class UserDao extends BasicHibernateDao<User, Integer> {
	
	}

***

	User user = userDao.load(1);
	user.setMoney(1000000.00);
	userDao.update(user);
	//or userDao.save(user);


**spring data jpa**:

	public interface UserDao extends BasicJpaRepository<User, Integer> {
	
	}

***
	User user = userDao.findOne(1);
	user.setMoney(1000000.00);
	userDao.save(user);

如果orm 框架为 spring data jpa 并且要用到这个功能,记得把.BasicRepositoryFactoryBean类配置到jpa:repositories标签的factory-class属性中。

**树形实体**：某些时候，在创建自身关联一对多时，往往会出现 n + 1 的问题。就是在遍历树时，获取子节点永远都会去读多一次数据库。特别是用到 orm 框架时，lazy功能会主要你调用到该方法，java代理就去会执行获取属性的操作，同时也产生了数据库的访问。为了避免这些 n + 1 很多人想到了在实体中加多一个属性去记录该节点是否包含子节点，如果包含就去读取，否则将不读取。如以下实体：

	@Entity
	@Table(name = "TB_ACCOUNT_RESOURCE")
	public class Resource implements Serializable{
	
		//主键id
	    private Integer id;
		//名称
		private String name;
		//父类
		private Resource parent;
		//是否包含叶子节点
		private Boolean leaf = Boolean.FALSE;
		//子类
		private List<Resource> children = new ArrayList<Resource>();

	｝

通过该实体，可以通过leaf字段是判断是否包含子节点，当等于true时表示有子节点，可以调用getChildren()方法去读取子节点信息。但现在问题是。要管理这个 leaf 字段似乎每次插入、保存、删除操作都会涉及到该值的修改问题。如：当前数据没有子节点，但添加了一个子节点进去后。当前数据的 leaf 字段要改成true。又或者：当前数据存在子节点，但子节点删除完时，要更新 leaf 字段成 false。又或者，当钱数据存在子节点，但 update 时移除了子节点，要更新 leaf 字段成 false。这些情况下只要调用到 update、 insert、 delete方法都要去更新一次数据。

dactiv orm 提供了这种功能。只要在实体类加上 **@TreeEntity** 注解后调用增、删、改操作会帮你维护该leaf字段。

	@Entity
	@TreeEntity
	@Table(name = "TB_ACCOUNT_RESOURCE")
	public class Resource implements Serializable{
	
		//主键id
	    private Integer id;
		//名称
		private String name;
		//父类
		private Resource parent;
		//是否包含叶子节点
		private Boolean leaf = Boolean.FALSE;
		//子类
		private List<Resource> children = new ArrayList<Resource>();

	｝

TreeEntity注解里面有一个属性为refreshHql，该属性是一个hql语句，在调用增、删、该方法时，会使用该语句去查一次数据，将所有的数据状态为：没子节点但leaf又等于true的数据加载出来，并设置这些数据的 leaf 属性为 false 值。

**TreeEntity注解源码**：

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TreeEntity {
	
		/**
		 * 是否包含叶子的标记属性名
		 * 
		 * @return String
		 */
		public String leafProperty() default "leaf";
		
		/**
		 * tree实体的父类属性名
		 * 
		 * @return String
		 */
		public String parentProperty() default "parent";
		
		/**
		 * 刷新的hql语句
		 * 
		 * @return String
		 */
		public String refreshHql() default "from {0} tree " +
										   "where tree.{1} = {2} and (" + 
										   "	select count(c) from {0} c " + 
										   "	where c.{3}.{4} = tree.{4} " +
										   ") = {5}";
		
		/**
		 * 是否包含叶子的标记属性类型
		 * 
		 * @return Class
		 */
		public Class<?> leafClass() default Boolean.class;
		
		/**
		 * 如果是包含叶子节点需要设置的值
		 * 
		 * @return String
		 */
		public String leafValue() default "1";
		
		/**
		 * 如果不是包含叶子节点需要设置的值
		 * 
		 * @return String
		 */
		public String unleafValue() default "0";
		
	}

如果 orm 框架为 spring data jpa 并且要用到这个功能,记得把.BasicRepositoryFactoryBean类配置到jpa:repositories标签的factory-class属性中。

以上所讲的一切在[base-framework](https://github.com/dactiv/base-framework "base-framework")的dactiv-orm项目中都有例子，如果不懂。可以看例子去理解。