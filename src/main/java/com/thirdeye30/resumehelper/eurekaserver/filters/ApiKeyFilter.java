package com.thirdeye30.resumehelper.eurekaserver.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ApiKeyFilter extends OncePerRequestFilter {

    private String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String requestApiKey = null;

        // Extract credentials if using standard Basic Authentication
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring(6).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                
                // Format is username:password -> "thirdeye:yourApiKey"
                String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    requestApiKey = values[1]; 
                }
            } catch (IllegalArgumentException e) {
                // Handle extraction or malformed base64 parsing issues cleanly
            }
        }

        // Bypass security parsing logic for diagnostic status routes if necessary
        String path = request.getRequestURI();
        if (path.contains("/api/statuschecker")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate token matches
        if (apiKey != null && apiKey.equals(requestApiKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            errorResponse.put("message", "Invalid or Missing Service Discovery Token");
            errorResponse.put("data", null);

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}