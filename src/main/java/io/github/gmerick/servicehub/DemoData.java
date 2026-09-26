package io.github.gmerick.servicehub;

import static io.github.gmerick.servicehub.Model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name="app.demo",havingValue="true")
public class DemoData implements CommandLineRunner {
    private final HubService service; private final HubRepository repo;
    public DemoData(HubService service,HubRepository repo) {this.service=service;this.repo=repo;}
    @Override @Transactional public void run(String... args) {
        if(!repo.customers().isEmpty()) return;
        var c=service.createCustomer(new CustomerInput("Estúdio Aurora (demo)","aurora@example.com","11900000001"));
        var c2=service.createCustomer(new CustomerInput("Clínica Horizonte (demo)","horizonte@example.com","11900000002"));
        var a=service.createAsset(new AssetInput(c.id(),"Notebook Latitude","DEMO-NB-01"));
        var a2=service.createAsset(new AssetInput(c2.id(),"Estação de atendimento","DEMO-PC-02"));
        var p=service.createPart(new PartInput("SSD 480 GB","SSD-480",new BigDecimal("249.90"),8,3));
        service.createPart(new PartInput("Memória DDR4 8 GB","RAM-8",new BigDecimal("139.90"),2,2));
        service.createPart(new PartInput("Fonte ATX 500 W","PSU-500",new BigDecimal("229.00"),6,2));
        var o=service.createOrder(new OrderInput(a.id(),"Upgrade de armazenamento","Lentidão ao iniciar aplicativos. Avaliar troca do disco.",Priority.HIGH,LocalDate.now().plusDays(2)));
        service.addItem(o.order().id(),new ItemInput(p.id(),"SSD",1,BigDecimal.ZERO));
        service.addItem(o.order().id(),new ItemInput(null,"Instalação e migração",1,new BigDecimal("150.00")));
        var second=service.createOrder(new OrderInput(a2.id(),"Manutenção preventiva","Limpeza e revisão da estação.",Priority.NORMAL,LocalDate.now().plusDays(4)));
        service.addItem(second.order().id(),new ItemInput(null,"Revisão preventiva",1,new BigDecimal("180.00")));
        service.transition(second.order().id(),new Transition(Status.APPROVED,"Orçamento demonstrativo aprovado."));
        service.transition(second.order().id(),new Transition(Status.IN_PROGRESS,"Técnico iniciou a revisão."));
        var done=service.createOrder(new OrderInput(a.id(),"Diagnóstico de rede","Analisar conexão intermitente.",Priority.LOW,LocalDate.now()));
        service.addItem(done.order().id(),new ItemInput(null,"Diagnóstico",1,new BigDecimal("120.00")));
        service.transition(done.order().id(),new Transition(Status.APPROVED,"Aprovado para demonstração."));
        service.transition(done.order().id(),new Transition(Status.IN_PROGRESS,"Diagnóstico iniciado."));
        service.transition(done.order().id(),new Transition(Status.COMPLETED,"Conector substituído e testes de conexão concluídos."));
    }
}
