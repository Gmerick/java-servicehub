package io.github.gmerick.servicehub;

import static io.github.gmerick.servicehub.Model.*;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HubController {
    private final HubService service;
    private final HubRepository repo;
    private final BuildProperties build;
    public HubController(HubService service,HubRepository repo,BuildProperties build) { this.service=service;this.repo=repo;this.build=build; }
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status","UP","version",build.getVersion()); }
    @GetMapping("/customers") public List<Customer> customers() { return repo.customers(); }
    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED) public Customer customer(@Valid @RequestBody CustomerInput input) { return service.createCustomer(input); }
    @GetMapping("/assets") public List<Asset> assets() { return repo.assets(); }
    @PostMapping("/assets") @ResponseStatus(HttpStatus.CREATED) public Asset asset(@Valid @RequestBody AssetInput input) { return service.createAsset(input); }
    @GetMapping("/parts") public List<Part> parts() { return repo.parts(); }
    @PostMapping("/parts") @ResponseStatus(HttpStatus.CREATED) public Part part(@Valid @RequestBody PartInput input) { return service.createPart(input); }
    @PostMapping("/parts/{id}/restock") public Part restock(@PathVariable long id,@Valid @RequestBody Restock input) { return service.restock(id,input); }
    @GetMapping("/parts/{id}/movements") public List<Map<String,Object>> movements(@PathVariable long id) { repo.part(id,false);return repo.jdbc().queryForList("SELECT id,delta,reason,order_id,created_at FROM stock_movements WHERE part_id=? ORDER BY id DESC",id); }
    @GetMapping("/orders") public Page orders(@RequestParam(required=false) Status status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) { return repo.orders(status,search.strip(),page,size); }
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) public Detail order(@Valid @RequestBody OrderInput input) { return service.createOrder(input); }
    @GetMapping("/orders/{id}") public Detail detail(@PathVariable long id) { return service.detail(id); }
    @PostMapping("/orders/{id}/items") public Detail item(@PathVariable long id,@Valid @RequestBody ItemInput input) { return service.addItem(id,input); }
    @DeleteMapping("/orders/{id}/items/{itemId}") public Detail remove(@PathVariable long id,@PathVariable long itemId) { return service.removeItem(id,itemId); }
    @PostMapping("/orders/{id}/status") public Detail transition(@PathVariable long id,@Valid @RequestBody Transition input) { return service.transition(id,input); }
    @GetMapping("/dashboard") public Dashboard dashboard() { return service.dashboard(); }
    @GetMapping(value="/orders.csv",produces="text/csv;charset=UTF-8")
    public ResponseEntity<String> csv() {
        var orders=repo.jdbc().query(HubRepository.ORDER_SELECT+" ORDER BY o.id",HubRepository.ORDER);
        var csv=new StringBuilder("\uFEFFOrdem;Cliente;Equipamento;Titulo;Status;Prazo;Total BRL\r\n");
        for(var o:orders) csv.append(o.id()).append(';').append(cell(o.customerName())).append(';').append(cell(o.assetName())).append(';').append(cell(o.title())).append(';').append(o.status()).append(';').append(o.dueDate()).append(';').append(o.total().toPlainString().replace('.',',')).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=servicehub-ordens.csv").body(csv.toString());
    }
    static String cell(String value) {
        // Evita fórmulas ao abrir no Excel, incluindo prefixos com espaços/controle.
        String clean=value.stripLeading();
        if(!clean.isEmpty() && "=+-@".indexOf(clean.charAt(0))>=0) value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
}
