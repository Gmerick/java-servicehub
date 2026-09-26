package io.github.gmerick.servicehub;

import static io.github.gmerick.servicehub.Model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HubService {
    private final HubRepository repo;
    public HubService(HubRepository repo) { this.repo=repo; }
    @Transactional
    public Customer createCustomer(CustomerInput input) {
        long id=repo.insert("INSERT INTO customers(name,email,phone) VALUES (?,?,?)",input.name().strip(),input.email().strip().toLowerCase(Locale.ROOT),input.phone().strip());
        return repo.customer(id);
    }
    @Transactional
    public Asset createAsset(AssetInput input) {
        repo.customer(input.customerId());
        long id=repo.insert("INSERT INTO assets(customer_id,name,serial) VALUES (?,?,?)",input.customerId(),input.name().strip(),input.serial().strip().toUpperCase(Locale.ROOT));
        return repo.asset(id);
    }
    @Transactional
    public Part createPart(PartInput input) {
        long id=repo.insert("INSERT INTO parts(name,sku,price,stock,minimum) VALUES (?,?,?,?,?)",input.name().strip(),input.sku().strip().toUpperCase(Locale.ROOT),input.price(),input.stock(),input.minimum());
        movement(id,null,input.stock(),"Estoque inicial"); return repo.part(id,false);
    }
    @Transactional
    public Part restock(long id,Restock input) {
        Part part=repo.part(id,true);
        if((long)part.stock()+input.quantity()>1000000) throw BusinessException.conflict("Limite de estoque excedido.");
        repo.jdbc().update("UPDATE parts SET stock=stock+? WHERE id=?",input.quantity(),id);
        movement(id,null,input.quantity(),input.reason().strip()); return repo.part(id,false);
    }
    @Transactional
    public Detail createOrder(OrderInput input) {
        repo.asset(input.assetId());
        long id=repo.insert("INSERT INTO work_orders(asset_id,title,description,priority,status,due_date) VALUES (?,?,?,?,'DRAFT',?)",input.assetId(),input.title().strip(),input.description().strip(),input.priority().name(),input.dueDate());
        repo.event(id,"Ordem criada em rascunho."); return detail(id);
    }
    @Transactional(readOnly=true)
    public Detail detail(long id) { return new Detail(repo.order(id),repo.items(id),repo.events(id)); }
    @Transactional
    public Detail addItem(long id,ItemInput input) {
        requireDraft(id);
        if(repo.items(id).size()>=100) throw BusinessException.conflict("Limite de 100 itens por ordem.");
        BigDecimal price=input.unitPrice(); String description=input.description().strip();
        if(input.partId()!=null) { var part=repo.part(input.partId(),false); price=part.price(); description=part.name(); }
        repo.insert("INSERT INTO order_items(order_id,part_id,description,quantity,unit_price) VALUES (?,?,?,?,?)",id,input.partId(),description,input.quantity(),price);
        repo.event(id,"Item adicionado: "+description+" ("+input.quantity()+")."); return detail(id);
    }
    @Transactional
    public Detail removeItem(long id,long itemId) {
        requireDraft(id);
        if(repo.jdbc().update("DELETE FROM order_items WHERE id=? AND order_id=?",itemId,id)!=1) throw BusinessException.missing();
        repo.event(id,"Item removido do orçamento."); return detail(id);
    }
    private void requireDraft(long id) {
        repo.lockOrder(id);
        if(repo.order(id).status()!=Status.DRAFT) throw BusinessException.conflict("O orçamento só pode ser alterado em rascunho.");
    }
    @Transactional
    public Detail transition(long id,Transition input) {
        repo.lockOrder(id);
        Order order=repo.order(id);
        Status from=order.status(),to=input.status();
        boolean allowed=switch(from) {
            case DRAFT -> to==Status.APPROVED || to==Status.CANCELED;
            case APPROVED -> to==Status.IN_PROGRESS || to==Status.CANCELED;
            case IN_PROGRESS -> to==Status.COMPLETED || to==Status.CANCELED;
            default -> false;
        };
        if(!allowed) throw BusinessException.conflict("Transição de status não permitida.");
        var items=repo.items(id);
        if(to==Status.APPROVED && items.isEmpty()) throw BusinessException.conflict("Adicione pelo menos um item antes de aprovar.");
        boolean returnStock=to==Status.CANCELED && from!=Status.DRAFT;
        if(to==Status.APPROVED || returnStock) {
            // Ordem está bloqueada; peças são bloqueadas em ordem crescente para evitar deadlock.
            Map<Long,Integer> quantities=new TreeMap<>();
            items.stream().filter(i->i.partId()!=null).forEach(i->quantities.merge(i.partId(),i.quantity(),Integer::sum));
            for(var entry:quantities.entrySet()) {
                Part part=repo.part(entry.getKey(),true);
                int delta=returnStock?entry.getValue():-entry.getValue();
                if(part.stock()+delta<0) throw BusinessException.conflict("Estoque insuficiente para "+part.name()+".");
                // Devoluções podem ultrapassar o limite de entrada manual; nunca perder peças reservadas.
                repo.jdbc().update("UPDATE parts SET stock=stock+? WHERE id=?",delta,part.id());
                movement(part.id(),id,delta,returnStock?"Cancelamento da ordem":"Aprovação do orçamento");
            }
        }
        repo.jdbc().update("UPDATE work_orders SET status=?, resolution=?, completed_at=CASE WHEN ?='COMPLETED' THEN CURRENT_TIMESTAMP ELSE completed_at END WHERE id=?",to.name(),to==Status.COMPLETED?input.note().strip():order.resolution(),to.name(),id);
        repo.event(id,from+" → "+to+": "+input.note().strip()); return detail(id);
    }
    private void movement(long partId,Long orderId,int delta,String reason) {
        repo.jdbc().update("INSERT INTO stock_movements(part_id,order_id,delta,reason) VALUES (?,?,?,?)",partId,orderId,delta,reason);
    }
    @Transactional(readOnly=true)
    public Dashboard dashboard() {
        long open=repo.jdbc().queryForObject("SELECT COUNT(*) FROM work_orders WHERE status NOT IN ('COMPLETED','CANCELED')",Long.class);
        long overdue=repo.jdbc().queryForObject("SELECT COUNT(*) FROM work_orders WHERE status NOT IN ('COMPLETED','CANCELED') AND due_date < ?",Long.class,LocalDate.now());
        long completed=repo.jdbc().queryForObject("SELECT COUNT(*) FROM work_orders WHERE status='COMPLETED'",Long.class);
        BigDecimal value=repo.jdbc().queryForObject("SELECT COALESCE(SUM(i.quantity*i.unit_price),0) FROM order_items i JOIN work_orders o ON o.id=i.order_id WHERE o.status='COMPLETED'",BigDecimal.class);
        long low=repo.jdbc().queryForObject("SELECT COUNT(*) FROM parts WHERE stock<=minimum",Long.class);
        return new Dashboard(open,overdue,completed,value,low,repo.orders(null,"",0,5).items());
    }
}
