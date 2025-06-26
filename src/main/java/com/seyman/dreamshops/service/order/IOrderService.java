package com.seyman.dreamshops.service.order;

import com.seyman.dreamshops.dto.OrderDto;
import com.seyman.dreamshops.model.Order;

import java.util.List;

public interface IOrderService {
    OrderDto placeOrder(Long userId);
    OrderDto placeOrder(Long userId, String couponCode);
    OrderDto getOrder(Long orderId);
    List<OrderDto> getUserOrders(Long userId);
    OrderDto cancelOrder(Long orderId);
    
    // Admin methods
    List<OrderDto> getAllOrders();
    OrderDto updateOrderStatus(Long orderId, String status);
    OrderDto approveOrder(Long orderId);
    OrderDto rejectOrder(Long orderId);
}
