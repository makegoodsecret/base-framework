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

*提示:Shiro支持了权限（permissions）概念。权限是功能的原始表述，如：开门、创建一个博文、删除jsmith用户等。通过让权限反映应用的原始功能，在改变应用功能时，你只需要改变权限检查。进而，你可以在运行时按需将权限分配给角色或用户。*

如果不配置任何东西在里面的话，shiro会起不到安全框架的作用。但如果将整个系统的所有链接配置到 filterChainDefinitions 里面会有很多，这样作的做法会不靠谱。所以，应该通过动态的、可配置的形式来做 filterChainDefinitions，该功能会在**动态filterChainDefinitions**里说明如何通过数据库来创建动态的filterChainDefinitions。

到这里，shiro 和 spring 集成的关键点只有这么点东西。最重要的接口在 **securityManager** 中。securityManager 管理了**认证、授权，session** 等 web 安全的重要类，首先来完成认证、授权方面的功能。

#### shiro 认证、授权 ####

在 shiro 里，**认证**，主要是知道**“你是谁”**，**授权**，是给于你权限去**“做什么”**。所以，在完成认证和授权之前，我们要构造最经典的权限3张表去完成这些事，但在这里画表要图片，整体感也很丑。所以，以 hibernate 实体的方式去说明表的结构：

首先，经典3张表需要用户、组、资源这3个实体，而3个实体的关系为多对多关系：

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

初始化数据假设是这样：

<table>
	<tr>
		<td colspan="3" align="center">
			TB_USER
		</td>
	</tr>
	<tr>
		<td>
			id
		</td>
		<td>
			username
		</td>
		<td>
			password
		</td>
	</tr>
	<tr>
		<td>
			17909124407b8d7901407be4996c0001
		</td>
		<td>
			admin
		</td>
		<td>
			admin
		</td>
	</tr>
</table>

***

<table>
	<tr>
		<td colspan="4" align="center">
			TB_GROUP
		</td>
	</tr>
	<tr>
		<td>
			id
		</td>
		<td>
			name
		</td>
		<td>
			role
		</td>
		<td>
			value
		</td>
	</tr>
	<tr>
		<td>
			17909124407b8d7901407be4996c0002
		</td>
		<td>
			超级管理员
		</td>
		<td>
			
		</td>
		<td>
			
		</td>
	</tr>
</table>

***

<table>
	<tr>
		<td colspan="4" align="center">
			TB_RESOURCE
		</td>
	</tr>
	<tr>
		<td>
			id
		</td>
		<td>
			name
		</td>
		<td>
			permission
		</td>
		<td>
			value
		</td>
	</tr>
	<tr>
		<td>
			17909124407b8d7901407be4996c0003
		</td>
		<td>
			进入首页
		</td>
		<td>
			perms[security:index]
		</td>
		<td>
			/index
		</td>
	</tr>
</table>

***

<table>
	<tr>
		<td colspan="2" align="center">
			TB_GROUP_USER
		</td>
	</tr>
	<tr>
		<td>
			FK_USER_ID
		</td>
		<td>
			FK_GROUP_ID
		</td>
	</tr>
	<tr>
		<td>
			17909124407b8d7901407be4996c0001
		</td>
		<td>
			17909124407b8d7901407be4996c0002
		</td>
	</tr>
</table>

***

<table>
	<tr>
		<td colspan="2" align="center">
			TB_GROUP_RESOURCE
		</td>
	</tr>
	<tr>
		<td>
			FK_GROUP_ID
		</td>
		<td>
			FK_RESOURCE_ID
		</td>
	</tr>
	<tr>
		<td>
			17909124407b8d7901407be4996c0002
		</td>
		<td>
			17909124407b8d7901407be4996c0003
		</td>
	</tr>
</table>

首先要认识的第一个对象是securityManager所管理的 **org.apache.shiro.realm.Realm** 接口,realm 担当 shiro 和你的应用程序的安全数据之间的“桥梁”或“连接器”。但它实际上与安全相关的数据（如用来执行认证及授权）的用户帐户交互时，shiro 从一个或多个为应用程序配置的 realm 中寻找许多这样的东西。

在这个意义上说，realm 本质上是一个特定安全的 dao：它封装了数据源的连接详细信息，使 shiro 所需的相关数据可用。当配置 shiro 时，你必须指定至少一个 realm 用来进行身份验证和授权。securityManager 可能配置多个 realms，**但至少必须有一个**。

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
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) 
	throws AuthenticationException;

