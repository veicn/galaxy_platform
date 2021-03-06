package com.galaxyinternet.user.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
import com.galaxyinternet.framework.core.constants.Constants;
import com.galaxyinternet.framework.core.model.BuryPoint;
import com.galaxyinternet.framework.core.model.BuryPointEntity;
import com.galaxyinternet.framework.core.model.Header;
import com.galaxyinternet.framework.core.model.ResponseData;
import com.galaxyinternet.framework.core.model.Result;
import com.galaxyinternet.framework.core.model.Result.Status;
import com.galaxyinternet.framework.core.service.BaseService;
import com.galaxyinternet.framework.core.utils.BuryRequest;
import com.galaxyinternet.framework.core.utils.DateUtil;
import com.galaxyinternet.model.auth.UserResult;
import com.galaxyinternet.model.user.User;
import com.galaxyinternet.service.UserService;
import com.galaxyinternet.utils.AuthRequest;

@Controller
@RequestMapping("/galaxy/userlogin")
public class LoginController extends BaseControllerImpl<User, UserBo> {
	final Logger logger = LoggerFactory.getLogger(LoginController.class);
	@Autowired
	private UserService userService;
	@Autowired
	com.galaxyinternet.framework.cache.Cache cache;
	@Autowired
	private AuthRequest authReq;
	@Autowired
	private BuryRequest buryRequest;

	@Override
	protected BaseService<User> getBaseService() {
		return this.userService;
	}

	/**
	 * 跳转登录
	 */
	@RequestMapping(value = "/toLogin")
	public String toLogin() {
		return "system/login";
	}

	/**
	 * 用户登录
	 * 
	 * @author zcy
	 */
	@ResponseBody
	@RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseData<User> login(@RequestBody User user, HttpServletRequest request) {
		ResponseData<User> responsebody = new ResponseData<User>();
		//登出 - 防止用户关闭前未登出
		Object attr = request.getSession().getAttribute(Constants.SESSION_USER_KEY);
		if(attr != null)
		{
			request.getSession().invalidate();
		}
		String email = user.getEmail();
		String password = user.getPassword();
		String aclient=user.getAclient();
		if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
			responsebody.setResult(new Result(Status.ERROR, Constants.IS_UP_EMPTY, "用户名或密码不能为空！"));
			return responsebody;
		}
		UserResult rtn = authReq.login(email, password);
		if(rtn == null || rtn.isSuccess() == false)
		{
			String msg = "";
			if(rtn.getMessage() != null)
			{
				msg = rtn.getMessage();
			}
			else
			{
				msg = "用户名或密码错误！";
			}
			responsebody.setResult(new Result(Status.ERROR, Constants.IS_UP_WRONG, msg));
			return responsebody;
		}
		user = rtn.getValue();
		String sessionId = request.getSession().getId(); // 生成sessionId
		setCacheSessionId(request, user, sessionId);
		Header header = getHeader(user, sessionId);
		responsebody.setHeader(header);
		user.setPassword(null);
		user.setCreatedTime(null);
		user.setUpdatedTime(null);
		user.setBirth(null);
		user.setBirthStr(null);
		user.setOriginPassword(null);
		user.setTelephone(null);
		user.setStatus(null);
		user.setGender(null);
		responsebody.setEntity(user);
		responsebody.setResult(new Result(Status.OK, Constants.OPTION_SUCCESS, "登录成功！"));
		
