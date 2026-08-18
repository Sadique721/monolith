package com.entitykart.monolith.controller;

import com.entitykart.monolith.service.EmailService;
import com.entitykart.monolith.service.OrderService;
import com.entitykart.monolith.service.ProductService;
import com.entitykart.monolith.service.UserService;
import com.entitykart.monolith.service.PaymentService;
import com.entitykart.monolith.service.ReturnService;
import com.entitykart.monolith.service.ReviewService;
import com.entitykart.monolith.service.WishlistService;
import com.entitykart.monolith.dto.OrderDTO;
import com.entitykart.monolith.dto.ProductDTO;
import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.dto.PaymentDTO;
import com.entitykart.monolith.dto.ReturnResponse;
import com.entitykart.monolith.dto.ReviewDTO;
import com.entitykart.monolith.dto.WishlistItemDTO;
import com.entitykart.monolith.mapper.PaymentMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
@Slf4j
public class AdminExportController {

    private final EmailService emailService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final ReturnService returnService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;

    // ==================== EXCEL EXPORTS ====================

    @GetMapping("/orders/excel")
    public void exportOrdersToExcel(HttpServletResponse response) throws IOException {
        List<OrderDTO> list = orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Orders");
        String[] cols = {"Order ID", "Customer ID", "Address ID", "Total Amount", "Order Status",
                         "Payment Status", "Order Date", "Created At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (OrderDTO o : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(o.getOrderId());
            row.createCell(1).setCellValue(o.getCustomerId());
            row.createCell(2).setCellValue(o.getAddressId());
            row.createCell(3).setCellValue(o.getTotalAmount());
            row.createCell(4).setCellValue(o.getOrderStatus());
            row.createCell(5).setCellValue(o.getPaymentStatus());
            row.createCell(6).setCellValue(o.getOrderDate() != null ? o.getOrderDate().toString() : "");
            row.createCell(7).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "orders.xlsx");
    }

