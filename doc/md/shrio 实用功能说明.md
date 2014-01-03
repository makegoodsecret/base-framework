### shrio 实用功能说明 ###

apache shiro 是功能强大并且容易集成的开源权限框架，它能够完成认证、授权、加密、会话管理等功能。认证和授权为权限控制的核心，简单来说，“认证”就是证明“你是谁？” Web 应用程序一般做法是通过表单提交的用户名及密码达到认证目的。“授权”即是"你能做什么?"，很多系统通过资源表的形式来完成用户能做什么。关于 shiro 的一系列特征及优点，很多文章已有列举，这里不再逐一赘述，本文首先会简单的讲述 shiro 和spring该如何集成，重点介绍 shiro 的几个实用功能和一些 shiro 的扩张知识。

#### shiro 集成 spring ####

由于 spring 在 java web 应用里广泛使用，在项目中使用 spring 给项目开发带来的好处有很多，spring 框架本身就是一个非常灵活的东西，而 shrio 的设计模式，让 shiro 集成 spring 并非难事。

首先在web.xml里，通过 spring 的 org.springframework.web.filter.DelegatingFilterProxy 定义一个 filter ,让所有可访问的请求通过一个主要的 shiro 过滤器。该过滤器本身是极为强大的，允许临时的自定义过滤器链基于任何 URL 路径表达式执行。

**web.xml：**

	<!-- shiro security filter -->
	<filter>
	    <filter-name>shiroSecurityFilter</filter-name>
	    <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
	    <init-param>
	        <param-name>targetFilterLifecycle</param-name>
	        <param-value>true</param-value>
	    </init-param>
	</filter>

	<filter-mapping>
	    <filter-name>shiroSecurityFilter</filter-name>
	    <url-pattern>/*</url-pattern>
	    <dispatcher>REQUEST</dispatcher>
		<dispatcher>FORWARD</dispatcher>
	</filter-mapping>

接下来在你的 applicationContext.xml 文件中定义 web 支持的 SecurityManager 和刚刚在 web.xml 定义的 shiroSecurityFilter 即可完成 shiro 和 spring 的集成。

**applicationContext.xml：**

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<!-- shiro的核心安全接口 -->
    	<property name="securityManager" ref="securityManager" />
    	<!-- 要求登录时的链接 -->
	    <property name="loginUrl" value="/login" />
	    <!-- 登陆成功后要跳转的连接 -->
	    <property name="successUrl" value="/index" />
	    <!-- 没有权限要跳转的链接-->
	    <property name="unauthorizedUrl" value="/unauthorized" />
	    <!-- 默认的连接拦截配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = authc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<...>
	</bean>

*提示: org.apache.shiro.spring.web.ShiroFilterFactoryBean 的 id 名称必须和 web.xml 的 filter-name 一致*

ShiroFilterFactoryBean 类似预初始化shiro的一个 java bean，主要作用是配置一些东西让 shiro 知道要做些什么动作，比如，通过以上的配置，要访问http://localhost:port/porject/index，必须当前用户拥有 permission 为 security:index 才能访问，否则将会跳转到指定的loginUrl。当登录时使用 FormAuthenticationFilter 来做登录等等。

ShiroFilterFactoryBean 的 filterChainDefinitions 是对系统要拦截的链接url做配置，比如，我系统中有一条链接为 http://localhost:prot/project/add ,需要当前用户存在角色为admin或者拥有 permission 为 system:add 的才能访问该链接。需要配置如下:

	/add = role[admin], perms[security:index]

如果不配置任何东西在里面的话，shiro会起不到安全框架的作用。所以在这里，如果将整个系统的所有链接配置到 filterChainDefinitions 里面会有很多，这样作的做法会不靠谱。所以，应该通过动态的、可配置的形式来做 filterChainDefinitions，该功能会在**动态filterChainDefinitions**里说明如何通过数据库来创建动态的filterChainDefinitions。

到这里，shiro 和 spring 集成的关键点只有这么点东西。最重要的接口在 **securityManager** 中。securityManager 管理了**认证、授权，session** 等 web 安全的重要类，首先来完成认证、授权方面的功能。

#### shiro 认证、授权 ####

在 shiro 里，**认证**，主要是知道**“你是谁”**，**授权**，是给于你权限去**“做什么”**。所以，在完成认证和授权之前，我们要构造最经典的权限3张表去完成这些事，但在这里画表要图片，整体感也很丑。所以，以 hibernate 实体的方式去说明表的机构：

首先，经典3张表需要用户、组、资源，这3个实体，而3个实体的关系为多对多关系：

	/**
	 * 用户实体
	 * @author maurice
	 *
	 */
	@Entity
	@Table(name="TB_USER")
	public class User extends IdEntity{
	
		private static final long serialVersionUID = 1L;

		//登录名称
		private String username;
		//登录密码
		private String password;
		
		//用户所在的组
		@ManyToMany(fetch=FetchType.LAZY)
		@JoinTable(
			name = "TB_GROUP_USER", 
			joinColumns = { @JoinColumn(name = "FK_USER_ID") }, 
			inverseJoinColumns = { @JoinColumn(name = "FK_GROUP_ID") }
		)
		private List<Group> groupsList = new ArrayList<Group>();
		
		//----------------getting/setting----------------//
	}

