package com.example.carmanage_service.controller;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Cho phep quan sat instance xu ly yeu cau, khong doi JSON nghiep vu. */
@Component
public class InstanceHeaderFilter extends OncePerRequestFilter {
    private final String instanceId;

    public InstanceHeaderFilter(@Value("${eureka.instance.instance-id}") String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Service-Instance", instanceId);
        chain.doFilter(request, response);
    }
}
