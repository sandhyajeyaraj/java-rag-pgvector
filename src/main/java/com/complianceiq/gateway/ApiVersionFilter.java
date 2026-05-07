package com.complianceiq.gateway;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class ApiVersionFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();
        String version = resolveVersion(path);

        if (!version.equals("none")) {
            httpRes.setHeader("X-API-Version", version);
            log.debug("[Gateway] {} {} → {} handler", httpReq.getMethod(), path, version);
        }

        chain.doFilter(request, response);
    }

    private String resolveVersion(String path) {
        if (path.contains("/v1/")) return "v1";
        if (path.contains("/v2/")) return "v2";
        return "none";
    }
}
