package io.github.gmerick.servicehub;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> business(BusinessException e) { return ResponseEntity.status(e.status()).body(Map.of("message",e.getMessage())); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicate(DataIntegrityViolationException e) { return ResponseEntity.status(409).body(Map.of("message","Registro duplicado ou vínculo inválido. Confira e-mail, série e SKU.")); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> validation(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Dados inválidos. Confira campos obrigatórios, valores, quantidades e prazo.")); }
}