***

	**
	 * 组实体
	 * 
	 * @author maurice
	 *
	 */
	@Entity
	@Table(name="TB_GROUP")
	public class Group extends IdEntity{
		
		private static final long serialVersionUID = 1L;
	
		//名称
		private String name;
		
		//用户成员
		@ManyToMany(fetch=FetchType.LAZY)
		@JoinTable(
			name = "TB_GROUP_USER", 
			joinColumns = { @JoinColumn(name = "FK_GROUP_ID") }, 
			inverseJoinColumns = { @JoinColumn(name = "FK_USER_ID") }
		)
		private List<User> membersList = new ArrayList<User>();
		
		//拥有访问的资源
		@ManyToMany(fetch=FetchType.LAZY)
		@JoinTable(
			name = "TB_GROUP_RESOURCE", 
			joinColumns = { @JoinColumn(name = "FK_GROUP_ID") }, 
			inverseJoinColumns = { @JoinColumn(name = "FK_RESOURCE_ID") }
		)
		private List<Resource> resourcesList = new ArrayList<Resource>();
		//shiro role 字符串
		private String role;
		//shiro role连定义的值
		private String value;

		//----------------getting/setting----------------//
	}

***

	**
	 * 资源实体
	 * 
	 * @author maurice
	 *
	 */
	@Entity
	@Table(name="TB_RESOURCE")
	public class Resource extends IdEntity{
		
		private static final long serialVersionUID = 1L;
		
		//名称
		private String name;
		//action url
		private String value;

		//资源所对应的组集合
		@ManyToMany(fetch=FetchType.LAZY)
		@JoinTable(
			name = "TB_GROUP_RESOURCE", 
			joinColumns = { @JoinColumn(name = "FK_RESOURCE_ID") }, 
			inverseJoinColumns = { @JoinColumn(name = "FK_GROUP_ID") }
		)
		private List<Group> groupsList = new ArrayList<Group>();
		//shiro permission 字符串
		private String permission;

		//----------------getting/setting----------------//
	}

通过以上3个 hibernate 实体，构建出了以下表结构，有了这些表，做认证和授权是非常简单的一件事：

<table>
	<tr>
		<th>
			表名
		</th>
		<th>
			表说明
		</th>
	</tr>
	<tr>
		<td>
			TB_USER
		</td>
		<td>
			用户表
		</td>
	</tr>
	<tr>
		<td>
			TB_GROUP
		</td>
		<td>
			组表
		</td>
	</tr>
	<tr>
		<td>
			TB_RESOURCE
		</td>
		<td>
			资源表
		</td>
	</tr>
	<tr>
		<td>
			TB_GROUP_USER
		</td>
		<td>
			用户与组的多对多中间表
		</td>
	</tr>
	<tr>
		<td>
			TB_GROUP_RESOURCE
		</td>
		<td>
			组与资源的多对多中间表
		</td>
	</tr>
</table>

首先要认识的第一个对象是securityManager所管理的 **org.apache.shiro.realm.Realm** 接口,realm 担当 shiro 和你的应用程序的安全数据之间的“桥梁”或“连接器”。但它实际上与安全相关的数据（如用来执行认证及授权）的用户帐户交互时，shiro 从一个或多个为应用程序配置的 realm 中寻找许多这样的东西。

在这个意义上说，realm 本质上是一个特定安全的 dao：它封装了数据源的连接详细信息，使 shiro 所需的相关的数据可用。当配置 shiro 时，你必须指定至少一个 realm 用来进行身份验证和授权。securityManager 可能配置多个 realms，**但至少必须有一个**。

shiro 提供了立即可用的 realms 来连接一些安全数据源（即目录），如LDAP、关系数据库（JDBC）、文本配置源等。如果默认地 realm 不符合你的需求，你可以插入你自己的 realm 实现来代表自定义的数据源。

