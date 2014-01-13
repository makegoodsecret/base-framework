package com.github.dactiv.showcase.web;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import com.github.dactiv.common.spring.mvc.SpringMvcHolder;
import com.github.dactiv.showcase.service.ServiceException;

/**
 * 系统异常控制器
 * 
 * @author maurice
 *
 */
@Controller
@ControllerAdvice
public class SystemExceptionController {
	
	/**
	 * 没有权限控制器，返回exception/unauthorized.html
	 * 
	 * @return String
	 */
	@RequestMapping("/unauthorized")
	public String unauthorized() {
		return "exception/unauthorized";
	}
	
	/**
	 * 
	 * 出现service异常拦截的控制器，返回exception/service-exception.html
	 * 
	 * @param exception 业务层异常
	 * 
	 * @return String
	 */
	@ExceptionHandler(ServiceException.class)
	public String serviceException(ServiceException exception) {
		SpringMvcHolder.addRequestAttribute("exception", exception);
		return "exception/service-exception";
	}
	
	/**
	 * 
	 * 出现验证异常拦截的控制器，返回exception/bind-exception.html
	 * 
	 * @param exception 验证异常
	 * 
	 * @return String
	 */
	@ExceptionHandler(BindException.class)
	public String bindException(BindException exception) {
		SpringMvcHolder.addRequestAttribute("errors", exception.getBindingResult().getAllErrors());
		return "exception/bind-exception";
	}
	
	/**
	 * 
	 * 出现任何异常拦截的控制器(除了BindException和ServiceException)，返回exception/global-exception.html
	 * 
	 * @param throwable 异常
	 * 
	 * @return String
	 */
	@ExceptionHandler
	public String globalException(Throwable throwable) {
		throwable.printStackTrace();
		return "exception/global-exception";
	}
}
