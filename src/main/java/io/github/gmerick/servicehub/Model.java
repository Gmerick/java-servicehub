package io.github.gmerick.servicehub;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.*;

public final class Model {
    private Model() {}
    public enum Status { DRAFT, APPROVED, IN_PROGRESS, COMPLETED, CANCELED }
    public enum Priority { LOW, NORMAL, HIGH }
    public record Customer(long id, String name, String email, String phone) {}
    public record Asset(long id, long customerId, String customerName, String name, String serial) {}
    public record Part(long id, String name, String sku, BigDecimal price, int stock, int minimum) {}
    public record Item(long id, Long partId, String description, int quantity, BigDecimal unitPrice, BigDecimal total) {}
    public record Event(long id, LocalDateTime createdAt, String message) {}
    public record Order(long id, long assetId, String assetName, String customerName, String title,
        String description, Priority priority, Status status, LocalDateTime createdAt,
        LocalDate dueDate, LocalDateTime completedAt, String resolution, BigDecimal total) {}
    public record Detail(Order order, List<Item> items, List<Event> events) {}
    public record Page(List<Order> items, long total, int page, int size) {}
    public record Dashboard(long open, long overdue, long completed, BigDecimal completedValue,
        long lowStock, List<Order> recent) {}
    public record CustomerInput(@NotBlank @Size(max=100) String name,
        @NotBlank @Email @Size(max=160) String email, @NotBlank @Size(max=30) String phone) {}
    public record AssetInput(@Positive long customerId, @NotBlank @Size(max=100) String name,
        @NotBlank @Size(max=80) String serial) {}
    public record PartInput(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=40) String sku,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) BigDecimal price,
        @Min(0) @Max(1000000) int stock, @Min(0) @Max(1000000) int minimum) {}
    public record OrderInput(@Positive long assetId, @NotBlank @Size(max=140) String title,
        @NotBlank @Size(max=2000) String description, @NotNull Priority priority,
        @NotNull @FutureOrPresent LocalDate dueDate) {}
    public record ItemInput(@Positive Long partId, @NotBlank @Size(max=160) String description,
        @Min(1) @Max(1000) int quantity,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) BigDecimal unitPrice) {}
    public record Transition(@NotNull Status status, @NotBlank @Size(max=2000) String note) {}
    public record Restock(@Min(1) @Max(100000) int quantity, @NotBlank @Size(max=160) String reason) {}
}
