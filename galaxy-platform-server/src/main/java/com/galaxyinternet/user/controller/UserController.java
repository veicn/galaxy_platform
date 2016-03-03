package com.galaxyinternet.user.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.galaxyinternet.bo.UserBo;
import com.galaxyinternet.common.controller.BaseControllerImpl;
import com.galaxyinternet.exception.PlatformException;
import com.galaxyinternet.framework.core.constants.Constants;
import com.galaxyinternet.framework.core.constants.UserConstant;
import com.galaxyinternet.framework.core.model.Page;
import com.galaxyinternet.framework.core.model.PageRequest;
import com.galaxyinternet.framework.core.model.ResponseData;
import com.galaxyinternet.framework.core.model.Result;
import com.galaxyinternet.framework.core.model.Result.Status;
import com.galaxyinternet.framework.core.service.BaseService;
import com.galaxyinternet.framework.core.utils.mail.SimpleMailSender;
import com.galaxyinternet.model.department.Department;
import com.galaxyinternet.model.user.User;
import com.galaxyinternet.service.DepartmentService;
import com.galaxyinternet.service.UserService;

/**
 * 用户相关
 * 
 * @author zhaoying
 *
 */
@Controller
@RequestMapping("/galaxy/user")
public class UserController extends BaseControllerImpl<User, UserBo> {
	final Logger logger = LoggerFactory.getLogger(UserController.class);
	@Autowired
	private UserService userService;

	@Autowired
	private DepartmentService departmentService;
	@Override
	protected BaseService<User> getBaseService() {
		return this.userService;
	}

	/**
	 * 默认页面
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String list(HttpServletRequest request,
			HttpServletResponse response) {
	/*	// 部门列表
		List<Department> deptList = departmentService.queryAll();
		// 默认取一页数据
		PageRequest pageable = new PageRequest();
		Page<User> page = userService.queryUserList(new User(), pageable);
		request.setAttribute("deptList", deptList);
		request.setAttribute("content", page.getContent());
		*/ 
		return"system/user/user_list";
	}

	/**
	 * 重置密码 邮件通知
	 * 
	 * @param user
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/resetPwd", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseData<User> resetPwd(@RequestBody User user) {
		
		ResponseData<User> responseBody = new ResponseData<User>();
		
		try {
			userService.resetPwd(user.getId());
			responseBody.setResult(new Result(Status.OK, user));

		} catch (PlatformException e) {
			responseBody
					.setResult(new Result(Status.ERROR, "resetPwd faild"));

			if (logger.isErrorEnabled()) {
				logger.error("resetPwd ", e);
			}
		}
		
		String toMail = user.getEmail(); // "sue_vip@126.com"; 收件人邮件地址
		String content = "<html>" + "<head></head>" + "<body>"
				+ "<div align=center>"
				+ "	<a href=http://localhost:8000/controller/vcs/login/toLogin target=_blank>"
				+ "您好，您密码已重置，请点击地址：http://localhost:8000/controller/vcs/login/toLogin  登陆 "
				+ "	</a>" + "</div>" + "</body>" + "</html>";// 邮件内容
		String subject = "重置密码通知";// 邮件主题
		SimpleMailSender.sendHtmlMail(toMail, subject, content);
		
		responseBody.setResult(new Result(Status.OK, user));
		return responseBody;
	}

	@ResponseBody
	@RequestMapping(value = "/disableUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseData<User> disableUser(@RequestBody User user) {

		if (StringUtils.equals(user.getStatus(), UserConstant.NORMAL)) {
			user.setStatus(UserConstant.DISABLE);
		} else {
			user.setStatus(UserConstant.NORMAL);
		}
		ResponseData<User> responseBody = new ResponseData<User>();
		try {

			userService.updateByIdSelective(user);
			responseBody.setResult(new Result(Status.OK, user));

		} catch (PlatformException e) {
			responseBody
					.setResult(new Result(Status.ERROR, "disableUser faild"));

			if (logger.isErrorEnabled()) {
				logger.error("disableUser ", e);
			}
		}
		
		return responseBody;
	}

	/**
	 * 获取用户列表数据 重新组装关联数据
	 * 
	 * @param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/queryUserList", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseData<User> queryUserList(HttpServletRequest request,
			@RequestBody User query, PageRequest pageable) {

		ResponseData<User> responseBody = new ResponseData<User>();

		Object obj = request.getSession().getAttribute(Constants.SESSION_USER_KEY);
		if (obj == null) {
			responseBody.setResult(
					new Result(Status.ERROR, "validate loging session failed"));
			return responseBody;
		}

		try {

			Page<User> page = userService.queryUserList(query, pageable);
			responseBody.setPageList(page);
			responseBody.setResult(new Result(Status.OK, ""));
			return responseBody;

		} catch (PlatformException e) {
			responseBody
					.setResult(new Result(Status.ERROR, "queryUserList faild"));

			if (logger.isErrorEnabled()) {
				logger.error("queryUserList ", e);
			}
		}

		return responseBody;
	}
	@ResponseBody
	@RequestMapping(value = "/updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseData<UserBo> updateUser(@RequestBody UserBo user) {
		ResponseData<UserBo> responseBody = new ResponseData<UserBo>();
		Result result = new Result();
		try {
			userService.updateUser(user);
			result.setStatus(Status.OK);
		} catch (PlatformException e) {
			result.addError(e.getMessage());
		} catch (Exception e) {
			result.addError("系统错误");
			logger.error("更新错误", e);
		}
		responseBody.setResult(result);
		return responseBody;
	}

	/**
	 * 获取部门列表
	 * @author zhaoying
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/departmentList", method = RequestMethod.GET)
	public ResponseData<Department> departmentList(Integer type) {

		ResponseData<Department> responseBody = new ResponseData<Department>();
		try {
			// 部门列表
			List<Department> deptList = null;
			if (null == type) {
				deptList = departmentService.queryAll();
			} else {
				deptList = departmentService.queryListByType(type);
			}
			responseBody.setEntityList(deptList);
			responseBody.setResult(new Result(Status.OK, ""));
			return responseBody;

		} catch (PlatformException e) {
			responseBody
					.setResult(new Result(Status.ERROR, "departmentList faild"));

			if (logger.isErrorEnabled()) {
				logger.error("departmentList ", e);
			}
		}
		return responseBody;
	}

}
