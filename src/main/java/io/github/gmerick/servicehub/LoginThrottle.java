package io.github.gmerick.servicehub;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fixed-window admission control, before BCrypt. No username or password retained. */
public final class LoginThrottle extends OncePerRequestFilter {
    private record Bucket(long start, int count) {}
    private final Map<String, Bucket> clients = new HashMap<>();
    private final Clock clock;
    private final int limit, capacity, globalLimit;
    private final long windowMillis;
    private Bucket global = new Bucket(0, 0);
    public LoginThrottle(int limit, int capacity, int globalLimit, long windowMillis, Clock clock) {
        if (limit < 1 || capacity < 1 || globalLimit < 1 || windowMillis < 1) throw new IllegalArgumentException("Limites invalidos");
        this.limit=limit;this.capacity=capacity;this.globalLimit=globalLimit;this.windowMillis=windowMillis;this.clock=clock;
    }
    synchronized boolean allow(String address) {
        long now=clock.millis();
        clients.entrySet().removeIf(e -> now-e.getValue().start() >= windowMillis);
        if(now-global.start() >= windowMillis) global=new Bucket(now,0);
        if(global.count() >= globalLimit) return false;
        Bucket b=clients.get(address);
        if(b==null) {
            if(clients.size()>=capacity) return false; // Never evict an active restriction.
            b=new Bucket(now,0);
        }
        if(b.count()>=limit) return false;
        clients.put(address,new Bucket(b.start(),b.count()+1));
        global=new Bucket(global.start(),global.count()+1);
        return true;
    }
    synchronized int size() {return clients.size();}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(req.getMethod().equals("POST") && (req.getServletPath().equals("/login") || req.getServletPath().equals("/demo/visit")) && !allow(req.getRemoteAddr())) {
            res.setHeader("Retry-After",Long.toString((windowMillis+999)/1000));
            SecurityConfig.message(res,429,"Muitas tentativas. Aguarde um minuto e tente novamente.");return;
        }
        chain.doFilter(req,res);
    }
}
