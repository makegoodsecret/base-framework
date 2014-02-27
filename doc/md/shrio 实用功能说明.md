### 1 shrio 实用功能说明 ###

apache shiro 是功能强大并且容易集成的开源权限框架，它能够完成认证、授权、加密、会话管理等功能。认证和授权为权限控制的核心，简单来说，“认证”就是证明“你是谁？” Web 应用程序一般做法是通过表单提交的用户名及密码达到认证目的。“授权”即是"你能做什么?"，很多系统通过资源表的形式来完成用户能做什么。关于 shiro 的一系列特征及优点，很多文章已有列举，这里不再逐一赘述，本文首先会简单的讲述 shiro 和spring该如何集成，重点介绍 shiro 的几个实用功能和一些 shiro 的扩展知识。

#### 1.1 shiro 集成 spring ####

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

##### 1.1.1 启用Shiro注解 #####

在独立应用程序和 web 应用程序中，你可能想为安全检查使用 shiro 的注释（例如，@RequiresRoles，@RequiresPermissions 等等）。这需要 shiro 的 spring AOP 集成来扫描合适的注解类以及执行必要的安全逻辑。以下是如何使用这些注解的。只需添加这两个 bean：

	<aop:aspectj-autoproxy proxy-target-class="true" />
	
	<bean class="org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor">
	    <property name="securityManager" ref="securityManager"/>
	</bean>