    @GetMapping("/products/excel")
    public void exportProductsToExcel(HttpServletResponse response) throws IOException {
        List<ProductDTO> list = productService.getAllProducts();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Products");
        String[] cols = {"Product ID", "Name", "Brand", "Description", "Price", "MRP", "Stock",
                         "SKU", "Category ID", "SubCategory ID", "Seller ID", "Created At", "Discount %"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (ProductDTO p : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(p.getProductId());
            row.createCell(1).setCellValue(p.getProductName());
            row.createCell(2).setCellValue(p.getBrand());
            row.createCell(3).setCellValue(p.getDescription());
            row.createCell(4).setCellValue(p.getPrice() != null ? p.getPrice().doubleValue() : 0.0);
            row.createCell(5).setCellValue(p.getMrp() != null ? p.getMrp().doubleValue() : 0.0);
            row.createCell(6).setCellValue(p.getStockQuantity());
            row.createCell(7).setCellValue(p.getSku());
            row.createCell(8).setCellValue(p.getCategoryId() != null ? p.getCategoryId() : 0L);
            row.createCell(9).setCellValue(p.getSubCategoryId() != null ? p.getSubCategoryId() : 0L);
            row.createCell(10).setCellValue(p.getSellerId() != null ? p.getSellerId() : 0L);
            row.createCell(11).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            row.createCell(12).setCellValue(p.getDiscountPercent() != null ? p.getDiscountPercent().doubleValue() : 0.0);
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "products.xlsx");
    }

    @GetMapping("/users/excel")
    public void exportUsersToExcel(HttpServletResponse response) throws IOException {
        List<UserDTO> list = userService.getAllUsers(org.springframework.data.domain.Pageable.unpaged()).getContent();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Users");
        String[] cols = {"User ID", "Name", "Email", "Role", "Active"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (UserDTO u : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(u.getId());
            row.createCell(1).setCellValue(u.getName());
            row.createCell(2).setCellValue(u.getEmail());
            row.createCell(3).setCellValue(u.getRole());
            row.createCell(4).setCellValue(u.getActive() != null ? u.getActive().toString() : "false");
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "users.xlsx");
    }

    @GetMapping("/payments/excel")
    public void exportPaymentsToExcel(HttpServletResponse response) throws IOException {
        List<PaymentDTO> list = paymentService.getAllPayments().stream()
                .map(paymentMapper::toDTO)
                .toList();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Payments");
        String[] cols = {"Payment ID", "Order ID", "Amount", "Mode", "Transaction Ref", "Status", "Payment Date"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (PaymentDTO p : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(p.getPaymentId());
            row.createCell(1).setCellValue(p.getOrderId());
            row.createCell(2).setCellValue(p.getAmount());
            row.createCell(3).setCellValue(p.getPaymentMode());
            row.createCell(4).setCellValue(p.getTransactionRef());
            row.createCell(5).setCellValue(p.getPaymentStatus());
            row.createCell(6).setCellValue(p.getPaymentDate() != null ? p.getPaymentDate().toString() : "");
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "payments.xlsx");
    }

    @GetMapping("/returns/excel")
    public void exportReturnsToExcel(HttpServletResponse response) throws IOException {
        List<ReturnResponse> list = returnService.getAllReturns();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Returns");
        String[] cols = {"Return ID", "Order ID", "Customer ID", "Product ID", "Quantity",
                         "Reason", "Status", "Refund Amount", "Admin Note", "Rejection Reason", "Created At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (ReturnResponse rt : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(rt.getReturnId());
            row.createCell(1).setCellValue(rt.getOrderId());
            row.createCell(2).setCellValue(rt.getCustomerId());
            row.createCell(3).setCellValue(rt.getProductId());
            row.createCell(4).setCellValue(rt.getQuantity());
            row.createCell(5).setCellValue(rt.getReason());
            row.createCell(6).setCellValue(rt.getStatus());
            row.createCell(7).setCellValue(rt.getRefundAmount());
            row.createCell(8).setCellValue(rt.getAdminNote());
            row.createCell(9).setCellValue(rt.getRejectionReason());
            row.createCell(10).setCellValue(rt.getCreatedAt() != null ? rt.getCreatedAt().toString() : "");
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "returns.xlsx");
    }

    @GetMapping("/reviews/excel")
    public void exportReviewsToExcel(HttpServletResponse response) throws IOException {
        List<ReviewDTO> list = reviewService.getAllReviews(Pageable.unpaged()).getContent();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Reviews");
        String[] cols = {"Review ID", "Product ID", "Customer ID", "Rating", "Comment", "Created At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (ReviewDTO rev : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(rev.getReviewId());
            row.createCell(1).setCellValue(rev.getProductId());
            row.createCell(2).setCellValue(rev.getCustomerId());
            row.createCell(3).setCellValue(rev.getRating());
            row.createCell(4).setCellValue(rev.getComment());
            row.createCell(5).setCellValue(rev.getCreatedAt() != null ? rev.getCreatedAt().toString() : "");
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "reviews.xlsx");
    }

    @GetMapping("/wishlist/excel")
    public void exportWishlistToExcel(HttpServletResponse response) throws IOException {
        List<WishlistItemDTO> list = wishlistService.getAllWishlistItems();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Wishlist");
        String[] cols = {"Wishlist ID", "Product ID", "Product Name", "Price", "Added At"};
        createHeaderRow(sheet, cols);
        int r = 1;
        for (WishlistItemDTO w : list) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(w.getWishlistId());
            row.createCell(1).setCellValue(w.getProductId());
            row.createCell(2).setCellValue(w.getProductName());
            row.createCell(3).setCellValue(w.getPrice() != null ? w.getPrice() : 0.0);
            row.createCell(4).setCellValue(w.getAddedAt() != null ? w.getAddedAt().toString() : "");
        }
        autoSizeColumns(sheet, cols.length);
        writeResponse(response, wb, "wishlist.xlsx");
    }

    // ==================== WORD EXPORTS ====================

    @GetMapping("/orders/word")
    public void exportOrdersToWord(HttpServletResponse response) throws IOException {
        List<OrderDTO> list = orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=orders.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Order Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (OrderDTO o : list) {
                writer.printf("Order #%d | Customer #%d | Total ₹%.2f | Status %s | Payment %s | Date %s%n",
                        o.getOrderId(), o.getCustomerId(), o.getTotalAmount(),
                        o.getOrderStatus(), o.getPaymentStatus(), o.getOrderDate());
            }
        }
    }

    @GetMapping("/products/word")
    public void exportProductsToWord(HttpServletResponse response) throws IOException {
        List<ProductDTO> list = productService.getAllProducts();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=products.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Product Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (ProductDTO p : list) {
                writer.printf("Product #%d | %s | Brand %s | Price ₹%.2f | Stock %d | SKU %s%n",
                        p.getProductId(), p.getProductName(), p.getBrand(),
                        p.getPrice() != null ? p.getPrice().doubleValue() : 0.0, p.getStockQuantity(), p.getSku());
            }
        }
    }