在[base-framework](https://github.com/dactiv/base-framework "base-framework")里就使用了自己的realm来完成授权和认证工作，realm接口有很多实现类，包括缓存、JdbcRealm、JndiLdapRealm，而JdbcRealm、JndiLdapRealm都是继承AuthorizingRealm类，AuthorizingRealm类有两个抽象方法：

	/**
	 * 
	 * 访问链接时的授权方法
	 * 
	 */
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals);
	
	/**
	 * 用户认证方法
	 * 
	 */
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException;

**doGetAuthenticationInfo**方法的作用是在用户进行**登录**时通过该方法去做认证工作（你是谁），doGetAuthenticationInfo 方法里的 AuthenticationToken 参数是一个认证令牌，装载着表单提交过来的数据，由于 shiro 的认证 filter 默认为 FormAuthenticationFilter,通过 filter 创建的令牌为 UsernamePasswordToken类，该类里面包含了表单提交上来的username、password、remeberme等信息。

**doGetAuthorizationInfo**方法的作用是在用户**认证**完成后（登录完成后），对要访问的链接做**授权**工作。比如刚刚在上面配置的 spring xml 文件里有那么一句话：

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<...>
		<!-- 要求登录时的链接 -->
	    <property name="loginUrl" value="/login" />
	    <!-- 登陆成功后要跳转的连接 -->
	    <property name="successUrl" value="/index" />
		<!-- 没有权限要跳转的链接-->
	    <property name="unauthorizedUrl" value="/unauthorized" />
	    <!-- 默认的连接拦截配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = authc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

当用户登录成功后会跳转到successUrl这个链接，即：http://localhost:port/index。那么这个index又要当前用户存在 permission 为 security:index 才能进入，所以，当登录完成跳转successUrl时，会进入到doGetAuthorizationInfo方法里进行一次**授权**，让 shiro 了解该链接在当前认证的用户里是否可以访问，如果可以访问，那就执行接入到index，否则就会跳转到unauthorizedUrl。

了解以上情况，首先我们创建UserDao和ResourceDao类来做数据访问工作:

	@Repository
	public class UserDao extends BasicHibernateDao<User, String> {
	
		/**通过登录帐号获取用户实体**/
	    public User getUserByUsername(String username) {
	        return findUniqueByProperty("username", username);
	    }
	
	}


***

	@Repository
	public class ResourceDao extends BasicHibernateDao<Resource, String> {
	
		/**通过用户id获取用户所有的资源集合**/
	    public List<Resource> getUserResource(String id) {
			String h = "select rl from User u left join u.groupsList gl left join gl.resourcesList rl where u.id=?1";
	        return distinct(h, id);
	    }
	
	}

然后在创建一个类，名叫JdbcAuthenticationRealm，并继承AuthorizingRealm这个抽象类，实现它的抽象方法：


	public class JdbcAuthenticationRealm extends AuthorizingRealm{
		
		@Autowired
		private UserDao userDao;
		@Autowired
		private ResourceDao resourceDao;
	
		/**
		 * 用户登录的身份验证方法
		 * 
		 */
		protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
			UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
	
	        String username = usernamePasswordToken.getUsername();
	        
	        if (username == null) {
	            throw new AccountException("用户名不能为空");
	        }
	        //通过登录帐号获取用户实体
	        User user = userDao.getUserByUsername(username);
	        
	        if (user == null) {
	            throw new UnknownAccountException("用户不存在");
	        }
	        
	        return new SimpleAuthenticationInfo(user,user.getPassword(),getName());
		}

		/**
		 * 
		 * 当用户进行访问链接时的授权方法
		 * 
		 */
		protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
	        
			if (principals == null) {
				throw new AuthorizationException("Principal对象不能为空");
			}
			
			User user = (User) principals.getPrimaryPrincipal();
			List<Resource> resource = resourceDao.getUserResource(user.getId());
			
			//获取用户相应的permission
			List<String> permissions = CollectionUtils.extractToList(resource, "permission",true);
			
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			
			info.addStringPermissions(permissions);
			
			return info;
		}
		
	
	}

以上代码首先从**doGetAuthenticationInfo**读起，首先。


#### 动态filterChainDefinitions ####

#### 扩张 filter 实现验证码登录 ####

#### 更好性能的 shiro + cache ####

#### 定义 AuthorizationRealm 抽象类,让多 realms 授权得到统一 ####