对于这两个bean，在[base-framework](https://github.com/dactiv/base-framework "base-framework")里添加在applicationContext-mvc.xml中，这样做是为了启用 shiro 注解时仅在 spring mvc 的 controller 层就可以了，不必在 service 和 dao 中也使用该注解。

到这里，shiro 和 spring 集成的关键点只有这么点东西。最重要的接口在 **securityManager** 中。securityManager 管理了**认证、授权，session** 等 web 安全的重要类，首先来完成认证、授权方面的功能。

#### 1.2 shiro 认证、授权 ####

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
			添加用户
		</td>
		<td>
			perms[user:add]
		</td>
		<td>
			/user/add/**
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

首先要认识的第一个对象是securityManager所管理的 **org.apache.shiro.realm.Realm** 接口，realm 担当 shiro 和你的应用程序的安全数据之间的“桥梁”或“连接器”。但它实际上与安全相关的数据（如用来执行认证及授权）的用户帐户交互时，shiro 从一个或多个为应用程序配置的 realm 中寻找许多这样的东西。

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
			
			addPermissions(info, permissions);
			
			return info;
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

完成后修改applicationContext.xml文件：

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
	</bean>
	
	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<!-- shiro的核心安全接口 -->
    	<property name="securityManager" ref="securityManager" />
    	<!-- 要求登录时的链接 -->
	    <property name="loginUrl" value="/login" />
	    <!-- 登陆成功后要跳转的连接 -->
	    <property name="successUrl" value="/index" />
	    <!-- 没有权限要跳转的链接 -->
	    <property name="unauthorizedUrl" value="/unauthorized" />
	    <!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
				/changePassword = perms[security:change-password]
			</value>
		</property>
	</bean>

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

#### 1.3 动态filterChainDefinitions ####

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

那么，继续用上面的经典三张表的资源数据访问去动态构造 filterChainDefinitions。 首先创建一个 ChainDefinitionSectionMetaSource 类并实现 FactoryBean 的方法和在resourceDao中添加一个获取所有资源的方法，如下:

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

ChainDefinitionSectionMetaSource 类，重点在 **getObject()** 中，返回了一个 shiro 的 **Ini.Section** 首先**Ini**类加载了filterChainDefinitions的配置信息（由于有些链接不一定要放到数据库里，也可以通过直接写在配置文件中）。通过ini.load(filterChainDefinitions);一话构造成了/login key = authc value等信息。那么shiro就知道了login这个url需要使用authc这个filter去拦截。完成之后，通过resourceDao的getAll()方法将所有数据库的信息再次叠加到Ini.Section中（在tb_resource表中的数据为:/user/add/** = perms[user:add]），形成了最后的配置。

完成该以上工作后，修改 spring 的 applicationContext.xml，当项目启动时，你会发现在容器加载spring内容时，会进入到ChainDefinitionSectionMetaSource，如果使用maven的朋友，进入到shiro的源码放一个断点，你会看到tb_resource表的/user/add/** = perms[user:add]已经构造到了filterChainDefinitionMap里。

**applicationContext.xml修改为：**

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
	</bean>

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

通过修改和添加以上三个文件，完成了动态 filterChainDefinitions 具体的过程在[base-framework](https://github.com/dactiv/base-framework "base-framework")的showcase的base-curd项目下有例子，如果看不懂。可以根据例子去理解。

#### 1.4 扩展 shiro 的 filter 实现验证码登录 ####

验证码登录在web开发中最常见，shiro对于验证码登录的功能没有支持，但shiro的设计模式让开发人员自定义一个小小的验证码登录不会很难。[base-framework](https://github.com/dactiv/base-framework "base-framework")的showcase的base-curd项目所扩展的验证码登录需求是：**当用户登录失败次数达到指标时，才出现验证码。**

通过该需求，我们回到上面提到的 FormAuthenticationFilter，该filter是专门做认证用的filter，所以本人第一时间想到扩展它，如果有更好的实现方式希望能够分享。

实现验证码登录，我们首先创建一个CaptchaAuthenticationFilter类，并继承FormAuthenticationFilter。FormAuthenticationFilter最需要重写的方法有：

	/**执行登录**/
	protected boolean executeLogin(ServletRequest request,ServletResponse response) throws Exception

	/**当登录失败时所响应的方法**/
	protected boolean onLoginFailure(AuthenticationToken token,
									 AuthenticationException e, 
									 ServletRequest request,
									 ServletResponse response);

	/**当登录成功时所响应的方法**/
	protected boolean onLoginSuccess(AuthenticationToken token,
									 Subject subject, 
									 ServletRequest request, 
									 ServletResponse response) throws Exception;

所以CaptchaAuthenticationFilter类应该这样实现：

	/**
	 * 验证码登录认证Filter
	 * 
	 * @author maurice
	 *
	 */
	@Component
	public class CaptchaAuthenticationFilter extends FormAuthenticationFilter{
		
		/**
		 * 默认验证码参数名称
		 */
		public static final String DEFAULT_CAPTCHA_PARAM = "captcha";
		
		/**
		 * 默认在session中存储的登录错误次数的名称
		 */
		private static final String DEFAULT_LOGIN_INCORRECT_NUMBER_KEY_ATTRIBUTE = "incorrectNumber";
		
		//验证码参数名称
	    private String captchaParam = DEFAULT_CAPTCHA_PARAM;
	    //在session中的存储验证码的key名称
	    private String sessionCaptchaKeyAttribute = DEFAULT_CAPTCHA_PARAM;
	    //在session中存储的登录错误次数名称
	    private String loginIncorrectNumberKeyAttribute = DEFAULT_LOGIN_INCORRECT_NUMBER_KEY_ATTRIBUTE;
	    //允许登录错误次数，当登录次数大于该数值时，会在页面中显示验证码
	    private Integer allowIncorrectNumber = 1;
	    
	    /**
	     * 重写父类方法，在shiro执行登录时先对比验证码，正确后在登录，否则直接登录失败
	     */
		@Override
		protected boolean executeLogin(ServletRequest request,ServletResponse response) throws Exception {
			
			Session session = SystemVariableUtils.createSessionIfNull();
			//获取登录错误次数
			Integer number = (Integer) session.getAttribute(getLoginIncorrectNumberKeyAttribute());
			
			//首次登录，将该数量记录在session中
			if (number == null) {
				number = new Integer(1);
				session.setAttribute(getLoginIncorrectNumberKeyAttribute(), number);
			}
			
			//如果登录次数大于allowIncorrectNumber，需要判断验证码是否一致
			if (number > getAllowIncorrectNumber()) {
				//获取当前验证码
				String currentCaptcha = (String) session.getAttribute(getSessionCaptchaKeyAttribute());
				//获取用户输入的验证码
				String submitCaptcha = getCaptcha(request);
				//如果验证码不匹配，登录失败
				if (StringUtils.isEmpty(submitCaptcha) || !StringUtils.equals(currentCaptcha,submitCaptcha.toLowerCase())) {
					return onLoginFailure(this.createToken(request, response), 
										  new AccountException("验证码不正确"), 
										  request, 
										  response);
				}
			
			}
			
			return super.executeLogin(request, response);
		}
	
	
		/**
		 * 重写父类方法，当登录失败将异常信息设置到request的attribute中
		 */
		@Override
		protected void setFailureAttribute(ServletRequest request,AuthenticationException ae) {
			if (ae instanceof IncorrectCredentialsException) {
				request.setAttribute(getFailureKeyAttribute(), "用户名密码不正确");
			} else {
				request.setAttribute(getFailureKeyAttribute(), ae.getMessage());
			}
		}
		
		/**
		 * 重写父类方法，当登录失败次数大于allowIncorrectNumber（允许登录错误次数）时，将显示验证码
		 */
		@Override
		protected boolean onLoginFailure(AuthenticationToken token,
										 AuthenticationException e, 
										 ServletRequest request,
										 ServletResponse response) {
			
			Session session = SystemVariableUtils.getSession();
			Integer number = (Integer) session.getAttribute(getLoginIncorrectNumberKeyAttribute());
			session.setAttribute(getLoginIncorrectNumberKeyAttribute(),++number);
			
			return super.onLoginFailure(token, e, request, response);
		}
		
		/**
		 * 重写父类方法，当登录成功后，将allowIncorrectNumber（允许登错误录次）设置为0，重置下一次登录的状态
		 */
		@Override
		protected boolean onLoginSuccess(AuthenticationToken token, 
										 Subject subject, 
										 ServletRequest request, 
										 ServletResponse response) throws Exception {
			
			Session session = SystemVariableUtils.getSession();
			session.removeAttribute(getLoginIncorrectNumberKeyAttribute());
			session.setAttribute("sv", subject.getPrincipal());
			
			return super.onLoginSuccess(token, subject, request, response);
		}
	
		//---------------------------------getter/setter方法----------------------------------//
	｝

CaptchaAuthenticationFilter类重点的代码在executeLogin方法和onLoginFailure方法中。当执行登录时，会在session中创建一个**"登录错误次数"**属性，当该属性大于指定的值时才去匹配验证码，否则继续调用FormAuthenticationFilter的executeLogin方法执行登录。

当登录失败时（onLoginFailure方法）会获取"登录错误次数"，并且加1。直到登录成功后，将"登录错误次数"属性从session中移除。

*提示setFailureAttribute方法的作用是当出现用户名密码错误时提示中文出去，这样会友好些。*

所以，在登录界面的html中用freemarker的话就这样写:

	<form action="${base}/login" method="post">
		<input type="text" name="username" />
		<input type="password" name="password" />
		<input type="checkbox" name="remeberMe" />
		<!--当登录错误次数大于1时，出现验证码 -->		
		<#if Session.incorrectNumber?? && Session.incorrectNumber gte 1>
		  <input type="text" name="captcha" id="captcha" >
		  <img id="captchaImg" src="get-captcha" />
		</#if>
		<input type="submit" value="提交"/>
	</form>

完成后在修改applicationContext.xml即可:

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
	</bean>

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="filters">
			<map>
				<entry key="captchaAuthc" value-ref="captchaAuthenticationFilter" />
			</map>
		</property>
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

*提示: applicationContext.xml修改了ShiroFilterFactoryBean的filters属性，在filters属性里添加了一个自定义的 captchaAuthenticationFilter , 名字叫 captchaAuthc 在 filterChainDefinitions 里将 /login = authc 该为 /login = captchaAuthc。*

通过添加CaptchaAuthenticationFilter类和修改applicationContext.xml文件，完成了验证码，具体的过程在[base-framework](https://github.com/dactiv/base-framework "base-framework")的showcase的base-curd项目下有例子，如果看不懂。可以根据例子去理解。

#### 1.5 定义 AuthorizationRealm 抽象类,让多 realms 的授权得到统一 ####

在**1.2 shiro 认证、授权**中提到，realm 担当 shiro 和你的应用程序的安全数据之间的“桥梁”或“连接器”。必须要存在一个。当一个应用程序配置了两个或两个以上的 realm 时，**ModularRealmAuthenticator** 依靠内部的 **AuthenticationStrategy** 组件来确定这些认证尝试的成功或失败条件。如：如果只有一个 realm 验证成功，但所有其他的都失败，这被认为是成功还是失败？又或者必须所有的 realm 验证成功才被认为样子成功？又或者如果一个 realm 验证成功，是否有必要进一步调用其他 realm ? 等等。

**AuthenticationStrategy**： 是一个无状态的组件，它在身份验证尝试中被询问4 次（这4 次交互所需的任何必要的状态将被作为方法参数）：

1. 在任何Realm 被调用之前被询问。
2. 在一个单独的Realm 的getAuthenticationInfo 方法被调用之前立即被询问。
3. 在一个单独的Realm 的getAuthenticationInfo 方法被调用之后立即被询问。
4. 在所有的Realm 被调用后询问。

另外，AuthenticationStrategy 负责从每一个成功的 realm 汇总结果并将它们“捆绑”到一个单一的 AuthenticationInfo 再现。这最后汇总的 AuthenticationInfo 实例就是从 Authenticator 实例返回的值以及 shiro 所用来代表 Subject 的最终身份ID 的值（即Principals(身份)）。

shiro 有 3 个具体的AuthenticationStrategy 实现：

<table>
	<tr>
		<td>
			AuthenticationStrategy 类
		</td>
		<td>
			描述
		</td>
	<tr>
	<tr>
		<td>
			AtLeastOneSuccessfulStrategy
		</td>
		<td>
			如果一个（或更多）Realm 验证成功，则整体的尝试被认为是成功的。如果没有一个验证成功,则整体尝试失败。
		</td>
	<tr>
	<tr>
		<td>
			FirstSuccessfulStrategy
		</td>
		<td>
			只有第一个成功验证的Realm 返回的信息将被使用。所有进一步的Realm 将被忽略。如果没有一个验证成功，则整体尝试失败。
		</td>
	<tr>
	<tr>
		<td>
			AllSucessfulStrategy
		</td>
		<td>
			为了整体的尝试成功，所有配置的Realm 必须验证成功。如果没有一个验证成功，则整体尝试失败。
		</td>
	<tr>
</table>

ModularRealmAuthenticator 默认的是AtLeastOneSuccessfulStrategy 实现，因为这是最常所需的方案。如果你不喜欢，你可以配置一个不同的方案：

	<!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<property name="authenticator">
			<bean class="org.apache.shiro.authc.pam.ModularRealmAuthenticator">
				<!-- 认证策略使用FirstSuccessfulStrategy策略 -->
				<property name="authenticationStrategy">
					<bean class="org.apache.shiro.authc.pam.FirstSuccessfulStrategy" />
				</property>
				<!-- 多realms配置 -->
				<property name="realms">
					<list>
						<value>
							<ref bean="jdbcAuthenticationRealm" />
							<ref bean="otherAuthenticationRealm" />
						</value>
					</list>
				</property>
			</bean>
		</property>
	</bean>

那么现在存在这样的需求，让shiro 的多 realms 就有了发挥余地：

1. 在用户登录时，先从本系统数据库获取用户信息。如果获取得到用户，就执行进行认证。
2. 如果获取不到用户，去第三方应用接口或者其他数据库获取用户。
3. 如果是从第三方应用接口或者其他数据库获取的用户，将用户插入到本系统的用户表中，并赋给它一些本系统的权限。

但问题是：授权和认证的接口AuthorizingRealm需要实现两个方法，但这个需求提到的是认证而已，授权却要统一进行授权。所以，在多realms都继承AuthorizingRealm会出现很多复制粘贴的授权代码。所以，写一个公用的授权抽象类会比较好些。当然，这个看需求而定。

那么定义 AuthorizationRealm 抽象类让多realms继承它，完成各各realms自己的授权：

	/**
	 * apache shiro 的公用授权类
	 * 
	 * @author maurice
	 *
	 */
	public abstract class AuthorizationRealm extends AuthorizingRealm{
	
		@Autowired
		private ResourceDao resourceDao;
		
		private List<String> defaultPermission = Lists.newArrayList();
		
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
		 * 设置默认permission
		 * 
		 * @param defaultPermission permission
		 */
		public void setDefaultPermission(List<String> defaultPermission) {
			this.defaultPermission = defaultPermission;
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
			
			User user = (User)principals.fromRealm(getName()).iterator().next();
			List<Resource> resource = resourceDao.getUserResource(user.getId());
			
			//获取用户相应的permission
			List<String> permissions = CollectionUtils.extractToList(resource, "permission",true);
			
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			
			//添加用户拥有的permission
	        addPermissions(info,authorizationInfo);
	        
	        return info;
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

本系统的认证:

	/**
	 * 
	 * apache shiro 的身份验证类
	 * 
	 * @author maurice
	 *
	 */
	public class JdbcAuthenticationRealm extends AuthorizationRealm{
		
		@Autowired
		private UserDao userDao;
	
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
		
	
	}

第三方应用和其他数据库的认证:
	
	
	public class otherAuthenticationRealm extends AuthorizationRealm{
	
		/**
		 * 用户登录的身份验证方法
		 * 
		 */
		protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
			//获取用户
			//插入到本系统
			//返回SimpleAuthenticationInfo.
		}
		
	
	}
完成后在修改applicationContext.xml即可:

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
	</bean>

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="otherAuthenticationRealm" class="domain.OtherAuthenticationRealm" />
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<property name="authenticator">
			<bean class="org.apache.shiro.authc.pam.ModularRealmAuthenticator">
				<!-- 多realms配置 -->
				<property name="realms">
					<list>
						<value>
							<ref bean="jdbcAuthenticationRealm" />
							<ref bean="otherAuthenticationRealm" />
						</value>
					</list>
				</property>
			</bean>
		</property>
	</bean>

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="filters">
			<map>
				<entry key="captchaAuthc" value-ref="captchaAuthenticationFilter" />
			</map>
		</property>
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

在[base-framework](https://github.com/dactiv/base-framework "base-framework")中没有多realms例子，如果存在什么问题。可以到[这里](https://github.com/dactiv/base-framework/issues "issues")提问。

#### 1.6 更好的性能 shiro + cache ####

在许多应用程序中性能是至关重要的。缓存是从第一天开始第一个建立在 shiro 中的一流功能，以确保安全操作保持尽可能的快。然而，缓存作为一个概念是 shiro 的基本组成部分，实现一个完整的缓存机制是安全框架核心能力之外的事情。为此，shiro 的缓存支持基本上是一个抽象的（包装）API，它将“坐”在一个基本的缓存机制产品（例如，Ehcache，OSCache，Terracotta，Coherence，GigaSpaces，JBossCache 等）之上。这允许 shiro 终端用户配置他们喜欢的任何缓存机制。

shiro 有三个重要的缓存接口：

1. CacheManager - 负责所有缓存的主要管理组件，它返回 Cache 实例。
1. Cache - 维护key/value 对。
1. CacheManagerAware - 通过想要接收和使用 CacheManager 实例的组件来实现。

CacheManager 返回 Cache 实例，各种不同的 shiro 组件使用这些 Cache 实例来缓存必要的数据。任何实现了 CacheManagerAware 的 shiro 组件将会自动地接收一个配置好的 CacheManager，该 CacheManager 能够用来获取 Cache 实例。

shiro 的 SecurityManager 实现及所有 AuthorizingRealm 实现都实现了 CacheManagerAware 。如果你在 SecurityManager 上设置了 CacheManger，它反过来也会将它设置到实现了 CacheManagerAware 的各种不同的Realm 上（OO delegation）。

那么为了方便，本节就使用先在比较流行的 ehcache 来做讲解，将spring的cache和shiro的cache结合起来用，通过spring的缓存注解来“缓存数据”，“清除缓存”等操作。

具体配置文件如下，applicationContext.xml:

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
		<!-- cacheManager,集合spring缓存工厂 -->
		<property name="cacheManager" ref="cacheManager" />
	</bean>

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="filters">
			<map>
				<entry key="captchaAuthc" value-ref="captchaAuthenticationFilter" />
			</map>
		</property>
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

	<!-- spring对ehcache的缓存工厂支持 -->
	<bean id="ehCacheManagerFactory" class="org.springframework.cache.ehcache.EhCacheManagerFactoryBean">
		<property name="configLocation" value="classpath:ehcache.xml" />
	</bean>
	
	<!-- spring对ehcache的缓存管理 -->
	<bean id="ehCacheManager" class="org.springframework.cache.ehcache.EhCacheCacheManager">
		<property name="cacheManager" ref="ehCacheManagerFactory"></property>
	</bean>
	
	<!-- 使用缓存annotation 配置 -->
	<cache:annotation-driven cache-manager="ehCacheManager" proxy-target-class="true" />

通过以上修改将spring cache和shiro cache整合了起来。完成这个之后先解决一个session集群同步的问题。

在很多web应用中，session共享是非常必要的一件事。而shiro提供了**SessionManager**给开发人员去管理和维护当前的session。当然，shiro所写的sessionDao 本人认为已经够用了。如果想使用nosql来做存储的话。可以实现SessionManager接口去做你自己的业务逻辑。

*提示:*

*SessionManager(org.apache.shiro.session.SessionManager)知道如何去创建及管理用户 Session 生命周期来为所有环境下的用户提供一个强健的 Session 体验。这在安全框架界是一个独有的特色 shiro 拥有能够在任何环境下本地化管理用户 Session 的能力，即使没有可用的 Web/Servlet 或 EJB 容器，它将会使用它内置的企业级会话管理来提供同样的编程体验。SessionDAO 的存在允许任何数据源能够在持久会话中使用。*

*SesssionDAO代表SessionManager 执行Session 持久化（CRUD）操作。这允许任何数据存储被插入到会话管理的基础之中。SessionDAO 的权力是你能够实现该接口来与你想要的任何数据存储进行通信。这意味着你的会话数据可以驻留在内存/缓存中，文件系统，关系数据库或NoSQL 的数据存储，或其他任何你需要的位置。你得控制持久性行为。*

*EHCache SessionDAO 默认是没有启用的，但如果你不打算实现你自己的SessionDAO，那么强烈地建议你为 shiro 的
SessionManagerment 启用EHCache 支持。EHCache SessionDAO 将会在内存中保存会话，并支持溢出到磁盘，若内存成为制约。这对生产程序确保你在运行时不会随机地“丢失”会话是非常好的。*

那么修改applicationContext.xml相关的配置:


	<!-- 使用EnterpriseCacheSessionDAO，将session放入到缓存，通过同步配置，将缓存同步到其他集群点上，解决session同步问题。 -->
    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">
    	<!-- 活动session缓存名称 -->
    	<property name="activeSessionsCacheName" value="shiroActiveSessionCache" />
    </bean>
    
    <!-- 考虑到集群，使用DefaultWebSessionManager来做sessionManager -->
    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
    	<!-- 使用EnterpriseCacheSessionDAO，解决session同步问题 -->
    	<property name="sessionDAO" ref="sessionDAO" />
    </bean>

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
		<!-- cacheManager,集合spring缓存工厂 -->
		<property name="cacheManager" ref="cacheManager" />
		<!-- 考虑到集群，使用DefaultWebSessionManager来做sessionManager -->
		<property name="sessionManager" ref="sessionManager" />
	</bean>

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="filters">
			<map>
				<entry key="captchaAuthc" value-ref="captchaAuthenticationFilter" />
			</map>
		</property>
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

	<!-- spring对ehcache的缓存工厂支持 -->
	<bean id="ehCacheManagerFactory" class="org.springframework.cache.ehcache.EhCacheManagerFactoryBean">
		<property name="configLocation" value="classpath:ehcache.xml" />
	</bean>
	
	<!-- spring对ehcache的缓存管理 -->
	<bean id="ehCacheManager" class="org.springframework.cache.ehcache.EhCacheCacheManager">
		<property name="cacheManager" ref="ehCacheManagerFactory"></property>
	</bean>
	
	<!-- 使用缓存annotation 配置 -->
	<cache:annotation-driven cache-manager="ehCacheManager" proxy-target-class="true" />

注意sessionDAO这个bean 里面的属性activeSessionsCacheName就是ehcache的缓存名称。通过该名称可以配置ehcache的缓存性质。

**ehcache.xml:**

	<?xml version="1.0" encoding="UTF-8"?>
	<ehcache>  
	    
	    <cacheManagerPeerProviderFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory" 
												properties="peerDiscovery=automatic,
												multicastGroupAddress=230.0.0.1,
												multicastGroupPort=4446,
												timeToLive=32" />
		
		<cacheManagerPeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory" /> 
		
	    <defaultCache maxElementsInMemory="10000" 
					  eternal="false" 
					  timeToIdleSeconds="300" 
					  timeToLiveSeconds="600" 
					  overflowToDisk="true"/>

	    <!-- shiro的活动session缓存名称 -->
	    <cache name="shiroActiveSessionCache" 
					 maxElementsInMemory="10000" 
					 timeToLiveSeconds="1200" 
					 memoryStoreEvictionPolicy="LRU">

	    	<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory" 
									   properties="replicateAsynchronously=false"/>
	    </cache>
	    
	</ehcache>

在shiroActiveSessionCache缓存里。集群的配置为同步缓存。作用是subject的getSession能够在所有集群的服务器上共享数据。

完成session共享后在发挥shiro的缓存功能，以**1.6 shiro 认证、授权**章节为例子，将**认证缓存** 和 **授权缓存** 一起解决。

**认证缓存**的用作相当于**"热用户"**的概念，意思就是说：

1. 当一个用户进行登录成功后，将该用户记录到缓存中，当下次登录时，不在去查数据库，而是直接在缓存中获取用户信息，
2. 当缓存满了。而就将缓存里最少使用的用户踢出去。
 
shiro 的 realm就能实现这个需求，shiro 的 realm 本身就支持缓存。而缓存的踢出规则，ehcache 就可以配置该规则。但是当用户修改信息时，需要将缓存清除。不然下次登录时，登录密码用以前旧的密码一样能够登录，新的密码就不起作用。

具体applicationContext.xml配置如下:

	<!-- 使用EnterpriseCacheSessionDAO，将session放入到缓存，通过同步配置，将缓存同步到其他集群点上，解决session同步问题。 -->
    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">
    	<!-- 活动session缓存名称 -->
    	<property name="activeSessionsCacheName" value="shiroActiveSessionCache" />
    </bean>
    
    <!-- 考虑到集群，使用DefaultWebSessionManager来做sessionManager -->
    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
    	<!-- 使用EnterpriseCacheSessionDAO，解决session同步问题 -->
    	<property name="sessionDAO" ref="sessionDAO" />
    </bean>

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
		<!-- 启用认证缓存，当用户登录一次后将不在查询数据库来获取用户信息，直接在从缓存获取 -->
    	<property name="authenticationCachingEnabled" value="true" />
    	<!-- 认证缓存名称 -->
    	<property name="authenticationCacheName" value="shiroAuthenticationCache" />
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
		<!-- cacheManager,集合spring缓存工厂 -->
		<property name="cacheManager" ref="cacheManager" />
		<!-- 考虑到集群，使用DefaultWebSessionManager来做sessionManager -->
		<property name="sessionManager" ref="sessionManager" />
	</bean>

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="filters">
			<map>
				<entry key="captchaAuthc" value-ref="captchaAuthenticationFilter" />
			</map>
		</property>
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

	<!-- spring对ehcache的缓存工厂支持 -->
	<bean id="ehCacheManagerFactory" class="org.springframework.cache.ehcache.EhCacheManagerFactoryBean">
		<property name="configLocation" value="classpath:ehcache.xml" />
	</bean>
	
	<!-- spring对ehcache的缓存管理 -->
	<bean id="ehCacheManager" class="org.springframework.cache.ehcache.EhCacheCacheManager">
		<property name="cacheManager" ref="ehCacheManagerFactory"></property>
	</bean>
	
	<!-- 使用缓存annotation 配置 -->
	<cache:annotation-driven cache-manager="ehCacheManager" proxy-target-class="true" />

在jdbcAuthenticationRealm这个bean里启用了认证缓存，而这个缓存的名称是shiroAuthenticationCache。

*提示:shiro默认不启动认证缓存，如果需要启用，必须在realm里将authenticationCachingEnabled设置成true*

**ehcache.xml:**

	<?xml version="1.0" encoding="UTF-8"?>
	<ehcache>  
	    
	    <cacheManagerPeerProviderFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory" 
												properties="peerDiscovery=automatic,
												multicastGroupAddress=230.0.0.1,
												multicastGroupPort=4446,
												timeToLive=32" />
		
		<cacheManagerPeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory" /> 
		
	    <defaultCache maxElementsInMemory="10000" 
					  eternal="false" 
					  timeToIdleSeconds="300" 
					  timeToLiveSeconds="600" 
					  overflowToDisk="true"/>

	    <!-- shiro的活动session缓存名称 -->
	    <cache name="shiroActiveSessionCache" 
					 maxElementsInMemory="10000" 
					 timeToLiveSeconds="1200" 
					 memoryStoreEvictionPolicy="LRU">

	    	<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory" 
									   properties="replicateAsynchronously=false"/>
	    </cache>

	    <!-- shiro认证的缓存名称 -->
    	<cache name="shiroAuthenticationCache" 
					 maxElementsInMemory="10000" 
					 timeToLiveSeconds="1200" 
					 memoryStoreEvictionPolicy="LRU">

    		<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory 
								   properties="replicateAsynchronously=false""/>
    	</cache>

	</ehcache>

在ehcache.xml中添加了shiroAuthenticationCache缓存。并且memoryStoreEvictionPolicy属性为LRU，LRU就是当缓存满了将**“最近最少访问”**的缓存踢出。

那么，通过以上配置完成了“热用户”，还有一步就是当修改用户时，将缓存清除，让下次这个用户登录时，重新去数据库加载新的数据进行认证。

shiro在存储授权用户缓存时，会将用户登录账户做键，实体做值的方式进行存储到缓存中。所以，当修改用户时，通过用户的登录帐号，和spring的缓存注解，将该缓存清空。具体代码如下:

	@Repository
	public class UserDao extends BasicHibernateDao<User, String> {
	
		/**通过登录帐号获取用户实体**/
	    public User getUserByUsername(String username) {
	        return findUniqueByProperty("username", username);
	    }
		
		
		/**通过用户实体信息修改用户**/
		//当更新后将shiro的认证缓存也更新，保证shiro和当前的用户一致
		@CacheEvict(value="shiroAuthenticationCache",key="#entity.getUsername()")
		public void updateUser(User entity) {
			update(entity);
		}

		/**通过用户实体删除用户**/
		//当更新后将shiro的认证缓存也更新，保证shiro和当前的用户一致
		@CacheEvict(value="shiroAuthenticationCache",key="#entity.getUsername()")
		public void deleteUser(User entity) {
			delete(entity);
		}
		
	}

通过以上代码，当调用updateUser或deletUser方法完成后，spring cache 会将 shiroAuthenticationCache缓存块里key为当前用户的登录帐号的缓存进行清除。

**授权缓存**的作用大部分是快速获取用户的认证信息，如果存在两个集群点，可以直接使用同步的功能将缓存同步到其他服务器里，当下次访问服务器时，当出现某台服务器没有进行授权工作时，不在进行授权的工作。具体配置如下：

**applicationContext.xml:**

	<!-- 使用EnterpriseCacheSessionDAO，将session放入到缓存，通过同步配置，将缓存同步到其他集群点上，解决session同步问题。 -->
    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">
    	<!-- 活动session缓存名称 -->
    	<property name="activeSessionsCacheName" value="shiroActiveSessionCache" />
    </bean>
    
    <!-- 考虑到集群，使用DefaultWebSessionManager来做sessionManager -->
    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
    	<!-- 使用EnterpriseCacheSessionDAO，解决session同步问题 -->
    	<property name="sessionDAO" ref="sessionDAO" />
    </bean>

	<!-- 自定义shiro的realm数据库身份验证 -->
	<bean id="jdbcAuthenticationRealm" class="domain.JdbcAuthenticationRealm">
		<property name="name" value="jdbcAuthentication" />
		<property name="credentialsMatcher">
			<bean class="org.apache.shiro.authc.credential.HashedCredentialsMatcher">
				<property name="hashAlgorithmName" value="MD5" />
			</bean>
		</property>
		<!-- 授权缓存名称 -->
    	<property name="authorizationCacheName" value="shiroAuthorizationCache" />
		<!-- 启用认证缓存，当用户登录一次后将不在查询数据库来获取用户信息，直接在从缓存获取 -->
    	<property name="authenticationCachingEnabled" value="true" />
    	<!-- 认证缓存名称 -->
    	<property name="authenticationCacheName" value="shiroAuthenticationCache" />
	</bean>
    
    <!-- 使用默认的WebSecurityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
		<!-- realm认证和授权,从数据库读取资源 -->
		<property name="realm" ref="jdbcAuthenticationRealm" />
		<!-- cacheManager,集合spring缓存工厂 -->
		<property name="cacheManager" ref="cacheManager" />
		<!-- 考虑到集群，使用DefaultWebSessionManager来做sessionManager -->
		<property name="sessionManager" ref="sessionManager" />
	</bean>

	<!-- 自定义对 shiro的连接约束,结合shiroSecurityFilter实现动态获取资源 -->
	<bean id="chainDefinitionSectionMetaSource" class="domian.ChainDefinitionSectionMetaSource">
		<!-- 默认的连接配置 -->
		<property name="filterChainDefinitions">
			<value>
				/login = captchaAuthc
				/logout = logout
				/index = perms[security:index]
			</value>
		</property>
	</bean>

	<!-- 将shiro与spring集合 -->
	<bean id="shiroSecurityFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="filters">
			<map>
				<entry key="captchaAuthc" value-ref="captchaAuthenticationFilter" />
			</map>
		</property>
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

	<!-- spring对ehcache的缓存工厂支持 -->
	<bean id="ehCacheManagerFactory" class="org.springframework.cache.ehcache.EhCacheManagerFactoryBean">
		<property name="configLocation" value="classpath:ehcache.xml" />
	</bean>
	
	<!-- spring对ehcache的缓存管理 -->
	<bean id="ehCacheManager" class="org.springframework.cache.ehcache.EhCacheCacheManager">
		<property name="cacheManager" ref="ehCacheManagerFactory"></property>
	</bean>
	
	<!-- 使用缓存annotation 配置 -->
	<cache:annotation-driven cache-manager="ehCacheManager" proxy-target-class="true" />

在applicationContext.xml里的jdbcAuthenticationRealm bean 添加了authorizationCacheName，值为:shiroAuthorizationCache


*提示:shiro默认启动授权缓存，如果不想使用授权缓存，将会每次访问到有perms的url都会授权一次。*

**ehcache.xml:**

	<?xml version="1.0" encoding="UTF-8"?>
	<ehcache>  
	    
	    <cacheManagerPeerProviderFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory" 
												properties="peerDiscovery=automatic,
												multicastGroupAddress=230.0.0.1,
												multicastGroupPort=4446,
												timeToLive=32" />
		
		<cacheManagerPeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory" /> 
		
	    <defaultCache maxElementsInMemory="10000" 
					  eternal="false" 
					  timeToIdleSeconds="300" 
					  timeToLiveSeconds="600" 
					  overflowToDisk="true"/>

	    <!-- shiro的活动session缓存名称 -->
	    <cache name="shiroActiveSessionCache" 
					 maxElementsInMemory="10000" 
					 timeToLiveSeconds="1200" 
					 memoryStoreEvictionPolicy="LRU">

	    	<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory" 
									   properties="replicateAsynchronously=false"/>
	    </cache>

	    <!-- shiro认证的缓存名称 -->
    	<cache name="shiroAuthenticationCache" 
					 maxElementsInMemory="10000" 
					 timeToLiveSeconds="1200" 
					 memoryStoreEvictionPolicy="LRU">

    		<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory 
								   properties="replicateAsynchronously=false""/>
    	</cache>

		<!-- shiro授权的缓存名称 -->
	    <cache name="shiroAuthorizationCache" 
			   maxElementsInMemory="10000" 
			   timeToLiveSeconds="1200">

	    	<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory 
									   properties="replicateAsynchronously=false""/>

	    </cache>

	</ehcache>


在ehcache中添加shiroAuthorizationCache缓存。将完成授权缓存同步。当修改或删除某些角色时，记得要清除所有缓存，让用户下次访问时授权一次，不然修改了角色需要重启服务器后才能生效。

	@Repository
	public class GroupDao extends BasicHibernateDao<Group, String> {
	
		/**通过用户实体信息修改用户**/
		@CacheEvict(value="shiroAuthorizationCache",allEntries=true)
		public void updateGroup(Group entity) {
			update(entity);
		}

		/**通过用户实体删除用户**/
		@CacheEvict(value="shiroAuthorizationCache",allEntries=true)
		public void deleteGroup(Group entity) {
			delete(entity);
		}
		
	}

具体的过程在[base-framework](https://github.com/dactiv/base-framework "base-framework")的showcase的base-curd项目下有例子，如果看不懂。可以根据例子去理解。
