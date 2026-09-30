package io.github.gmerick.servicehub;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1","app.demo=true","app.public-demo=true","app.login.max-attempts=100","app.session.seconds=2"})
class SecurityTest extends HttpTestSupport {
  @Override boolean autoLogin(){return false;}
  @Test void anonymousCannotReadOrWrite() throws Exception {
    for(String path:new String[]{"/api/customers","/api/assets","/api/parts","/api/orders","/api/dashboard","/api/orders/1"}) assertEquals(401,raw("GET",path,null,null,null).status());
    assertEquals(401,call("POST","/api/customers","{}").status());
  }
  @Test void invalidAndValidLogin() throws Exception {
    assertEquals(401,login("incorrect").status());
    assertEquals(200,login(PASSWORD).status());
    assertTrue(call("GET","/api/session",null).body().get("admin").asBoolean());
  }
  @Test void visitorReadsButEveryMutationIsDenied() throws Exception {
    assertEquals(200,raw("POST","/demo/visit",null,null,csrf()).status());
    assertFalse(call("GET","/api/session",null).body().get("admin").asBoolean());
    for(String path:new String[]{"/api/customers","/api/assets","/api/parts","/api/orders","/api/dashboard","/api/orders/1","/api/parts/1/movements"}) assertEquals(200,call("GET",path,null).status());
    for(String path:new String[]{"/api/customers","/api/assets","/api/parts","/api/orders","/api/parts/1/restock","/api/orders/1/items","/api/orders/1/status"}) assertEquals(403,call("POST",path,"{}").status());
    assertEquals(403,call("DELETE","/api/orders/1/items/1",null).status());
    assertEquals(403,call("PUT","/api/customers/1","{}").status());
    assertEquals(403,call("PATCH","/api/orders/1","{}").status());
  }
  @Test void csrfRequiredForLoginLogoutAndWrites() throws Exception {
    assertEquals(403,raw("POST","/login","username=x&password=x","application/x-www-form-urlencoded",null).status());
    assertEquals(403,raw("POST","/demo/visit",null,null,null).status());
    String beforeLogin=csrf();assertEquals(200,login(PASSWORD).status());
    assertEquals(403,raw("POST","/api/customers","{}","application/json",beforeLogin).status());
    assertEquals(403,raw("POST","/api/customers","{}","application/json",null).status());
    assertEquals(403,raw("POST","/logout",null,null,null).status());
    assertEquals(200,raw("POST","/logout",null,null,csrf()).status());
    assertEquals(401,raw("GET","/api/customers",null,null,null).status());
  }
  @Test void expiredSessionDoesNotGrantAccess() throws Exception {
    assertEquals(200,login(PASSWORD).status());Thread.sleep(2200);
    assertEquals(401,raw("GET","/api/customers",null,null,null).status());
  }
  @Test void sessionCookieIsHttpOnlyAndSameSite() throws Exception {
    var cookie=raw("GET","/api/csrf",null,null,null).headers().firstValue("Set-Cookie").orElseThrow();
    assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("SameSite=Lax"));
  }
}
