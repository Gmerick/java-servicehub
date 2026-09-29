package io.github.gmerick.servicehub;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

abstract class HttpTestSupport {
  static final String USER="integration-admin", PASSWORD=UUID.randomUUID().toString();
  static final String HASH=new BCryptPasswordEncoder(10).encode(PASSWORD);
  @DynamicPropertySource static void credentials(DynamicPropertyRegistry r) throws Exception {
    r.add("app.admin.username",()->USER);r.add("app.admin.password-hash",()->HASH);
    Files.createDirectories(Path.of("target"));
    Files.writeString(Path.of("target/test-auth.json"),new JsonMapper().writeValueAsString(java.util.Map.of("username",USER,"password",PASSWORD,"hash",HASH)));
  }
  @LocalServerPort int port;
  HttpClient client;
  final JsonMapper json=new JsonMapper();
  boolean autoLogin() {return true;}
  @BeforeEach void authenticate() throws Exception {
    client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();
    if(autoLogin()) login(PASSWORD);
  }
  record Result(int status,JsonNode body,HttpHeaders headers) {}
  Result raw(String method,String path,String payload,String contentType,String token) throws Exception {
    var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10));
    if(contentType!=null)builder.header("Content-Type",contentType);
    if(token!=null)builder.header("X-CSRF-TOKEN",token);
    var response=client.send(builder.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(payload)).build(),HttpResponse.BodyHandlers.ofString());
    return new Result(response.statusCode(),response.body().isBlank()?null:json.readTree(response.body()),response.headers());
  }
  String csrf() throws Exception {return raw("GET","/api/csrf",null,null,null).body().get("token").asText();}
  Result login(String password) throws Exception {return raw("POST","/login","username="+USER+"&password="+URLEncoder.encode(password,java.nio.charset.StandardCharsets.UTF_8),"application/x-www-form-urlencoded",csrf());}
  Result call(String method,String path,String payload) throws Exception {
    return raw(method,path,payload,"application/json",method.equals("GET")?null:csrf());
  }
}
