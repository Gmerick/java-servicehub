package io.github.gmerick.servicehub;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.concurrent.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","app.demo=false"})
class ServiceHubTest extends HttpTestSupport {
 @Autowired JdbcTemplate db;
 @BeforeEach void clear() {
  for(String table:new String[]{"stock_movements","order_events","order_items","work_orders","parts","assets","customers"}) db.update("DELETE FROM "+table);
 }
 long customer() throws Exception {var r=call("POST","/api/customers","{\"name\":\"Cliente teste\",\"email\":\"teste@example.com\",\"phone\":\"11900000000\"}");assertEquals(201,r.status());return r.body().get("id").asLong();}
 long asset() throws Exception {return call("POST","/api/assets","{\"customerId\":"+customer()+",\"name\":\"Notebook\",\"serial\":\"SER-1\"}").body().get("id").asLong();}
 long order(long asset,String title) throws Exception {var r=call("POST","/api/orders","{\"assetId\":"+asset+",\"title\":\""+title+"\",\"description\":\"Diagnostico\",\"priority\":\"HIGH\",\"dueDate\":\""+LocalDate.now().plusDays(2)+"\"}");assertEquals(201,r.status());return r.body().get("order").get("id").asLong();}
 long part(String sku,int stock) throws Exception {var r=call("POST","/api/parts","{\"name\":\"Peca "+sku+"\",\"sku\":\""+sku+"\",\"price\":10.10,\"stock\":"+stock+",\"minimum\":2}");assertEquals(201,r.status());return r.body().get("id").asLong();}
 Result item(long o,Long p,int quantity,String price) throws Exception {return call("POST","/api/orders/"+o+"/items","{\"partId\":"+p+",\"description\":\"Servico\",\"quantity\":"+quantity+",\"unitPrice\":"+price+"}");}
 Result state(long o,String status) throws Exception {return call("POST","/api/orders/"+o+"/status","{\"status\":\""+status+"\",\"note\":\"Observacao de teste\"}");}
 int stock(long p){return db.queryForObject("SELECT stock FROM parts WHERE id=?",Integer.class,p);}
 @Test void completeLifecycleWithExactBudgetAndHistory() throws Exception {
  long o=order(asset(),"Troca de disco"),p=part("P1",10);
  assertEquals(200,item(o,p,3,"0.01").status()); // preço do catálogo prevalece
  var budget=item(o,null,2,"0.10");assertEquals(0,new java.math.BigDecimal("30.50").compareTo(new java.math.BigDecimal(budget.body().get("order").get("total").asText())));
  assertEquals(200,state(o,"APPROVED").status());assertEquals(7,stock(p));
  assertEquals(200,state(o,"IN_PROGRESS").status());var done=state(o,"COMPLETED");assertEquals(200,done.status());
  assertEquals("Observacao de teste",done.body().get("order").get("resolution").asText());
  assertFalse(done.body().get("order").get("completedAt").isNull());assertEquals(6,done.body().get("events").size());
  assertEquals(409,item(o,null,1,"10.00").status());assertEquals(409,state(o,"CANCELED").status());
 }
 @Test void insufficientStockRollsBackEveryPartAndEvent() throws Exception {
  long o=order(asset(),"Rollback"),p1=part("A",5),p2=part("B",0);
  item(o,p1,2,"1");item(o,p2,1,"1");
  int events=db.queryForObject("SELECT COUNT(*) FROM order_events",Integer.class);
  assertEquals(409,state(o,"APPROVED").status());assertEquals(5,stock(p1));assertEquals(0,stock(p2));
  assertEquals(events,db.queryForObject("SELECT COUNT(*) FROM order_events",Integer.class));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE order_id=?",Integer.class,o));
  assertEquals("DRAFT",call("GET","/api/orders/"+o,null).body().get("order").get("status").asText());
 }
 @Test void cancellationReturnsOnceEvenWithDuplicatePartLines() throws Exception {
  long o=order(asset(),"Cancelamento"),p=part("A",7);item(o,p,2,"1");item(o,p,1,"1");
  state(o,"APPROVED");assertEquals(4,stock(p));state(o,"IN_PROGRESS");state(o,"CANCELED");
  assertEquals(7,stock(p));assertEquals(409,state(o,"CANCELED").status());assertEquals(7,stock(p));
 }
 @Test void concurrentOrdersCannotOversell() throws Exception {
  long a=asset(),o1=order(a,"Um"),o2=order(a,"Dois"),p=part("A",1);item(o1,p,1,"1");item(o2,p,1,"1");
  var executor=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try {var f1=executor.submit(()->{gate.await();return state(o1,"APPROVED").status();});var f2=executor.submit(()->{gate.await();return state(o2,"APPROVED").status();});gate.countDown();
   var results=java.util.List.of(f1.get(15,TimeUnit.SECONDS),f2.get(15,TimeUnit.SECONDS));assertTrue(results.contains(200));assertTrue(results.contains(409));assertEquals(0,stock(p));
  }finally{executor.shutdownNow();}
 }
 @Test void concurrentDuplicateApprovalOnlyWithdrawsOnce() throws Exception {
  long o=order(asset(),"Unica"),p=part("A",5);item(o,p,2,"1");
  var executor=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try {var f1=executor.submit(()->{gate.await();return state(o,"APPROVED").status();});var f2=executor.submit(()->{gate.await();return state(o,"APPROVED").status();});gate.countDown();
   var results=java.util.List.of(f1.get(15,TimeUnit.SECONDS),f2.get(15,TimeUnit.SECONDS));assertTrue(results.contains(200));assertTrue(results.contains(409));assertEquals(3,stock(p));
  }finally{executor.shutdownNow();}
 }
 @Test void validatesInputAndReferences() throws Exception {
  assertEquals(400,call("POST","/api/customers","{\"name\":\" \",\"email\":\"invalid\",\"phone\":\"x\"}").status());
  assertEquals(404,call("POST","/api/assets","{\"customerId\":999999,\"name\":\"PC\",\"serial\":\"X\"}").status());
  assertEquals(404,call("GET","/api/orders/999999",null).status());
  assertEquals(400,call("GET","/api/orders?size=0",null).status());
  assertEquals(400,call("GET","/api/orders?status=WRONG",null).status());
  long o=order(asset(),"Valida");assertEquals(400,item(o,null,0,"1").status());assertEquals(400,item(o,null,1,"-1").status());assertEquals(400,item(o,null,1,"0.001").status());
 }
 @Test void normalizesUniqueEmailAndSerial() throws Exception {
  long a=asset();assertTrue(a>0);
  assertEquals(409,call("POST","/api/customers","{\"name\":\"Duplicado\",\"email\":\"TESTE@example.com\",\"phone\":\"11\"}").status());
  long c=db.queryForObject("SELECT id FROM customers",Long.class);
  assertEquals(409,call("POST","/api/assets","{\"customerId\":"+c+",\"name\":\"Outro\",\"serial\":\"ser-1\"}").status());
 }
 @Test void transitionsAndItemOwnershipAreEnforced() throws Exception {
  long a=asset(),o=order(a,"Original"),other=order(a,"Outro");assertEquals(409,state(o,"APPROVED").status());assertEquals(409,state(o,"COMPLETED").status());
  var r=item(o,null,1,"100");long i=r.body().get("items").get(0).get("id").asLong();
  assertEquals(404,call("DELETE","/api/orders/"+other+"/items/"+i,null).status());
  assertEquals(200,call("DELETE","/api/orders/"+o+"/items/"+i,null).status());
  assertEquals(0,call("GET","/api/orders/"+o,null).body().get("items").size());
 }
 @Test void draftCancellationNeverReturnsUnreservedStock() throws Exception {
  long o=order(asset(),"Rascunho"),p=part("A",5);item(o,p,2,"1");assertEquals(200,state(o,"CANCELED").status());assertEquals(5,stock(p));
 }
 @Test void approvedBudgetIsImmutable() throws Exception {
  long o=order(asset(),"Imutavel");long i=item(o,null,1,"90").body().get("items").get(0).get("id").asLong();state(o,"APPROVED");
  assertEquals(409,item(o,null,1,"100").status());assertEquals(409,call("DELETE","/api/orders/"+o+"/items/"+i,null).status());
 }
 @Test void dashboardFiltersAndPagination() throws Exception {
  long a=asset(),o=order(a,"Monitor");order(a,"Teclado");item(o,null,1,"150.25");state(o,"APPROVED");state(o,"IN_PROGRESS");state(o,"COMPLETED");
  var dashboard=call("GET","/api/dashboard",null).body();assertEquals(1,dashboard.get("open").asInt());assertEquals(1,dashboard.get("completed").asInt());assertEquals("150.25",dashboard.get("completedValue").asText());
  assertEquals(1,call("GET","/api/orders?search=monitor&status=COMPLETED",null).body().get("total").asInt());
  assertEquals(0,call("GET","/api/orders?search=monitor&status=DRAFT",null).body().get("total").asInt());
  assertEquals(1,call("GET","/api/orders?size=1&page=1",null).body().get("items").size());
 }
 @Test void restockIsRecordedAndValidated() throws Exception {
  long p=part("A",0);assertEquals(200,call("POST","/api/parts/"+p+"/restock","{\"quantity\":5,\"reason\":\"Compra\"}").status());assertEquals(5,stock(p));
  assertEquals(400,call("POST","/api/parts/"+p+"/restock","{\"quantity\":-2,\"reason\":\"X\"}").status());
  assertEquals(2,call("GET","/api/parts/"+p+"/movements",null).body().size());
 }
 @Test void csvEscapesFormulaAndQuotes() throws Exception {
  order(asset(),"=2+2");var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/orders.csv")).GET().build();var res=client.send(request,HttpResponse.BodyHandlers.ofString());
  assertEquals(200,res.statusCode());assertTrue(res.body().contains("\"'=2+2\""));assertTrue(res.headers().firstValue("Content-Disposition").orElse("").contains(".csv"));
  assertEquals("\"'  @SUM(1)\"",HubController.cell("  @SUM(1)"));assertEquals("\"a\"\"b\"",HubController.cell("a\"b"));
 }
 @Test void crossSiteMutationRejected() throws Exception {
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/customers")).header("Origin","https://example.com").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
  assertEquals(403,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
  assertEquals("nosniff",call("GET","/api/health",null).headers().firstValue("X-Content-Type-Options").orElse(""));
 }
}
