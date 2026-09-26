package io.github.gmerick.servicehub;

public class BusinessException extends RuntimeException {
    private final int status;
    public BusinessException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static BusinessException missing() { return new BusinessException(404, "Registro não encontrado."); }
    public static BusinessException conflict(String message) { return new BusinessException(409, message); }
}