    @GetMapping("/users/word")
    public void exportUsersToWord(HttpServletResponse response) throws IOException {
        List<UserDTO> list = userService.getAllUsers(org.springframework.data.domain.Pageable.unpaged()).getContent();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=users.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("User Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (UserDTO u : list) {
                writer.printf("User #%d | %s | %s | Role %s | Active %s%n",
                        u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getActive() != null ? u.getActive().toString() : "false");
            }
        }
    }

    @GetMapping("/payments/word")
    public void exportPaymentsToWord(HttpServletResponse response) throws IOException {
        List<PaymentDTO> list = paymentService.getAllPayments().stream()
                .map(paymentMapper::toDTO)
                .toList();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=payments.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Payment Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (PaymentDTO p : list) {
                writer.printf("Payment #%d | Order #%d | ₹%.2f | Mode %s | Status %s | Ref %s%n",
                        p.getPaymentId(), p.getOrderId(), p.getAmount(),
                        p.getPaymentMode(), p.getPaymentStatus(), p.getTransactionRef());
            }
        }
    }

    @GetMapping("/returns/word")
    public void exportReturnsToWord(HttpServletResponse response) throws IOException {
        List<ReturnResponse> list = returnService.getAllReturns();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=returns.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Return Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (ReturnResponse rt : list) {
                writer.printf("Return #%d | Order #%d | Status %s | Reason %s | Refund ₹%.2f%n",
                        rt.getReturnId(), rt.getOrderId(), rt.getStatus(),
                        rt.getReason(), rt.getRefundAmount());
            }
        }
    }

    @GetMapping("/reviews/word")
    public void exportReviewsToWord(HttpServletResponse response) throws IOException {
        List<ReviewDTO> list = reviewService.getAllReviews(Pageable.unpaged()).getContent();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=reviews.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Review Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (ReviewDTO r : list) {
                writer.printf("Review #%d | Product #%d | Customer #%d | Rating %d/5 | Comment: %s%n",
                        r.getReviewId(), r.getProductId(), r.getCustomerId(),
                        r.getRating(), r.getComment());
            }
        }
    }

    @GetMapping("/wishlist/word")
    public void exportWishlistToWord(HttpServletResponse response) throws IOException {
        List<WishlistItemDTO> list = wishlistService.getAllWishlistItems();
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=wishlist.doc");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Wishlist Report\n");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            for (WishlistItemDTO w : list) {
                writer.printf("Wishlist #%d | Product #%d | Name %s | Price ₹%.2f | Added %s%n",
                        w.getWishlistId(), w.getProductId(), w.getProductName(),
                        w.getPrice(), w.getAddedAt());
            }
        }
    }

    // ==================== EMAIL REPORT (Excel + Word attachment) ====================

    @PostMapping("/send-report")
    public String sendReportEmail(@RequestParam String reportType, @RequestParam String email) {
        try {
            byte[] excelData = generateExcelReportBytes(reportType);
            byte[] wordData  = generateWordReportBytes(reportType);
            emailService.sendReportWithAttachments(email, reportType, excelData, wordData);
            return "Report sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send report for type: {}", reportType, e);
            throw new RuntimeException("Failed to send report: " + e.getMessage());
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private void createHeaderRow(Sheet sheet, String[] cols) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try { sheet.autoSizeColumn(i); } catch (Exception ignored) { }
        }
    }

    private void writeResponse(HttpServletResponse response, Workbook wb, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        wb.write(response.getOutputStream());
        wb.close();
    }
    private byte[] generateExcelReportBytes(String reportType) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(reportType);
            String[] cols = null;
            int r = 1;
            if ("orders".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Order ID", "Customer ID", "Address ID", "Total Amount", "Order Status", "Payment Status", "Order Date", "Created At"};
                createHeaderRow(sheet, cols);
                for (OrderDTO o : orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(o.getOrderId()); row.createCell(1).setCellValue(o.getCustomerId());
                    row.createCell(2).setCellValue(o.getAddressId()); row.createCell(3).setCellValue(o.getTotalAmount());
                    row.createCell(4).setCellValue(o.getOrderStatus()); row.createCell(5).setCellValue(o.getPaymentStatus());
                    row.createCell(6).setCellValue(o.getOrderDate() != null ? o.getOrderDate().toString() : ""); row.createCell(7).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
                }
            } else if ("products".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Product ID", "Name", "Brand", "Price", "Stock", "Discount %"};
                createHeaderRow(sheet, cols);
                for (ProductDTO p : productService.getAllProducts()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(p.getProductId()); row.createCell(1).setCellValue(p.getProductName());
                    row.createCell(2).setCellValue(p.getBrand()); row.createCell(3).setCellValue(p.getPrice() != null ? p.getPrice().doubleValue() : 0.0);
                    row.createCell(4).setCellValue(p.getStockQuantity()); row.createCell(5).setCellValue(p.getDiscountPercent() != null ? p.getDiscountPercent().doubleValue() : 0.0);
                }
            } else if ("users".equalsIgnoreCase(reportType)) {
                cols = new String[]{"User ID", "Name", "Email", "Role", "Active"};
                createHeaderRow(sheet, cols);
                for (UserDTO u : userService.getAllUsers(org.springframework.data.domain.Pageable.unpaged()).getContent()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(u.getId()); row.createCell(1).setCellValue(u.getName());
                    row.createCell(2).setCellValue(u.getEmail()); row.createCell(3).setCellValue(u.getRole());
                    row.createCell(4).setCellValue(u.getActive() != null ? u.getActive().toString() : "false");
                }
            } else if ("payments".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Payment ID", "Order ID", "Amount", "Mode", "Transaction Ref", "Status", "Payment Date"};
                createHeaderRow(sheet, cols);
                for (PaymentDTO p : paymentService.getAllPayments().stream().map(paymentMapper::toDTO).toList()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(p.getPaymentId());
                    row.createCell(1).setCellValue(p.getOrderId());
                    row.createCell(2).setCellValue(p.getAmount());
                    row.createCell(3).setCellValue(p.getPaymentMode());
                    row.createCell(4).setCellValue(p.getTransactionRef());
                    row.createCell(5).setCellValue(p.getPaymentStatus());
                    row.createCell(6).setCellValue(p.getPaymentDate() != null ? p.getPaymentDate().toString() : "");
                }
            } else if ("returns".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Return ID", "Order ID", "Customer ID", "Product ID", "Quantity", "Reason", "Status", "Refund Amount", "Created At"};
                createHeaderRow(sheet, cols);
                for (ReturnResponse rt : returnService.getAllReturns()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(rt.getReturnId());
                    row.createCell(1).setCellValue(rt.getOrderId());
                    row.createCell(2).setCellValue(rt.getCustomerId());
                    row.createCell(3).setCellValue(rt.getProductId());
                    row.createCell(4).setCellValue(rt.getQuantity());
                    row.createCell(5).setCellValue(rt.getReason());
                    row.createCell(6).setCellValue(rt.getStatus());
                    row.createCell(7).setCellValue(rt.getRefundAmount());
                    row.createCell(8).setCellValue(rt.getCreatedAt() != null ? rt.getCreatedAt().toString() : "");
                }
            } else if ("reviews".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Review ID", "Product ID", "Customer ID", "Rating", "Comment", "Created At"};
                createHeaderRow(sheet, cols);
                for (ReviewDTO rev : reviewService.getAllReviews(Pageable.unpaged()).getContent()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(rev.getReviewId());
                    row.createCell(1).setCellValue(rev.getProductId());
                    row.createCell(2).setCellValue(rev.getCustomerId());
                    row.createCell(3).setCellValue(rev.getRating());
                    row.createCell(4).setCellValue(rev.getComment());
                    row.createCell(5).setCellValue(rev.getCreatedAt() != null ? rev.getCreatedAt().toString() : "");
                }
            } else if ("wishlist".equalsIgnoreCase(reportType)) {
                cols = new String[]{"Wishlist ID", "Product ID", "Product Name", "Price", "Added At"};
                createHeaderRow(sheet, cols);
                for (WishlistItemDTO w : wishlistService.getAllWishlistItems()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(w.getWishlistId());
                    row.createCell(1).setCellValue(w.getProductId());
                    row.createCell(2).setCellValue(w.getProductName());
                    row.createCell(3).setCellValue(w.getPrice() != null ? w.getPrice() : 0.0);
                    row.createCell(4).setCellValue(w.getAddedAt() != null ? w.getAddedAt().toString() : "");
                }
            }
            if (cols != null) autoSizeColumns(sheet, cols.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] generateWordReportBytes(String reportType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println(reportType.toUpperCase() + " REPORT");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            writer.println("========================================\n");
            if ("orders".equalsIgnoreCase(reportType)) {
                for (OrderDTO o : orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent())
                    writer.printf("Order #%d | Customer #%d | Total ₹%.2f | Status %s | Payment %s | Date %s%n",
                            o.getOrderId(), o.getCustomerId(), o.getTotalAmount(),
                            o.getOrderStatus(), o.getPaymentStatus(), o.getOrderDate());
            } else if ("products".equalsIgnoreCase(reportType)) {
                for (ProductDTO p : productService.getAllProducts())
                    writer.printf("Product #%d | %s | Brand %s | Price ₹%.2f | Stock %d | SKU %s%n",
                            p.getProductId(), p.getProductName(), p.getBrand(),
                            p.getPrice(), p.getStockQuantity(), p.getSku());
            } else if ("users".equalsIgnoreCase(reportType)) {
                for (UserDTO u : userService.getAllUsers(org.springframework.data.domain.Pageable.unpaged()).getContent())
                    writer.printf("User #%d | %s | %s | Role %s | Active %s%n",
                            u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getActive() != null ? u.getActive().toString() : "false");
            } else if ("payments".equalsIgnoreCase(reportType)) {
                for (PaymentDTO p : paymentService.getAllPayments().stream().map(paymentMapper::toDTO).toList())
                    writer.printf("Payment #%d | Order #%d | ₹%.2f | Mode %s | Status %s | Ref %s%n",
                            p.getPaymentId(), p.getOrderId(), p.getAmount(),
                            p.getPaymentMode(), p.getPaymentStatus(), p.getTransactionRef());
            } else if ("returns".equalsIgnoreCase(reportType)) {
                for (ReturnResponse rt : returnService.getAllReturns())
                    writer.printf("Return #%d | Order #%d | Status %s | Reason %s | Refund ₹%.2f%n",
                            rt.getReturnId(), rt.getOrderId(), rt.getStatus(),
                            rt.getReason(), rt.getRefundAmount());
            } else if ("reviews".equalsIgnoreCase(reportType)) {
                for (ReviewDTO r : reviewService.getAllReviews(Pageable.unpaged()).getContent())
                    writer.printf("Review #%d | Product #%d | Customer #%d | Rating %d/5 | Comment: %s%n",
                            r.getReviewId(), r.getProductId(), r.getCustomerId(),
                            r.getRating(), r.getComment());
            } else if ("wishlist".equalsIgnoreCase(reportType)) {
                for (WishlistItemDTO w : wishlistService.getAllWishlistItems())
                    writer.printf("Wishlist #%d | Product #%d | Name %s | Price ₹%.2f | Added %s%n",
                            w.getWishlistId(), w.getProductId(), w.getProductName(),
                            w.getPrice(), w.getAddedAt());
            }
        }
        return baos.toByteArray();
    }
}
