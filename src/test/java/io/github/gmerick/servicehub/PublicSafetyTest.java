package io.github.gmerick.servicehub;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.web.*;
class PublicSafetyTest {
 @Test void publicModeRejectsHttpExceptHealth() throws Exception {
  var filter=new LocalApiFilter();filter.requireHttps=true;
  for(String path:new String[]{"/login","/login.html","/api/csrf","/api/customers","/demo/visit"}) {
   var request=new MockHttpServletRequest("GET",path);var response=new MockHttpServletResponse();
   filter.doFilter(request,response,(q,s)->fail("HTTP should be rejected"));assertEquals(403,response.getStatus());
  }
  var req=new MockHttpServletRequest("GET","/api/health");var res=new MockHttpServletResponse();
  filter.doFilter(req,res,(q,s)->s.getWriter().write("health"));assertEquals("health",res.getContentAsString());
  req=new MockHttpServletRequest("GET","/api/csrf");req.setSecure(true);res=new MockHttpServletResponse();
  filter.doFilter(req,res,(q,s)->s.getWriter().write("https"));assertEquals("https",res.getContentAsString());
 }
 @Test void demoCannotExposeOriginalDatabase(){
  var guard=new DemoSafety();guard.demo=true;guard.url="jdbc:h2:file:/var/lib/servicehub/servicehub;WRITE_DELAY=0";
  assertThrows(IllegalStateException.class,guard::afterPropertiesSet);
  guard.url="jdbc:h2:file:/var/lib/servicehub-demo/servicehub;WRITE_DELAY=0";guard.https=true;
  assertThrows(IllegalStateException.class,guard::afterPropertiesSet);guard.secure=true;assertDoesNotThrow(guard::afterPropertiesSet);
 }
}