**doGetAuthenticationInfo**方法的作用是在用户进行**登录**时通过该方法去做认证工作（你是谁），doGetAuthenticationInfo 方法里的 AuthenticationToken 参数是一个认证令牌，装载着表单提交过来的数据，由于 shiro 的认证 filter 默认为 FormAuthenticationFilter,通过 filter 创建的令牌为 UsernamePasswordToken类，该类里面包含了表单提交上来的username、password、remeberme等信息。

**doGetAuthorizationInfo**方法的作用是在用户**认证**完成后（登录完成后），对要访问的链接做**授权**工作。比如刚刚在上面配置的 spring xml 文件里有那么一句话：

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<...>
	    <!-- 登陆成功后要跳转的连接 -->
	    <property name="successUrl" value="/index" />
		<!-- 没有权限要跳转的链接 -->
	    <property name="unauthorizedUrl" value="/unauthorized" />
	    <!-- 默认的连接拦截配置 -->
		<property name="filterChainDefinitions">
			<value>
				...
				/index = perms[security:index]
			</value>
		</property>
	</bean>

当用户登录成功后会跳转到 **successUrl** 这个链接，即：**http://localhost:port/index**。那么这个index又要当前用户存在 **permission** 为 **security:index** 才能进入，所以，当登录完成跳转 **successUrl** 时，会进入到 **doGetAuthorizationInfo** 方法里进行一次**授权**，让 shiro 了解该链接在当前认证的用户里是否可以访问，如果可以访问，那就执行接入到index，否则就会跳转到unauthorizedUrl。

*提示： shiro 支持了权限（permissions）概念。权限是功能的原始表述，如‘开门’，‘创建一个博文’，‘删除‘jsmith’用户’等。通过让权限反映应用的原始功能，在改变应用功能时，你只需要改变权限检查。进而，你可以在运行时按需将权限分配给角色或用户。*

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
			
			User user = principals.oneByType(User.class);
			List<Resource> resource = resourceDao.getUserResource(user.getId());
			
			//获取用户相应的permission
			List<String> permissions = CollectionUtils.extractToList(resource, "permission",true);
			
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			
			info.addStringPermissions(permissions);
			
			return info;
		}
		
	
	}

以上代码首先从**doGetAuthenticationInfo**读起，首先。假设我们有一个表单

	<form action="${base}/login" method="post">
		<input type="text" name="username" />
		<input type="password" name="password" />
		<input type="checkbox" name="remeberMe" />
		<input type="submit" value="提交"/>
	</form>

*提示: input标签的所有name属性不一定要写死 username,password,remeberMe。可以在 FormAuthenticationFilter 修改*

当点击提交时，shiro 会拦截这次的表单提交，因为在配置文件里已经说明，/login 由 authc 做处理，就是:

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<...>
	    <!-- 默认的连接拦截配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = authc
				...
			</value>
		</property>
	</bean>

而 authc 就是 shiro 的 **FormAuthenticationFilter** 。shiro 首先会判断 /login 这次请求是否为**post请求**，如果是，那么就交给 FormAuthenticationFilter 处理，否则将不做任何处理。

当 FormAuthenticationFilter 接收到要处理时。那么 FormAuthenticationFilter 首先会根据表单提交过来的请求参数创建一个 **UsernamePasswordToken**，然后获取一个 **Subject** 对象，由Subject去执行登录。

*提示： Subject 实质上是一个当前执行用户的特定的安全“视图”。鉴于“User”一词通常意味着一个人，而一个 Subject 可以是一个人，但它还可以代表第三方服务，daemon account，cron job，或其他类似的任何东西——基本上是当前正与软件进行交互的任何东西。* 
 
*所有 Subject 实例都被绑定到（且这是必须的）一个 SecurityManager 上。当你与一个 Subject 交互时，那些交互作用转化为与 SecurityManager 交互的特定 subject 的交互作用。*

Subject执行登录时，会将UsernamePasswordToken传入到Subject.login方法中。在经过一些小小的处理过程后（如：是否启用了认证缓存，如果是，获取认证缓存，执行登录，不在查询数据库），会进入到 **doGetAuthenticationInfo**方法里，而在doGetAuthenticationInfo方法做的事情就是：

1. 通过用户名获取当前用户
2. 通过当前用户和用户密码创建一个 **SimpleAuthenticationInfo** 然后去匹配密码是否正确

