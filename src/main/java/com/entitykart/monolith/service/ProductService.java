package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.ProductDTO;
import com.entitykart.monolith.entity.ProductEntity;
import com.entitykart.monolith.event.ProductCreatedEvent;
import com.entitykart.monolith.exception.EntityNotFoundException;
import com.entitykart.monolith.repository.ProductRepository;
import com.entitykart.monolith.repository.CategoryRepository;
import com.entitykart.monolith.repository.SubCategoryRepository;
import com.entitykart.monolith.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final int MAX_UNBOUNDED_FETCH = 500;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductMapper productMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        ProductEntity product = new ProductEntity();
        product.setProductName(dto.getProductName());
        product.setDescription(dto.getDescription());
        product.setBrand(dto.getBrand());
        product.setPrice(dto.getPrice());
        product.setMrp(dto.getMrp());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setMainImageURL(dto.getMainImageURL());
        product.setCategoryId(dto.getCategoryId());
        product.setSubCategoryId(dto.getSubCategoryId());
        product.setSellerId(dto.getSellerId());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

        ProductEntity saved = productRepository.save(product);

        ProductCreatedEvent event = new ProductCreatedEvent(
                saved.getProductId(),
                saved.getProductName(),
                saved.getSellerId(),
                LocalDateTime.now());
        eventPublisher.publishEvent(event);

        return productMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return productMapper.toDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return productRepository.findAllById(ids).stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable).map(productMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsBySeller(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable).map(productMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        long total = productRepository.count();
        if (total > MAX_UNBOUNDED_FETCH) {
            log.warn("getAllProducts() fetched only first {} of {} total products. Use paginated endpoint instead.",
                    MAX_UNBOUNDED_FETCH, total);
        }
        return productRepository.findAll(PageRequest.of(0, MAX_UNBOUNDED_FETCH))
                .stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setProductName(dto.getProductName());
        product.setDescription(dto.getDescription());
        product.setBrand(dto.getBrand());
        product.setPrice(dto.getPrice());
        product.setMrp(dto.getMrp());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setMainImageURL(dto.getMainImageURL());
        product.setCategoryId(dto.getCategoryId());
        product.setSubCategoryId(dto.getSubCategoryId());
        product.setSellerId(dto.getSellerId());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        ProductEntity saved = productRepository.save(product);
        return productMapper.toDTO(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsFiltered(
            Long categoryId,
            Long subCategoryId,
            String search,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            Pageable pageable) {
        Pageable sanitized = sanitizePageable(pageable);
        return productRepository.filterProducts(categoryId, subCategoryId, search, minPrice, maxPrice, sanitized)
                .map(productMapper::toDTO);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            boolean hasDiscount = pageable.getSort().stream()
                    .anyMatch(order -> order.getProperty().equalsIgnoreCase("discount") || 
                                       order.getProperty().equalsIgnoreCase("discountPercent"));
            if (hasDiscount) {
                return org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, 
                                "mrp"
                        )
                );
            }
        }
        return pageable;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getProductStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalProducts", productRepository.count());
        stats.put("totalCategories", categoryRepository.count());
        stats.put("totalSubCategories", subCategoryRepository.count());
        return stats;
    }
}
