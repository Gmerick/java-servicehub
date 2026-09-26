package io.github.gmerick.servicehub;

import static io.github.gmerick.servicehub.Model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class HubRepository {
    private final JdbcTemplate db;
    public JdbcTemplate jdbc() { return db; }
    public HubRepository(JdbcTemplate db) { this.db = db; }
    long insert(String sql, Object... args) {
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i=0; i<args.length; i++) statement.setObject(i+1, args[i]);
            return statement;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    static final RowMapper<Customer> CUSTOMER = (r,n) -> new Customer(r.getLong("id"),r.getString("name"),r.getString("email"),r.getString("phone"));
    static final RowMapper<Asset> ASSET = (r,n) -> new Asset(r.getLong("id"),r.getLong("customer_id"),r.getString("customer_name"),r.getString("name"),r.getString("serial"));
    static final RowMapper<Part> PART = (r,n) -> new Part(r.getLong("id"),r.getString("name"),r.getString("sku"),r.getBigDecimal("price"),r.getInt("stock"),r.getInt("minimum"));
    static final RowMapper<Order> ORDER = (r,n) -> new Order(r.getLong("id"),r.getLong("asset_id"),r.getString("asset_name"),r.getString("customer_name"),r.getString("title"),r.getString("description"),Priority.valueOf(r.getString("priority")),Status.valueOf(r.getString("status")),r.getTimestamp("created_at").toLocalDateTime(),r.getDate("due_date").toLocalDate(),r.getTimestamp("completed_at")==null?null:r.getTimestamp("completed_at").toLocalDateTime(),r.getString("resolution"),r.getBigDecimal("total"));
    static final String ORDER_SELECT = """
        SELECT o.*, a.name asset_name, c.name customer_name,
        COALESCE((SELECT SUM(i.quantity*i.unit_price) FROM order_items i WHERE i.order_id=o.id),0) total
        FROM work_orders o JOIN assets a ON a.id=o.asset_id JOIN customers c ON c.id=a.customer_id
        """;
    public List<Customer> customers() { return db.query("SELECT * FROM customers ORDER BY name,id",CUSTOMER); }
    public List<Asset> assets() { return db.query("SELECT a.*, c.name customer_name FROM assets a JOIN customers c ON c.id=a.customer_id ORDER BY a.id DESC",ASSET); }
    public List<Part> parts() { return db.query("SELECT * FROM parts ORDER BY name,id",PART); }
    public Customer customer(long id) { return one(db.query("SELECT * FROM customers WHERE id=?",CUSTOMER,id)); }
    public Asset asset(long id) { return one(db.query("SELECT a.*, c.name customer_name FROM assets a JOIN customers c ON c.id=a.customer_id WHERE a.id=?",ASSET,id)); }
    public Part part(long id, boolean lock) { return one(db.query("SELECT * FROM parts WHERE id=?"+(lock?" FOR UPDATE":""),PART,id)); }
    public Order order(long id) { return one(db.query(ORDER_SELECT+" WHERE o.id=?",ORDER,id)); }
    public void lockOrder(long id) {
        one(db.query("SELECT id FROM work_orders WHERE id=? FOR UPDATE",(r,n)->r.getLong(1),id));
    }
    public List<Item> items(long orderId) {
        return db.query("SELECT * FROM order_items WHERE order_id=? ORDER BY id",(r,n)-> {
            Long part = r.getObject("part_id",Long.class);
            var price=r.getBigDecimal("unit_price"); var quantity=r.getInt("quantity");
            return new Item(r.getLong("id"),part,r.getString("description"),quantity,price,price.multiply(BigDecimal.valueOf(quantity)));
        },orderId);
    }
    public List<Event> events(long id) { return db.query("SELECT * FROM order_events WHERE order_id=? ORDER BY id DESC",(r,n)->new Event(r.getLong("id"),r.getTimestamp("created_at").toLocalDateTime(),r.getString("message")),id); }
    public void event(long id,String message) { db.update("INSERT INTO order_events(order_id,message) VALUES (?,?)",id,message); }
    public Page orders(Status status,String search,int page,int size) {
        if(page<0 || page>100000 || size<1 || size>100 || search.length()>140) throw new BusinessException(400,"Paginação ou busca inválida.");
        var args=new ArrayList<Object>();
        String filter=" WHERE 1=1";
        if(status!=null) { filter+=" AND o.status=?";args.add(status.name()); }
        if(!search.isBlank()) { filter+=" AND (LOCATE(?,LOWER(o.title))>0 OR LOCATE(?,LOWER(c.name))>0 OR CAST(o.id AS VARCHAR)=?)";args.add(search.toLowerCase(Locale.ROOT));args.add(search.toLowerCase(Locale.ROOT));args.add(search); }
        long total=db.queryForObject("SELECT COUNT(*) FROM work_orders o JOIN assets a ON a.id=o.asset_id JOIN customers c ON c.id=a.customer_id"+filter,Long.class,args.toArray());
        args.add(size); args.add(page*size);
        return new Page(db.query(ORDER_SELECT+filter+" ORDER BY o.id DESC LIMIT ? OFFSET ?",ORDER,args.toArray()),total,page,size);
    }
    static <T> T one(List<T> rows) { if(rows.isEmpty()) throw BusinessException.missing(); return rows.get(0); }
}
