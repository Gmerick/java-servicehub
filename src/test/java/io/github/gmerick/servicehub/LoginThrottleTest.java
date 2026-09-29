package io.github.gmerick.servicehub;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
class LoginThrottleTest {
 @Test void boundedAndExpiresWithoutEvictingActiveRestrictions(){
  class Time extends Clock {long time=1000; public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return Instant.ofEpochMilli(time);}}
  var time=new Time();var limiter=new LoginThrottle(2,2,10,1000,time);
  assertTrue(limiter.allow("a"));assertTrue(limiter.allow("a"));assertFalse(limiter.allow("a"));
  assertTrue(limiter.allow("b"));for(int i=0;i<10000;i++)assertFalse(limiter.allow("other"+i));assertEquals(2,limiter.size());
  time.time+=1000;assertTrue(limiter.allow("c"));assertEquals(1,limiter.size());
 }
 @Test void globalCeiling(){var limiter=new LoginThrottle(10,10,2,60000,Clock.systemUTC());assertTrue(limiter.allow("a"));assertTrue(limiter.allow("b"));assertFalse(limiter.allow("c"));}
}
