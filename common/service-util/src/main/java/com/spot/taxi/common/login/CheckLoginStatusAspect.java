package com.spot.taxi.common.login;

import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class CheckLoginStatusAspect {
    private final RedisTemplate<String, String> redisTemplate;

    @Around("execution(* com.spot.taxi.*.controller.*.*(..)) && @annotation(CheckLoginStatus)")
    public Object Login(ProceedingJoinPoint proceedingJoinPoint, CheckLoginStatus CheckLoginStatus) throws Throwable {
        log.debug("进入登录检查切面");
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();

        String token = request.getHeader("token");

//        System.out.println("token = " + token);
        if (!StringUtils.hasText(token)) {
            log.error("token为空");
            throw new CustomException(ResultCodeEnum.LOGIN_AUTH);
        }

        String userId = redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        if (StringUtils.hasText(userId)) {
            AuthContextHolder.setUserId(Long.valueOf(userId));
        }

        return proceedingJoinPoint.proceed();
    }

}
