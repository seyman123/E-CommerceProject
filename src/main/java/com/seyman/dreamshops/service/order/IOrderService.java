package com.seyman.dreamshops.service.order;

import com.seyman.dreamshops.dto.OrderDto;
import com.seyman.dreamshops.model.Order;

import java.util.List;

public interface IOrderService {
    OrderDto placeOrder(Long userId);
    OrderDto getOrder(Long orderId);
    List<OrderDto> getUserOrders(Long userId);
}
