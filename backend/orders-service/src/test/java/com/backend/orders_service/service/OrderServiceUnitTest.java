package com.backend.orders_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestTemplate;

import com.backend.orders_service.client.ProductInventoryClient;
import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.RedoOrderResponse;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.model.PaymentMethod;
import com.backend.orders_service.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductInventoryClient productInventoryClient;

    @Mock
    private OrderStatusScheduler orderStatusScheduler;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testOrderItem = new OrderItem(
                "prod-123",
                2,
                new BigDecimal("29.99"),
                "seller-456",
                "Test Product",
                null);

        testOrder = Order.builder()
                .id("order-123")
                .userId("user-456")
                .shippingAddress("123 Main St")
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(testOrderItem)))
                .paymentMethod(PaymentMethod.CARD)
                .orderDate(Instant.now())
                .build();

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId("user-456");
        createOrderRequest.setShippingAddress("123 Main St");
        createOrderRequest.setItems(new ArrayList<>(List.of(testOrderItem)));
        createOrderRequest.setPaymentMethod(PaymentMethod.CARD);
    }

    // ============================================================================
    // ORDER CRUD OPERATIONS
    // ============================================================================

    @Nested
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void testCreateOrderSuccess() {
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId("new-order-id");
                return order;
            });

            Order created = orderService.createOrder(createOrderRequest);

            assertThat(created).isNotNull();
            assertThat(created.getUserId()).isEqualTo("user-456");
            assertThat(created.getShippingAddress()).isEqualTo("123 Main St");
            assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(created.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should create order with empty items list")
        void testCreateOrderWithEmptyItems() {
            createOrderRequest.setItems(null);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order created = orderService.createOrder(createOrderRequest);

            assertThat(created.getItems()).isNotNull();
            assertThat(created.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should set order date on creation")
        void testCreateOrderSetsOrderDate() {
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order created = orderService.createOrder(createOrderRequest);

            assertThat(created.getOrderDate()).isNotNull();
        }
    }

    @Nested
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should get order by ID successfully")
        void testGetOrderByIdSuccess() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            Order found = orderService.getOrderById("order-123");

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo("order-123");
            assertThat(found.getUserId()).isEqualTo("user-456");
            verify(orderRepository).findById("order-123");
        }

        @Test
        @DisplayName("Should return null when order not found")
        void testGetOrderByIdNotFound() {
            when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

            Order found = orderService.getOrderById("nonexistent");

            assertThat(found).isNull();
        }
    }

    @Nested
    class GetOrdersByUserIdTests {

        @Test
        @DisplayName("Should get orders by user ID with pagination")
        void testGetOrdersByUserIdSuccess() {
            List<Order> orders = List.of(testOrder);
            Page<Order> page = new PageImpl<>(orders);
            when(orderRepository.findByUserIdAndIsRemovedFalse(eq("user-456"), any(Pageable.class))).thenReturn(page);

            Page<Order> result = orderService.getOrdersByUserId("user-456", 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo("user-456");
        }

        @Test
        @DisplayName("Should return empty page when no orders found")
        void testGetOrdersByUserIdEmpty() {
            Page<Order> emptyPage = new PageImpl<>(List.of());
            when(orderRepository.findByUserIdAndIsRemovedFalse(eq("user-456"), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<Order> result = orderService.getOrdersByUserId("user-456", 0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class FindLatestPendingOrderTests {

        @Test
        @DisplayName("Should find latest pending order")
        void testFindLatestPendingOrderSuccess() {
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.of(testOrder));

            Optional<Order> result = orderService.findLatestPendingOrder("user-456");

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Should return empty when no pending order exists")
        void testFindLatestPendingOrderNotFound() {
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.empty());

            Optional<Order> result = orderService.findLatestPendingOrder("user-456");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update order status successfully")
        void testUpdateOrderStatusSuccess() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order updated = orderService.updateOrderStatus("order-123", OrderStatus.SHIPPING);

            assertThat(updated.getStatus()).isEqualTo(OrderStatus.SHIPPING);
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void testUpdateOrderStatusNotFound() {
            when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus("nonexistent", OrderStatus.SHIPPING))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order successfully")
        void testCancelOrderSuccess() {
            testOrder.setStatus(OrderStatus.SHIPPING);
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            orderService.cancelOrder("order-123");

            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void testCancelOrderNotFound() {
            when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder("nonexistent"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ============================================================================
    // REDO ORDER TESTS (Stock Availability Check)
    // ============================================================================

    @Nested
    class RedoOrderTests {

        private ProductInventoryClient.ProductDetail mockProductDetail;

        @BeforeEach
        void setUpProductDetail() {
            mockProductDetail = new ProductInventoryClient.ProductDetail();
            mockProductDetail.setProductId("prod-123");
            mockProductDetail.setName("Test Product");
            mockProductDetail.setQuantity(10);
            mockProductDetail.setPrice(29.99);
        }

        @Test
        @DisplayName("Should redo order with all items in stock")
        void testRedoOrderAllItemsInStock() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId("new-order-id");
                return order;
            });

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getMessage()).isEqualTo("All items successfully added to cart");
            assertThat(response.getOrder()).isNotNull();
            assertThat(response.getOutOfStockProducts()).isEmpty();
            assertThat(response.getPartiallyFilledProducts()).isEmpty();
        }

        @Test
        @DisplayName("Should handle out of stock products")
        void testRedoOrderOutOfStock() {
            mockProductDetail.setQuantity(0);
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getMessage())
                    .isEqualTo("No items could be added to cart. All products are out of stock.");
            assertThat(response.getOrder()).isNull();
            assertThat(response.getOutOfStockProducts()).hasSize(1);
            assertThat(response.getOutOfStockProducts().get(0)).contains("out of stock");
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should handle partially available stock")
        void testRedoOrderPartialStock() {
            mockProductDetail.setQuantity(1);
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getMessage()).isEqualTo("Some items could not be fully added to cart");
            assertThat(response.getOrder()).isNotNull();
            assertThat(response.getPartiallyFilledProducts()).hasSize(1);
            assertThat(response.getPartiallyFilledProducts().get(0)).contains("only 1 available");
        }

        @Test
        @DisplayName("Should add items to existing pending order")
        void testRedoOrderAddToExistingCart() {
            Order existingPendingOrder = Order.builder()
                    .id("existing-cart")
                    .userId("user-456")
                    .status(OrderStatus.PENDING)
                    .items(new ArrayList<>())
                    .build();

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.of(existingPendingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getOrder()).isNotNull();
            assertThat(response.getOrder().getId()).isEqualTo("existing-cart");
            assertThat(response.getOrder().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Should merge items with same productId in existing cart")
        void testRedoOrderMergeItems() {
            OrderItem existingItem = new OrderItem("prod-123", 3, new BigDecimal("29.99"), "seller-456",
                    "Test Product", null);
            Order existingPendingOrder = Order.builder()
                    .id("existing-cart")
                    .userId("user-456")
                    .status(OrderStatus.PENDING)
                    .items(new ArrayList<>(List.of(existingItem)))
                    .build();

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.of(existingPendingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getOrder().getItems()).hasSize(1);
            assertThat(response.getOrder().getItems().get(0).getQuantity()).isEqualTo(5); // 3 + 2
        }

        @Test
        @DisplayName("Should handle product service exception")
        void testRedoOrderProductServiceException() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123"))
                    .thenThrow(new RuntimeException("Service unavailable"));

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getMessage())
                    .isEqualTo("No items could be added to cart. All products are out of stock.");
            assertThat(response.getOutOfStockProducts()).hasSize(1);
            assertThat(response.getOutOfStockProducts().get(0)).contains("could not be verified");
        }

        @Test
        @DisplayName("Should throw exception when original order not found")
        void testRedoOrderNotFound() {
            when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.redoOrder("nonexistent"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ============================================================================
    // ORDER ITEM MANAGEMENT TESTS
    // ============================================================================

    @Nested
    class AddItemToOrderTests {

        @Test
        @DisplayName("Should add new item to order")
        void testAddNewItemToOrder() {
            testOrder.setItems(new ArrayList<>());
            OrderItem newItem = new OrderItem("prod-789", 1, new BigDecimal("19.99"), "seller-789", "New Product",
                    null);

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.addItemToOrder("order-123", newItem);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductId()).isEqualTo("prod-789");
        }

        @Test
        @DisplayName("Should merge quantity when adding existing product")
        void testAddExistingItemMergesQuantity() {
            OrderItem additionalItem = new OrderItem("prod-123", 3, new BigDecimal("29.99"), "seller-456",
                    "Test Product", null);

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.addItemToOrder("order-123", additionalItem);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5); // 2 + 3
        }

        @Test
        @DisplayName("Should throw exception when order is not PENDING")
        void testAddItemToNonPendingOrder() {
            testOrder.setStatus(OrderStatus.SHIPPING);
            OrderItem newItem = new OrderItem("prod-789", 1, null, null, null, null);

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.addItemToOrder("order-123", newItem))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot modify order");
        }
    }

    @Nested
    class UpdateOrderItemTests {

        @Test
        @DisplayName("Should update order item successfully")
        void testUpdateOrderItemSuccess() {
            OrderItem updatedItem = new OrderItem("prod-123", 5, new BigDecimal("29.99"), "seller-456", "Test Product",
                    null);

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.updateOrderItem("order-123", "prod-123", updatedItem);

            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw exception when product not in order")
        void testUpdateOrderItemNotFound() {
            OrderItem updatedItem = new OrderItem("nonexistent", 5, null, null, null, null);

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateOrderItem("order-123", "nonexistent", updatedItem))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found in order");
        }

        @Test
        @DisplayName("Should throw exception when order is not PENDING")
        void testUpdateItemInNonPendingOrder() {
            testOrder.setStatus(OrderStatus.DELIVERED);
            OrderItem updatedItem = new OrderItem("prod-123", 5, null, null, null, null);

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateOrderItem("order-123", "prod-123", updatedItem))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class RemoveItemFromOrderTests {

        @Test
        @DisplayName("Should remove item from order successfully")
        void testRemoveItemSuccess() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.removeItemFromOrder("order-123", "prod-123");

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when product not in order")
        void testRemoveItemNotFound() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.removeItemFromOrder("order-123", "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found in order");
        }
    }

    @Nested
    class ClearOrderItemsTests {

        @Test
        @DisplayName("Should clear all items from order")
        void testClearOrderItemsSuccess() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.clearOrderItems("order-123");

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when order is not PENDING")
        void testClearItemsInNonPendingOrder() {
            testOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.clearOrderItems("order-123"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================================
    // CHECKOUT TESTS
    // ============================================================================

    @Nested
    class CheckoutOrderTests {

        private CheckoutRequest checkoutRequest;

        @BeforeEach
        void setUpCheckout() {
            checkoutRequest = new CheckoutRequest();
            checkoutRequest.setShippingAddress("456 New Address");
            checkoutRequest.setPaymentMethod(PaymentMethod.PAY_ON_DELIVERY);
        }

        @Test
        @DisplayName("Should checkout order with PAY_ON_DELIVERY successfully")
        void testCheckoutWithPayOnDelivery() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.checkoutOrder("order-123", checkoutRequest);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPING);
            assertThat(result.getShippingAddress()).isEqualTo("456 New Address");
            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAY_ON_DELIVERY);
            verify(productInventoryClient).decreaseStock(testOrder.getItems());
            verify(orderStatusScheduler).schedulePostCheckoutUpdate("order-123");
        }

        @Test
        @DisplayName("Should throw exception when order is not PENDING")
        void testCheckoutNonPendingOrder() {
            testOrder.setStatus(OrderStatus.SHIPPING);
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.checkoutOrder("order-123", checkoutRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending orders can be checked out");
        }

        @Test
        @DisplayName("Should throw exception when order is empty")
        void testCheckoutEmptyOrder() {
            testOrder.setItems(new ArrayList<>());
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.checkoutOrder("order-123", checkoutRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot checkout an empty order");
        }

        @Test
        @DisplayName("Should create new cart after successful checkout")
        void testCheckoutCreatesNewCart() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            orderService.checkoutOrder("order-123", checkoutRequest);

            // should save twice: once for checkout, once for new cart
            verify(orderRepository, times(2)).save(any(Order.class));
        }
    }

    // ============================================================================
    // UTILITY METHOD TESTS
    // ============================================================================

    @Nested
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should get all orders")
        void testGetAllOrders() {
            when(orderRepository.findAll()).thenReturn(List.of(testOrder));

            List<Order> result = orderService.getAllOrders();

            assertThat(result).hasSize(1);
            verify(orderRepository).findAll();
        }
    }
}