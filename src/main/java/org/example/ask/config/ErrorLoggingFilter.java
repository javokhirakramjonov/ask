package org.example.ask.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Error-handler middleware: sits at the outermost layer of the filter chain,
 * logs every request/response pair, and captures any unhandled exception that
 * bubbles up through the chain so it is always visible in the terminal.
 */
@Slf4j
@Component
@Order(1)   // run before all other filters
public class ErrorLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri    = request.getRequestURI();
        String query  = request.getQueryString();
        String remote = request.getRemoteAddr();

        log.info("--> {} {}{} [{}]", method, uri,
                query != null ? "?" + query : "",
                remote);

        try {
            filterChain.doFilter(request, response);

            long elapsed = System.currentTimeMillis() - start;
            int  status  = response.getStatus();

            if (status >= 500) {
                log.error("<-- {} {} {} ({}ms)", method, uri, status, elapsed);
            } else if (status >= 400) {
                log.warn("<-- {} {} {} ({}ms)", method, uri, status, elapsed);
            } else {
                log.info("<-- {} {} {} ({}ms)", method, uri, status, elapsed);
            }

        } catch (ServletException | IOException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("<-- {} {} FAILED after {}ms — {}: {}",
                    method, uri, elapsed,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("<-- {} {} FAILED after {}ms — {}: {}",
                    method, uri, elapsed,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw new ServletException("Unhandled exception in filter chain", ex);
        }
    }
}