在SimpleAuthenticationInfo对象里的密码为数据库里面的用户密码，返回SimpleAuthenticationInfo后 shiro 会根据表单提交的密码和 SimpleAuthenticationInfo 的密码去做对比，如果完全正确，就表示认证成功，当成功后，会重定向到successUrl这个链接。

当重定向到 index 时，会进入到 perms过滤器，就是 shiro 的**PermissionsAuthorizationFilter**，因为配置文件里已经说明,就是:

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<...>
	    <!-- 默认的连接拦截配置 -->
		<property name="filterChainDefinitions">
			<value>
				...
				/index = perms[security:index]
			</value>
		</property>
	</bean>

PermissionsAuthorizationFilter的工作主要是判断当前subject是否有足够的权限去访问index,判断条件有：

1. 判断subject是否已认证，如果没认证，返回登录页面,就是在配置文件里指定的**loginUrl**。
 
2. 如果认证了，判断当前用户是否存未授权，如果没有就去授权，当授权时，就会进入到 **doGetAuthorizationInfo** 方法

3. 如果已经认证了。就判断是否存在xx链接的permission,如果有，就进入，否则重定向到未授权页面，就是在配置文件里指定的**unauthorizedUrl**

那么，认证我们上面已经认证过了。就会进入到第二个判断，第二个判断会跑到了doGetAuthorizationInfo方法，而doGetAuthorizationInfo方法里做了几件事：

1. 获取当前的用户
2. 通过用户id获取用户的资源集合
3. 将资源实体集合里的permission获取出来形成一个List
4. 将用户拥有的permission放入到**SimpleAuthorizationInfo**对象中

doGetAuthorizationInfo 返回 SimpleAuthorizationInfo 对象的作用是让 shiro 的 **AuthorizingRealm** 逐个循环里面的 permission 和当前访问链接的 permission 去做匹配，如果匹配到了，就表示当前用户可以访问本次请求的链接，否则就重定向到未授权页面。

实现认证和授权功能继承**AuthorizingRealm**已经可以达到效果，但是要注意几点就是：

1. 表单提交的action要和filterChainDefinitions的一致。
2. filterChainDefinitions的**“/login = authc”**这句话的左值要和**loginUrl**属性一致。
3. 表单提交必须要**post方法**。

完成认证和授权后现在的缺陷在于filterChainDefinitions都是要手动去一个个配置，一个系统那么多链接都要写上去非常不靠谱，下面将介绍如何使用资源表动态去构建filterChainDefinitions。

#### 动态filterChainDefinitions ####

动态 filterChainDefinitions 是为了能够通过数据库的数据，将 filterChainDefinitions 构造出来，而不在是一个个手动的写入到配置文件中，在shiro的 ShiroFilterFactoryBean 启动时，会通过 filterChainDefinitions 的配置信息构造成一个Map，在赋值到 **filterChainDefinitionMap** 中，shiro的源码如下:

	/**
     * A convenience method that sets the {@link #setFilterChainDefinitionMap(java.util.Map) filterChainDefinitionMap}
     * property by accepting a {@link java.util.Properties Properties}-compatible string (multi-line key/value pairs).
     * Each key/value pair must conform to the format defined by the
     * {@link FilterChainManager#createChain(String,String)} JavaDoc - each property key is an ant URL
     * path expression and the value is the comma-delimited chain definition.
     *
     * @param definitions a {@link java.util.Properties Properties}-compatible string (multi-line key/value pairs)
     *                    where each key/value pair represents a single urlPathExpression-commaDelimitedChainDefinition.
     */
    public void setFilterChainDefinitions(String definitions) {
        Ini ini = new Ini();
        ini.load(definitions);
        //did they explicitly state a 'urls' section?  Not necessary, but just in case:
        Ini.Section section = ini.getSection(IniFilterChainResolverFactory.URLS);
        if (CollectionUtils.isEmpty(section)) {
            //no urls section.  Since this _is_ a urls chain definition property, just assume the
            //default section contains only the definitions:
            section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
        }
        setFilterChainDefinitionMap(section);
    }

*提示:Ini.Section 该类是一个 Map 子类。*

ShiroFilterFactoryBean 也提供了设置 filterChainDefinitionMap 的方法，配置 filterChainDefinitions 和 filterChainDefinitionMap 两者只需一个即可。

