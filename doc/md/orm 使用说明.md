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

**UserDao：**

	public class UserDao extends BasicHibernateDao<User, String> {
	
	}

##### 2.2 使用 spring data jpa #####

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

##### 2.3 PropertyFilter 查询表达式说明#####

在 dactiv orm 里，对 hibernate 和 spring data jpa 都扩展了一套**查询表达式**，是专门用来应付一些比较简单的查询而不用写语句的功能。通过该表达式，dactiv orm 能够解析出最终的查询语句去让 hibernate 或 spring data jpa 去执行，需要使用该表达式，如果是用 hibernate 需要集成 **HibernateSupportDao** 类，如果使用 spring data jpa 的话需要使用到 **Specifications.get()** 方法去构造 spring data jpa 的 **Specification** 后才能执行查询，或者根据 **2.2 使用 spring data jpa** 配置完成后，集成BasicJpaRepository 接口，里面就提供了支持 PropertyFilter 的查询方法。

该表达式的规则为：**<约束类型><属性类型>_<属性名称>**，例如现在有个用户实体：

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
	    //<约束类型><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a")
	);
	userDao.findByPropertyFilter(filters);

**spring data jpa**：

	public interface UserRepository extends JpaRepository<User, String>,JpaSpecificationExecutor<User>{
	
	}
	
***
	userRepository.findAll(Specifications.get(Lists.newArrayList(
		//<约束类型><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a")
	)));

查询username等于a的并且realname等于c的用户可以通过多个条件进行and关系查询：

**hibernate**：

	public class UserDao extends HibernateSupportDao<User, String>{
	
	}

***

	List<PropertyFilter> filters = Lists.newArrayList(
	    //<约束类型><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a"),
		PropertyFilters.get("EQS_realname", "c")
	);
	userDao.findByPropertyFilter(filters);

**spring data jpa**：

	public interface UserRepository extends JpaRepository<User, String>,JpaSpecificationExecutor<User>{
	
	}
	
***
	userRepository.findAll(Specifications.get(Lists.newArrayList(
		//<约束类型><属性类型>_<属性名称>
	    PropertyFilters.get("EQS_username", "a"),
		PropertyFilters.get("EQS_realname", "c")
	)));

了解一些功能后，解释一下 **<约束类型><属性类型>_<属性名称>** 应该怎么写。

**约束类型**：约束类型是表达式第一个参数的必须条件,在这里的约束类型是指通过什么条件去做查询，如等于、不等于、包含(in)、大于、小于...等等。

约束类型描述列表:
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

##### 2.4 页面多条件查询 #####

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


##### 2.5 扩展表达式的约束名称 #####

如果你在项目开发时觉得表达式里面的约束名称不够用，可以对表达式做扩展处理。扩展约束名称时 spring data jpa 和 hibernate 所关注的类不同：

**hiberante**：

1. HibernateRestrictionBuilder
2. CriterionBuilder
3. CriterionSingleValueSupport
4. CriterionMultipleValueSupport

**spring data jpa**：

1. JpaRestrictionBuilder
2. PredicateBuilder
3. PredicateSingleValueSupport
4. PredicateMultipleValueSupport