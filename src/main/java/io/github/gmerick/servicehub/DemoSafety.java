package io.github.gmerick.servicehub;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.env.Environment;

/** Refuse accidental exposure/seeding of the pre-existing database. */
@Configuration
public class DemoSafety implements InitializingBean {
    // Validate before DataSource/schema initialization, not only after opening H2.
    @Bean static BeanFactoryPostProcessor validateDemoBeforeDatabase(Environment env) {
        return factory -> {
            var guard = new DemoSafety();
            guard.demo = env.getProperty("app.public-demo", Boolean.class, false);
            guard.https = env.getProperty("app.require-https", Boolean.class, false);
            guard.secure = env.getProperty("server.servlet.session.cookie.secure", Boolean.class, false);
            guard.url = env.getProperty("spring.datasource.url", "");
            guard.afterPropertiesSet();
        };
    }
    @Value("${app.public-demo:false}") boolean demo;
    @Value("${app.require-https:false}") boolean https;
    @Value("${spring.datasource.url}") String url;
    @Value("${server.servlet.session.cookie.secure:false}") boolean secure;
    @Override public void afterPropertiesSet() {
        if(demo && !(url.equals("jdbc:h2:file:./data-demo/servicehub;WRITE_DELAY=0") || url.equals("jdbc:h2:file:/var/lib/servicehub-demo/servicehub;WRITE_DELAY=0") || (!https && url.startsWith("jdbc:h2:mem:"))))
            throw new IllegalStateException("Demonstração exige banco separado em data-demo ou /var/lib/servicehub-demo.");
        if(https && (!demo || !secure || !url.equals("jdbc:h2:file:/var/lib/servicehub-demo/servicehub;WRITE_DELAY=0")))
            throw new IllegalStateException("Publicação exige demo isolada e cookie Secure.");
    }
}
