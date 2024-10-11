package com.sys.config;

import com.sys.common.ErrorCode;
import com.sys.common.ResponseResult;
import com.sys.excption.BusinessException;
import com.sys.utils.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Slf4j
@Component
@RequiredArgsConstructor
public class SessionInterceptor implements HandlerInterceptor {

	/**
	 * 每次请求都查询用户是否登录
	 */

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		//先判断，当前用户是否已经登录，已经登录就放行
		//session中没有user值，然后再根据cookie里面的token值与用户进行匹配

		String url = request.getRequestURI();

//        如果为登录接口，之间放行
		if (url.equals("/login") || url.equals("/getDeptList")) {
			return true;
		}

        boolean isAllow = true;

        Cookie[] cookies = request.getCookies();


		if (cookies != null && cookies.length > 0) {
			for (Cookie cookie : cookies) {
				CookieUtil.deleteCookie(request,response,cookie.getName());
				CookieUtil.setCookieValue(request,response,
						cookie.getName(), cookie.getValue(),cookie.getMaxAge());
			}
		} else {
			isAllow = false;
		}

//        如果不允许
        if (!isAllow) {
            throw new BusinessException(ErrorCode.TOKEN_ERROR);
        }

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

	}

}
