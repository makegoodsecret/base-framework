#### 框架说明 ####


base-framework是对常用的java web开发封装实用功能来提高开发效率的底层框架。base-framework基于spring做核心框架、hibernate或spring data jpa做持久化框架，用 spring mvc 框架对 mvc 做管理。使用到的新功能有 spring 缓存工厂、apeche shiro 安全框架、spring mvc，spring data jpa 等主要流行技术，该项目分为两个部分做底层的封装，和带有一个项目演示例子。

**dactiv common**：该jar包是对基本的常用工具类的一些简单封装。如泛型，反射，配置文件等工具类的封装。

获取dactiv common jar:

	<dependency>
	    <groupId>com.github.dactiv</groupId>
	    <artifactId>dactiv-common</artifactId>
	    <version>1.1.0.RELEASE</version>
	</dependency>

**dactiv orm**：该jar包是对持久化层的框架封装，目前只对 Hibernate 4 和 spring data jpa 的 curd 和辅助查询功能封装。

获取dactiv orm jar:

	<dependency>
	    <groupId>com.github.dactiv</groupId>
	    <artifactId>dactiv-orm</artifactId>
	    <version>1.1.0.RELEASE</version>
	</dependency>

**项目功能演示例子**：在文件夹的shorcase里有一个base-curd项目。该项目是对以上两个框架(dactiv-common和dactiv-orm)和其他技术的整合做的例子，通过该例子使用maven做了一个archetype基础模板。可以通过该archetype来生成一个新的项目。该文件在base-curd\bin下面（archetype-generate.bat）。

通过base-curd项目文件夹中的bin/jetty.bat文件运行项目，也可以用eclipse.bat生成项目导入到开发工具中在运行。该工程下有一个基于jeety运行的java文件org.dactiv.showcase.test.LaunchJetty，。你也可以通过该文件运行整个项目。

#### 安装说明 ####

1. 配置好maven。
2. 点击项目跟目录的quick-start。

[相关帮助文档](https://github.com/dactiv/base-framework/blob/master/doc/reference.md)