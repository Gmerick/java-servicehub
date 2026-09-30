package io.github.gmerick.servicehub;

import java.util.Map;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class AuthController {
    @Value("${app.public-demo:false}") boolean demo;
    @Value("${app.session.seconds:1800}") int sessionSeconds;
    @GetMapping("/api/csrf") public Map<String,Object> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName(),"demo",demo);
    }
    @GetMapping("/api/session") public Map<String,Object> session(Authentication auth) {
        return Map.of("username",auth.getName(),"admin",auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")),"demo",demo);
    }
    @PostMapping("/demo/visit") public Map<String,String> visit(HttpServletRequest req,HttpServletResponse res) {
        if(!demo) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        req.getSession();req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("Visitante",null,AuthorityUtils.createAuthorityList("ROLE_VISITANTE")));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,req,res);
        new HttpSessionCsrfTokenRepository().saveToken(null,req,res);
        req.getSession().setMaxInactiveInterval(sessionSeconds);
        return Map.of("message","Consulta demonstrativa iniciada.");
    }
}
