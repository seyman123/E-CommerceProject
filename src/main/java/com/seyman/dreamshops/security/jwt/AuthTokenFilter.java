package com.seyman.dreamshops.security.jwt;

import com.seyman.dreamshops.security.user.ShopUserDetails;
import com.seyman.dreamshops.security.user.ShopUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final ShopUserDetailsService userDetailService;

    public AuthTokenFilter(JwtUtils jwtUtils, ShopUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        System.out.println("=== AUTH FILTER REQUEST ===");
        System.out.println("Request: " + method + " " + requestURI);
        
        String jwt = parseJwt(request);
        System.out.println("JWT token present: " + (jwt != null));
        System.out.println("JWT token (first 20 chars): " + (jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null"));

        try {
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                System.out.println("JWT token is valid");
                String username = jwtUtils.getUsernameFromToken(jwt);
                System.out.println("Username from token: " + username);
                UserDetails userDetails = userDetailService.loadUserByUsername(username);
                System.out.println("UserDetails loaded: " + (userDetails != null));
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                System.out.println("Authentication set successfully");
            } else {
                System.out.println("JWT token validation failed or token is empty");
            }
        } catch (JwtException e) {
            System.out.println("JWT Exception: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(e.getMessage() + " : Invalid or expired token, you may login and try again!");
            return;
        } catch (Exception e) {
            System.out.println("General Exception in auth filter: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getMessage());
            return;
        }

        System.out.println("Proceeding to next filter");
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
