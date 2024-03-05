package org.hanghae.markethub.global.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hanghae.markethub.global.security.impl.UserDetailsServiceImpl;
import org.hanghae.markethub.global.security.jwt.JwtUtil;
import org.hanghae.markethub.global.security.service.SecurityRedisService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final SecurityRedisService securityRedisService;


    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        log.info("dofilterInternal 실행");
        String accessToken = jwtUtil.getTokenFromRequest(req, jwtUtil.AUTHORIZATION_HEADER); // 변경된 쿠키 이름으로 수정
        accessToken = jwtUtil.substringToken(accessToken);
        if (!jwtUtil.validateToken(accessToken)) {
            String refreshToken = jwtUtil.getTokenFromRequest(req, jwtUtil.REFRESHTOKEN_HEADER);
            refreshToken = jwtUtil.substringToken(refreshToken);
            if (!jwtUtil.validateToken(refreshToken)) {
                filterChain.doFilter(req, res);
                return;
            }

            String userEmailFromToken = jwtUtil.getUserEmailFromToken(req, jwtUtil.REFRESHTOKEN_HEADER);
            if (!securityRedisService.getValues(userEmailFromToken).substring(JwtUtil.BEARER_PREFIX.length()).equals(refreshToken)) {
                filterChain.doFilter(req, res);
                return;
            }

        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);

            if (newAccessToken != null) {
                accessToken = newAccessToken;
                jwtUtil.addJwtToCookie(accessToken, res, "Authorization");
                accessToken = accessToken.substring(JwtUtil.BEARER_PREFIX.length());
                res.sendRedirect(req.getRequestURI());
            }
        }
        try {
            Claims info = jwtUtil.getUserInfoFromToken(accessToken);
            setAuthentication(info.getSubject());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        filterChain.doFilter(req, res);
    }

    // 인증 처리
    public void setAuthentication(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(username);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    // 인증 객체 생성
    private Authentication createAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}