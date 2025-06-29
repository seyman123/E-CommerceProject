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

import org.springframework.stereotype.Service;

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

    @Override
    public OrderDto placeOrder(Long userId) {
        return placeOrder(userId, null);
    }

    @Override
    public OrderDto placeOrder(Long userId, String couponCode) {
        Cart cart = cartService.getCartByUserId(userId);
        Order order = createOrder(cart);
        List<OrderItem> orderItems = createOrderItems(order, cart);
        order.setOrderItems(new HashSet<>(orderItems));
        
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

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(cart.getId());

        return this.convertToDto(savedOrder);
    }

    private Order createOrder(Cart cart) {
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
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
    public OrderDto getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Override
    public List<OrderDto> getUserOrders(Long userId) {
        return orderRepository.findByUser_Id(userId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public OrderDto cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
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
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public OrderDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
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
    public OrderDto approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
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
    public OrderDto rejectOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
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
        OrderItemDto dto = new OrderItemDto();
        dto.setProductId(orderItem.getProduct().getId());
        dto.setProductName(orderItem.getProduct().getName());
        dto.setProductBrand(orderItem.getProduct().getBrand());
        dto.setProductCategory(orderItem.getProduct().getCategory() != null ? orderItem.getProduct().getCategory().getName() : "");
        dto.setProductImageUrl(orderItem.getProduct().getImages() != null && !orderItem.getProduct().getImages().isEmpty() 
                ? "http://localhost:9193/api/v1" + orderItem.getProduct().getImages().get(0).getDownloadUrl() 
                : "");
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        return dto;
    }
}
