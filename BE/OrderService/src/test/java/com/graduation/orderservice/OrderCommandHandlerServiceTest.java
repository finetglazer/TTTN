//package com.graduation.orderservice;
//
//import com.graduation.orderservice.constant.Constant;
//
//import com.graduation.orderservice.model.Order;
//import com.graduation.orderservice.repository.OrderRepository;
//import com.graduation.orderservice.service.OrderCommandHandlerService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.slf4j.Logger;
//
//import java.math.BigDecimal;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class OrderCommandHandlerServiceTest {
//
//    @Mock
//    private OrderRepository orderRepository;
//
//    @Mock
//    private Logger log;
//
//    @InjectMocks
//    private OrderCommandHandlerService orderCommandHandlerService;
//
//    // Test data
//    private static final String USER_ID = "user123";
//    private static final String USER_EMAIL = "test@example.com";
//    private static final String USER_NAME = "John Doe";
//    private static final String ORDER_DESCRIPTION = "Test order description";
//    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.valueOf(99.99);
//    private static final String SHIPPING_ADDRESS = "123 Test Street";
//    private static final String ORDER_ID = "123";
//
//    private Order mockOrder;
//
//    @BeforeEach
//    void setUp() {
//        mockOrder = mock(Order.class);
//        lenient().when(mockOrder.getId()).thenReturn(Long.valueOf(ORDER_ID));
//
//        // Replace the logger field in the service with our mock
//        try {
//            var logField = OrderCommandHandlerService.class.getDeclaredField("log");
//            logField.setAccessible(true);
//            logField.set(orderCommandHandlerService, log);
//        } catch (Exception e) {
//            // If logger field access fails, tests will still work but won't verify logging
//        }
//    }
//
//    @Test
//    void createOrder_SuccessfulCreation_ReturnsCreatedOrder() {
//        // Arrange
//        try (MockedStatic<Order> orderMock = mockStatic(Order.class)) {
//            orderMock.when(() -> Order.createOrder(
//                            USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS))
//                    .thenReturn(mockOrder);
//
//            when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
//
//            // Mock the publishOrderCreatedEvent method (assuming it exists in the service)
//            OrderCommandHandlerService spyService = spy(orderCommandHandlerService);
//            doNothing().when(spyService).publishOrderCreatedEvent(mockOrder);
//
//            // Act
//            Order result = spyService.createOrder(USER_ID, USER_EMAIL, USER_NAME,
//                    ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS);
//
//            // Assert
//            assertNotNull(result);
//            assertEquals(mockOrder, result);
//
//            // Verify method calls
//            orderMock.verify(() -> Order.createOrder(
//                    USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS));
//            verify(orderRepository).save(mockOrder);
//            verify(spyService).publishOrderCreatedEvent(mockOrder);
//        }
//    }
//
//    @Test
//    void createOrder_OrderCreationFails_ThrowsRuntimeException() {
//        // Arrange
//        String errorMessage = "Invalid order data";
//        RuntimeException originalException = new RuntimeException(errorMessage);
//
//        try (MockedStatic<Order> orderMock = mockStatic(Order.class)) {
//            orderMock.when(() -> Order.createOrder(
//                            USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS))
//                    .thenThrow(originalException);
//
//            // Act & Assert
//            RuntimeException exception = assertThrows(RuntimeException.class, () ->
//                    orderCommandHandlerService.createOrder(USER_ID, USER_EMAIL, USER_NAME,
//                            ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS));
//
//            // Verify exception details
//            assertTrue(exception.getMessage().contains(errorMessage));
//            assertEquals(originalException, exception.getCause());
//
//            // Verify repository save was never called
//            verify(orderRepository, never()).save(any());
//        }
//    }
//
//    @Test
//    void createOrder_RepositorySaveFails_ThrowsRuntimeException() {
//        // Arrange
//        String errorMessage = "Database connection failed";
//        RuntimeException repositoryException = new RuntimeException(errorMessage);
//
//        try (MockedStatic<Order> orderMock = mockStatic(Order.class)) {
//            orderMock.when(() -> Order.createOrder(
//                            USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS))
//                    .thenReturn(mockOrder);
//
//            when(orderRepository.save(mockOrder)).thenThrow(repositoryException);
//
//            // Act & Assert
//            RuntimeException exception = assertThrows(RuntimeException.class, () ->
//                    orderCommandHandlerService.createOrder(USER_ID, USER_EMAIL, USER_NAME,
//                            ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS));
//
//            // Verify exception details
//            assertTrue(exception.getMessage().contains(errorMessage));
//            assertEquals(repositoryException, exception.getCause());
//
//            // Verify order creation was called but success log was not
//            orderMock.verify(() -> Order.createOrder(
//                    USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS));
//            verify(log, never()).info(Constant.LOG_ORDER_CREATED_SUCCESS, ORDER_ID);
//        }
//    }
//
//    @Test
//    void createOrder_EventPublishingFails_ThrowsRuntimeException() {
//        // Arrange
//        String errorMessage = "Event publishing failed";
//        RuntimeException eventException = new RuntimeException(errorMessage);
//
//        try (MockedStatic<Order> orderMock = mockStatic(Order.class)) {
//            orderMock.when(() -> Order.createOrder(
//                            USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS))
//                    .thenReturn(mockOrder);
//
//            when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
//
//            OrderCommandHandlerService spyService = spy(orderCommandHandlerService);
//            doThrow(eventException).when(spyService).publishOrderCreatedEvent(mockOrder);
//
//            // Act & Assert
//            RuntimeException exception = assertThrows(RuntimeException.class, () ->
//                    spyService.createOrder(USER_ID, USER_EMAIL, USER_NAME,
//                            ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS));
//
//            // Verify exception details
//            assertTrue(exception.getMessage().contains(errorMessage));
//            assertEquals(eventException, exception.getCause());
//
//            // Verify all steps up to event publishing were executed
//            orderMock.verify(() -> Order.createOrder(
//                    USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, SHIPPING_ADDRESS));
//            verify(orderRepository).save(mockOrder);
//            verify(spyService).publishOrderCreatedEvent(mockOrder);
//        }
//    }
//
//    @Test
//    void createOrder_NullShippingAddress_HandledCorrectly() {
//        // Arrange
//        try (MockedStatic<Order> orderMock = mockStatic(Order.class)) {
//            orderMock.when(() -> Order.createOrder(
//                            USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, null))
//                    .thenReturn(mockOrder);
//
//            when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
//
//            OrderCommandHandlerService spyService = spy(orderCommandHandlerService);
//            doNothing().when(spyService).publishOrderCreatedEvent(mockOrder);
//
//            // Act
//            Order result = spyService.createOrder(USER_ID, USER_EMAIL, USER_NAME,
//                    ORDER_DESCRIPTION, TOTAL_AMOUNT, null);
//
//            // Assert
//            assertNotNull(result);
//            assertEquals(mockOrder, result);
//
//            // Verify null shipping address was passed correctly
//            orderMock.verify(() -> Order.createOrder(
//                    USER_ID, USER_EMAIL, USER_NAME, ORDER_DESCRIPTION, TOTAL_AMOUNT, null));
//        }
//    }
//}