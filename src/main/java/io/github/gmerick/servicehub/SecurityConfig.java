package io.github.gmerick.servicehub;

import java.io.IOException;
import java.time.Clock;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    static void message(HttpServletResponse response,int status,String message) throws IOException {
        response.setStatus(status);response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\""+message+"\"}");
    }
    @Bean UserDetailsService users(@Value("${app.admin.username:}") String username,@Value("${app.admin.password-hash:}") String hash) {
        if(username.isBlank() || username.length()>80 || !hash.matches("\\$2[aby]\\$(1[0-6])\\$[./A-Za-z0-9]{53}"))
            throw new IllegalStateException("Configure o usuario administrativo e um hash BCrypt (custo 10 a 16) fora do Git.");
        return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).roles("ADMIN").build());
    }
    @Bean BCryptPasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
    @Bean SecurityFilterChain security(HttpSecurity http,
            @Value("${app.login.max-attempts:8}") int attempts,
            @Value("${app.session.seconds:1800}") int sessionSeconds) throws Exception {
        http.csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html","/login.js","/style.css","/login","/api/csrf","/api/health","/demo/visit","/error").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/**").hasAnyRole("ADMIN","VISITANTE")
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,e)->{
                    if(req.getServletPath().startsWith("/api/")) message(res,401,"Sessão encerrada. Entre novamente.");
                    else res.sendRedirect("/login.html");
                })
                .accessDeniedHandler((req,res,e)->message(res,403,"Ação não autorizada ou token de segurança expirado. Atualize a página.")))
            .formLogin(login -> login.loginPage("/login.html").loginProcessingUrl("/login")
                .successHandler((req,res,auth)->{req.getSession().setMaxInactiveInterval(sessionSeconds);message(res,200,"Login realizado.");})
                .failureHandler((req,res,e)->message(res,401,"Usuário ou senha inválidos.")))
            .logout(logout -> logout.logoutUrl("/logout").invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->message(res,200,"Sessão encerrada.")))
            .requestCache(cache -> cache.disable())
            .addFilterBefore(new LoginThrottle(attempts,2048,Math.max(60,attempts),60000,Clock.systemUTC()),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
