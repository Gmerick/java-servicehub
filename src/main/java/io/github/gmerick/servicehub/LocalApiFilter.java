package io.github.gmerick.servicehub;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Defense in depth: CSP and rejection of cross-site API mutations. */
@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
public class LocalApiFilter extends OncePerRequestFilter {
    @org.springframework.beans.factory.annotation.Value("${app.require-https:false}") boolean requireHttps;
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(requireHttps && !req.isSecure() && !req.getRequestURI().equals("/api/health")) {
            SecurityConfig.message(res,403,"Use o endereço HTTPS da demonstração.");return;
        }
        res.setHeader("X-Content-Type-Options","nosniff");
        res.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        if(req.getRequestURI().startsWith("/api")) {
            res.setHeader("Cache-Control","no-store");
            if(!req.getMethod().equals("GET") && !req.getMethod().equals("HEAD")) {
                String origin=req.getHeader("Origin");
                String expected=req.getScheme()+"://"+req.getHeader("Host");
                if("cross-site".equals(req.getHeader("Sec-Fetch-Site")) || (origin!=null && !origin.equals(expected))) {
                    res.sendError(403);return;
                }
                if(!req.getMethod().equals("DELETE") && (req.getContentType()==null || !req.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json"))) { res.sendError(415);return; }
            }
        }
        chain.doFilter(req,res);
    }
}
