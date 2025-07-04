package com.seyman.dreamshops.service.order;

import com.seyman.dreamshops.dto.OrderDto;
import com.seyman.dreamshops.dto.OrderItemDto;
import com.seyman.dreamshops.enums.OrderStatus;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.model.Order;
import com.seyman.dreamshops.model.OrderItem;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.repository.OrderRepository;
import com.seyman.dreamshops.repository.ProductRepository;
import com.seyman.dreamshops.service.cart.ICartService;
import com.seyman.dreamshops.service.coupon.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ICartService cartService;
    private final ICouponService couponService;

    @Value("${api.prefix:/api/v1}")
    private String apiPrefix;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    // Use Render's built-in environment variable for the external URL
    @Value("${RENDER_EXTERNAL_URL:}")
    private String renderExternalUrl;

    @Override
    public OrderDto placeOrder(Long userId) {
        return placeOrder(userId, null);
    }

    @Override
    @Transactional
    public OrderDto placeOrder(Long userId, String couponCode) {
        Cart cart = cartService.getCartByUserId(userId);
        
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty or not found");
        }
        
        Order order = createOrder(cart, couponCode);
        order = orderRepository.save(order);
        
        // Clear the cart after successful order creation
        cartService.clearCart(cart.getId());
        
        return convertToDto(order);
    }

    private Order createOrder(Cart cart, String couponCode) {
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        
        // Create order items from cart items
        List<OrderItem> orderItems = createOrderItems(order, cart);
        order.setOrderItems(new HashSet<>(orderItems));
        
        // Calculate amounts
        BigDecimal totalAmount = calculateTotalAmount(orderItems);
        BigDecimal originalAmount = totalAmount;
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        // Apply coupon if provided
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            try {
                // Validate coupon first
                boolean isValid = couponService.validateCoupon(couponCode, totalAmount);
                if (!isValid) {
                    throw new RuntimeException("Invalid coupon code: " + couponCode);
                }
                
                // Apply coupon discount
                discountAmount = couponService.applyCoupon(couponCode, totalAmount);
                totalAmount = totalAmount.subtract(discountAmount);
                
                // Ensure total doesn't go negative
                if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                    totalAmount = BigDecimal.ZERO;
                }
                
                // Store coupon code in order
                order.setCouponCode(couponCode);
                
                // Mark coupon as used (increment usage count)
                couponService.useCoupon(couponCode);
                
            } catch (Exception e) {
                throw new RuntimeException("Error applying coupon: " + e.getMessage());
            }
        }
        
        order.setOriginalAmount(originalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        
        return order;
    }

    private List<OrderItem> createOrderItems(Order order, Cart cart) {
        return cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            product.setInventory(product.getInventory() - cartItem.getQuantity());
            productRepository.save(product);

            return new OrderItem(order, product, cartItem.getQuantity(), cartItem.getUnitPrice());
        }).toList();
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItemList) {
        return orderItemList
                .stream()
                .map(item -> item.getPrice()
                        .multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found");
        }
        return this.convertToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(Long userId) {
        try {
            List<Order> orders = orderRepository.findByUser_Id(userId);
            return orders.stream()
                    .map(this::convertToDto)
                    .toList();
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Error fetching user orders for userId: " + userId + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch user orders: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found");
        }
        
        // Check if order can be cancelled (only PENDING and CONFIRMED orders can be cancelled)
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }
        
        // Set order status to CANCELLED
        order.setOrderStatus(OrderStatus.CANCELLED);
        
        // Restore product inventory
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setInventory(product.getInventory() + orderItem.getQuantity());
            productRepository.save(product);
        }
        
        Order savedOrder = orderRepository.save(order);
        return this.convertToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllWithDetails()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found");
        }
        
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setOrderStatus(orderStatus);
            Order savedOrder = orderRepository.save(order);
            return this.convertToDto(savedOrder);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    @Override
    @Transactional
    public OrderDto approveOrder(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found");
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be approved");
        }
        
        // Check stock availability before approving
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            if (product.getInventory() < 0) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }
        }
        
        order.setOrderStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);
        return this.convertToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto rejectOrder(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found");
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be rejected");
        }
        
        // Restore product inventory
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setInventory(product.getInventory() + orderItem.getQuantity());
            productRepository.save(product);
        }
        
        order.setOrderStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return this.convertToDto(savedOrder);
    }

    private OrderDto convertToDto(Order order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getOrderId());
        orderDto.setUserId(order.getUser().getId());
        
        // Order date is already LocalDateTime, no conversion needed
        orderDto.setOrderDate(order.getOrderDate());
        orderDto.setCreatedAt(order.getOrderDate());
        
        orderDto.setTotalAmount(order.getTotalAmount());
        orderDto.setOriginalAmount(order.getOriginalAmount());
        orderDto.setDiscountAmount(order.getDiscountAmount());
        orderDto.setStatus(order.getOrderStatus().name());
        orderDto.setCouponCode(order.getCouponCode());
        
        List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(this::convertOrderItemToDto)
                .collect(Collectors.toList());
        orderDto.setItems(itemDtos);
        
        return orderDto;
    }
    
    private OrderItemDto convertOrderItemToDto(OrderItem orderItem) {
        try {
            OrderItemDto dto = new OrderItemDto();
            dto.setProductId(orderItem.getProduct().getId());
            dto.setProductName(orderItem.getProduct().getName());
            dto.setProductBrand(orderItem.getProduct().getBrand());
            
            // Safely get category name
            String categoryName = "";
            try {
                if (orderItem.getProduct().getCategory() != null) {
                    categoryName = orderItem.getProduct().getCategory().getName();
                }
            } catch (Exception e) {
                System.err.println("Error getting category for product: " + orderItem.getProduct().getId() + " - " + e.getMessage());
            }
            dto.setProductCategory(categoryName);
            
            // Safely handle image URL
            String imageUrl = "";
            try {
                if (orderItem.getProduct().getImages() != null && !orderItem.getProduct().getImages().isEmpty()) {
                    String downloadUrl = orderItem.getProduct().getImages().get(0).getDownloadUrl();
                    if (downloadUrl != null && !downloadUrl.startsWith("http")) {
                        // If it's a relative URL, construct the full URL
                        String fullBaseUrl;
                        if (!renderExternalUrl.isEmpty()) {
                            // Use Render's external URL in production
                            fullBaseUrl = renderExternalUrl + apiPrefix;
                        } else {
                            // Fallback for development
                            fullBaseUrl = contextPath + apiPrefix;
                        }
                        imageUrl = fullBaseUrl + downloadUrl;
                    } else if (downloadUrl != null) {
                        imageUrl = downloadUrl;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting image URL for product: " + orderItem.getProduct().getId() + " - " + e.getMessage());
                imageUrl = ""; // Default to empty string on error
            }
            dto.setProductImageUrl(imageUrl);
            
            dto.setQuantity(orderItem.getQuantity());
            dto.setPrice(orderItem.getPrice());
            return dto;
        } catch (Exception e) {
            System.err.println("Error converting OrderItem to DTO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to convert order item: " + e.getMessage(), e);
        }
    }
}