		Date date=new Date();
		long dateTime=DateUtil.dateToLong(date);
		BuryPoint buryPoint=new BuryPoint();
		buryPoint.setpCode("93");
		buryPoint.setUserId(user.getId().toString());
		buryPoint.setRecordDate(dateTime+"");
		buryPoint.setOs("1");
		buryPoint.setOsVersion(aclient);
		buryPoint.setOsType("");
		buryPoint.setHardware("");
		BuryPointEntity entity=new BuryPointEntity();
		List<BuryPoint>  list=new ArrayList<BuryPoint>();
		list.add(buryPoint);
		entity.setList(list);
		entity.setSessionId(sessionId);
		entity.setUserId(user.getId().toString());
		buryRequest.burySave(entity);
		/**
		 * zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
		 */
		/**UserLoginHis userLogonHis = new UserLoginHis();	
		User query=new User();
		query.setId(user.getId());
		User userOne = userService.queryOne(query);
		Date date = new Date();       
	    Timestamp initdate = new Timestamp(date.getTime()); 
		userLogonHis.setUserId(user.getId());
		userLogonHis.setLoginDate(date);
		userLogonHis.setAccessClient("PC");
		userLogonHis.setAndroidClient(aclient);
		 List<UserLoginHis> selectUserLogonHis = userLoginHisService.selectUserLogonHis(userLogonHis);
		if(null!=selectUserLogonHis&&selectUserLogonHis.size()>0){
			UserLoginHis queryOne=selectUserLogonHis.get(0);
			Integer logonTimes = queryOne.getLogonTimes();
			queryOne.setLogonTimes(logonTimes+1);
			queryOne.setInitLogonTime(initdate);
			userLoginHisService.updateUserLogonHis(queryOne);
		}else{
			userLogonHis.setLogonTimes(1);
			userLogonHis.setInitLogonTime(initdate);
			userLogonHis.setNickName(userOne.getNickName());
			userLoginHisService.insertUserLogonHis(userLogonHis);
		}*/
		
		/**
		 * zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
		 */
		logger.info("Login success{userId:" + user.getId() + ", email:" + user.getEmail() + ", userRealName:" + user.getRealName() + "}");
		return responsebody;
	}

	/**
	 * @author zcy
	 * @param user
	 * @param role
	 * @return
	 */
	private Header getHeader(User user, String sessionId) {
		Header header = new Header();
		header.setLoginName(user.getEmail());
		header.setSessionId(sessionId);
		header.setUserId(user.getId());
		return header;
	}

	private void setCacheSessionId(HttpServletRequest request, User user, String sessionId) {
		user.setSessionId(sessionId);
		request.getSession().setAttribute(Constants.SESSION_USER_KEY, user);
		User cacheUser = new User();
		cacheUser.setId(user.getId());
		cacheUser.setEmail(user.getEmail());
		cacheUser.setDepartmentId(user.getDepartmentId());
		cacheUser.setSessionId(sessionId);
		int secs = request.getSession().getMaxInactiveInterval();
		cache.setByRedis(sessionId, cacheUser, secs);
	}
	

	/**
	 * 用户注销
	 * 
	 * @author zcy
	 */
	@ResponseBody
	@RequestMapping(value = "/logout", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseData<User> logout(HttpServletRequest request) {
		ResponseData<User> responsebody = new ResponseData<User>();
		String sessionId = request.getHeader(Constants.SESSION_ID_KEY);
		if (StringUtils.isBlank(sessionId)) {
			responsebody.setResult(new Result(Status.ERROR, Constants.IS_SESSIONID_EMPTY, "sessionId为空！"));
			return responsebody;
		}
		boolean flag = removeSessionId(sessionId, request); // 从session和cache中删除sessionId
		if (!flag) {
			responsebody.setResult(new Result(Status.ERROR, Constants.INVALID_SESSIONID, "sessionId错误！"));
			return responsebody;
		}
		request.getSession().invalidate();
		responsebody.setResult(new Result(Status.OK, Constants.OPTION_SUCCESS, "退出登录"));
		return responsebody;
	}

	/**
	 * 删除session 和 cache中的 sessionId
	 * 
	 * @author zcy
	 * @param key
	 */
	private boolean removeSessionId(String sessionId, HttpServletRequest request) {
		Object cahceUser = cache.get(sessionId);
		Object sessionUser = request.getSession().getAttribute(Constants.SESSION_USER_KEY);

		if (null == cahceUser || null == sessionUser) {
			return false;
		}
		request.getSession().removeAttribute(Constants.SESSION_USER_KEY); // 从本地session删除user
		cache.remove(sessionId); // 从redis中删除sessionId
		return true;
	}

}
