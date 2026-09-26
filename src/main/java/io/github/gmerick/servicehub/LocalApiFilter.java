package io.github.gmerick.servicehub;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** A aplicação não tem autenticação: execução local e bloqueio de mutações cross-site. */
@Component
public class LocalApiFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
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
