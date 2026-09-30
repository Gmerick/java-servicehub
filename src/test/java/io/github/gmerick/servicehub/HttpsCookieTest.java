package io.github.gmerick.servicehub;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:securecookie","app.demo=false","server.servlet.session.cookie.secure=true"})
class HttpsCookieTest extends HttpTestSupport {
 @Override boolean autoLogin(){return false;}
 @Test void secureCookieOnConfiguredServer() throws Exception {var value=raw("GET","/api/csrf",null,null,null).headers().firstValue("Set-Cookie").orElseThrow();assertTrue(value.contains("Secure"));assertTrue(value.contains("HttpOnly"));assertTrue(value.contains("SameSite=Lax"));}
}
