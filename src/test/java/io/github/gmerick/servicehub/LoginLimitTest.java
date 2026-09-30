package io.github.gmerick.servicehub;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:limit","app.demo=false","app.login.max-attempts=2"})
class LoginLimitTest extends HttpTestSupport {
 @Override boolean autoLogin(){return false;}
 @Test void actualHttpLoginIsThrottled() throws Exception {assertEquals(401,login("wrong").status());assertEquals(401,login("wrong").status());var r=login(PASSWORD);assertEquals(429,r.status());assertEquals("60",r.headers().firstValue("Retry-After").orElse(""));}
}