在实现动态 filterChainDefinitions 时，需要借助 spring 的 **FactoryBean** 接口去做这件事。spring 的 FactoryBean 接口是专门暴露bean对象的接口，通过接口的 **getObject()** 方法获取bean实例，也可以通过 **getObjectType()** 方法去指定bean的类型，让注解Autowired能够注入或在 spring 上下文中 getBean()方法直接通过class去获取该bean。

那么，继续用上面的经典三张表的资源数据访问去动态构造 filterChainDefinitions。 首先创建一个 ChainDefinitionSectionMetaSource 类并实现 FactoryBean 的方法,实现FactoryBean接口的所有方法，并且在resourceDao中添加一个获取所有资源的方法，如下:

	@Repository
	public class ResourceDao extends BasicHibernateDao<Resource, String> {
	
		/**通过用户id获取用户所有的资源集合**/
	    public List<Resource> getUserResource(String id) {
			String h = "select rl from User u left join u.groupsList gl left join gl.resourcesList rl where u.id=?1";
	        return distinct(h, id);
	    }
		
		/**获取有时有资源**/		
		public List<Resource> getAllResource() {
			return getAll();
		}
	
	}

***

	/**
	 * 借助spring {@link FactoryBean} 对apache shiro的premission进行动态创建
	 * 
	 * @author maurice
	 *
	 */
	public class ChainDefinitionSectionMetaSource implements FactoryBean<Ini.Section>{

		@Autowired
		private ResourceDao resourceDao;
		
		//shiro默认的链接定义
		private String filterChainDefinitions;
		
		/**
		 * 通过filterChainDefinitions对默认的链接过滤定义
		 * 
		 * @param filterChainDefinitions 默认的接过滤定义
		 */
		public void setFilterChainDefinitions(String filterChainDefinitions) {
			this.filterChainDefinitions = filterChainDefinitions;
		}
		
		@Override
		public Section getObject() throws BeansException {
			Ini ini = new Ini();
	        //加载默认的url
	        ini.load(filterChainDefinitions);
	        
	        Ini.Section section = ini.getSection(IniFilterChainResolverFactory.URLS);
	        if (CollectionUtils.isEmpty(section)) {
	            section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
	        }
	        
	        //循环数据库资源的url
	        for (Resource resource : resourceDao.getAll()) {
	        	if(StringUtils.isNotEmpty(resource.getValue()) && StringUtils.isNotEmpty(resource.getPermission())) {
	        		section.put(resource.getValue(), resource.getPermission());
	        	}
	        }
	        
	        return section;
		}
		
		@Override
		public Class<?> getObjectType() {
			return Section.class;
		}
		
		@Override
		public boolean isSingleton() {
			return true;
		}
	
	}

ChainDefinitionSectionMetaSource 类，重点在 **getObject()** 中，返回了一个 shiro 的 **Ini.Section** 首先**Ini**类加载了filterChainDefinitions的配置信息（由于有些链接不一定要放到数据库里，也可以通过直接写在配置文件中）。通过ini.load(filterChainDefinitions);一话构造成了/login key = authc value等信息。那么shiro就知道了login这个url需要使用authc这个filter去拦截。完成之后，通过resourceDao的getAll()方法将所有数据库的信息再次叠加到Ini.Section中（在tb_resource表中的数据为:/index = perms[security:index]），形成了最后的配置。

完成该以上工作后，修改 spring 的 applicationContext.xml 当项目启动时，你会发现在容器加载spring内容时，会进入到ChainDefinitionSectionMetaSource，如果使用maven的朋友，进入到shiro的源码放一个断点，你会看到tb_resource表的/index = perms[security:index]已经构造到了filterChainDefinitionMap里。

**applicationContext.xml修改为：**

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = authc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

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
	    <!-- shiro连接约束配置,在这里使用自定义的动态获取资源类 -->
	    <property name="filterChainDefinitionMap" ref="chainDefinitionSectionMetaSource" />
	</bean>

通过修改和添加以上三个文件，完成了动态 filterChainDefinitions 具体的过程在[base-framework](https://github.com/dactiv/base-framework "base-framework")的showcase的base-curd项目下有例子，如果看不到。可以根据例子去理解。

#### 扩展 filter 实现验证码登录 ####

#### 定义 AuthorizationRealm 抽象类,让多 realms 授权得到统一 ####

#### 更好性能的 shiro + cache ####