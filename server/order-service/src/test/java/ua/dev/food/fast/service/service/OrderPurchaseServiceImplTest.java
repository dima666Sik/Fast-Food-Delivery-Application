package ua.dev.food.fast.service.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ua.dev.feign.clients.ProductFeignClient;
import ua.dev.feign.clients.ProductPurchaseDto;
import ua.dev.feign.clients.ProductResponseDto;
import ua.dev.food.fast.service.domain.dto.request.OrderPurchaseRequest;
import ua.dev.food.fast.service.domain.dto.request.PurchaseItemRequest;
import ua.dev.food.fast.service.domain.dto.response.OrderPurchaseResponse;
import ua.dev.food.fast.service.domain.model.*;
import ua.dev.food.fast.service.repository.*;
import ua.dev.jwt.service.JwtDecodeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.*;

@SpringBootTest
class OrderPurchaseServiceImplTest {
    @InjectMocks
    private OrderPurchaseServiceImpl orderPurchaseService;

    @Mock
    private AddressOrderRepository addressOrderRepository;

    @Mock
    private BasicOrderRepository basicOrderRepository;

    @Mock
    private BasicOrderUserRepository basicOrderUserRepository;

    @Mock
    private BasicOrderGuestRepository basicOrderGuestRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private JwtDecodeService jwtDecodeService;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private StatusOrderRepository statusOrderRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private ProcessingOrderRepository processingOrderRepository;

    @Test
    void addOrderWithPurchaseUserShouldSaveOrderSuccessfully() {
        // Arrange
        String authHeader = "Bearer token";
        OrderPurchaseRequest orderRequest = new OrderPurchaseRequest();
        orderRequest.setPurchaseItems(List.of(new PurchaseItemRequest(1L, new BigDecimal(2), 50)));
        orderRequest.setDelivery(false);

        Long userId = 1L;
        AddressOrder addressOrder = new AddressOrder();
        BasicOrder basicOrder = new BasicOrder();
        basicOrder.setId(10L);
        StatusOrder statusOrder = StatusOrder.builder().statusId(1L).orderId(basicOrder.getId()).id(1L).build();

        ProductResponseDto product = new ProductResponseDto(100L, "Product", new BigDecimal(100),
            110L, null, null, null, "Category", null);

        when(jwtDecodeService.extractUserDataFromBearerHeader(authHeader)).thenReturn(userId);
        when(addressOrderRepository.save(any(AddressOrder.class))).thenReturn(Mono.just(addressOrder));
        when(basicOrderRepository.save(any(BasicOrder.class))).thenReturn(Mono.just(basicOrder));
        when(basicOrderUserRepository.save(any(BasicOrderUser.class))).thenReturn(Mono.just(new BasicOrderUser()));
        when(productFeignClient.getProductById(anyLong())).thenReturn(Mono.just(product));
        when(statusOrderRepository.findByOrderId(anyLong())).thenReturn(Mono.just(statusOrder));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(Mono.just(new Purchase()));
        when(statusRepository.findStatusByState(anyString())).thenReturn(Mono.just(new Status()));
        when(statusOrderRepository.save(any(StatusOrder.class))).thenReturn(Mono.just(new StatusOrder()));

        // Act
        Mono<Void> result = orderPurchaseService.addOrderWithPurchaseUser(authHeader, orderRequest);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        verify(jwtDecodeService).extractUserDataFromBearerHeader(authHeader);
        verify(basicOrderRepository).save(any(BasicOrder.class));
        verify(basicOrderUserRepository).save(any(BasicOrderUser.class));
        verify(statusRepository).findStatusByState(anyString());
        verify(statusOrderRepository).save(any(StatusOrder.class));
    }

    @Test
    void getGuestPurchasesShouldReturnPurchaseDetails() {
        // Arrange
        Long orderGuestId = 1L;
        Purchase purchase = new Purchase();
        purchase.setProductId(100L);
        purchase.setQuantity(2);
        purchase.setTotalPrice(new BigDecimal(200));

        ProductResponseDto product = new ProductResponseDto(100L, "Product", new BigDecimal(100),
            110L, null, null, null, "Category", null);

        when(purchaseRepository.findByBasicOrderGuestId(orderGuestId)).thenReturn(Flux.just(purchase));
        when(productFeignClient.getProductById(100L)).thenReturn(Mono.just(product));

        // Act
        Flux<ProductPurchaseDto> result = orderPurchaseService.getGuestPurchases(orderGuestId);

        // Assert
        StepVerifier.create(result)
            .expectNextMatches(dto -> dto.id()
                .equals(100L) && dto.quantity() == 2 && Objects.equals(dto.totalPrice(), new BigDecimal(200)))
            .verifyComplete();

        verify(purchaseRepository).findByBasicOrderGuestId(orderGuestId);
        verify(productFeignClient).getProductById(100L);
    }

    @Test
    void getAllOrderShouldReturnOrders() {
        // Arrange
        BasicOrder basicOrder = new BasicOrder();
        basicOrder.setId(1L);

        BasicOrderUser basicOrderUser = new BasicOrderUser();
        basicOrderUser.setBasicOrderId(1L);

        Purchase purchase = Purchase.builder().id(1L).basicOrderUserId(1L).productId(1L).build();
        ProductResponseDto product = ProductResponseDto.builder()
            .id(1L)
            .title("Test")
            .category("Burger")
            .image03(null)
            .image01(null)
            .image02(null)
            .price(new BigDecimal(10))
            .likes(10L)
            .build();

        StatusOrder statusOrder = StatusOrder.builder().statusId(1L).orderId(basicOrder.getId()).id(1L).build();
        Status status = Status.builder().id(1L).state(State.PENDING).build();

        when(basicOrderRepository.findAll()).thenReturn(Flux.just(basicOrder));
        when(basicOrderUserRepository.findByBasicOrderId(anyLong())).thenReturn(Mono.just(basicOrderUser));
        when(purchaseRepository.findByBasicOrderUserId(anyLong())).thenReturn(Flux.just(purchase));
        when(productFeignClient.getProductById(anyLong())).thenReturn(Mono.just(product));
        when(statusOrderRepository.findByOrderId(anyLong())).thenReturn(Mono.just(statusOrder));
        when(statusRepository.findById(anyLong())).thenReturn(Mono.just(status));
        // Act
        Flux<OrderPurchaseResponse> result = orderPurchaseService.getAllOrder();

        // Assert
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();

        verify(basicOrderRepository).findAll();
        verify(basicOrderUserRepository).findByBasicOrderId(1L);
    }

    @Test
    void orderAcceptanceShouldUpdateOrderStatus() {
        // Arrange
        String authHeader = "Bearer token";
        List<Long> orderIds = List.of(1L, 2L);
        Status status = Status.builder().id(1L).state(State.IN_TRANSIT).build();
        StatusOrder statusOrder = StatusOrder.builder().statusId(1L).orderId(1L).id(1L).build();

        Long courierId = 123L;

        when(jwtDecodeService.extractUserDataFromBearerHeader(authHeader)).thenReturn(courierId);
        when(statusRepository.findStatusByState(status.getState().name())).thenReturn(Mono.just(status));
        when(statusOrderRepository.findByOrderId(anyLong())).thenReturn(Mono.just(statusOrder));
        when(statusOrderRepository.save(any(StatusOrder.class))).thenReturn(Mono.just(statusOrder));
        when(processingOrderRepository.save(any(ProcessingOrder.class))).thenReturn(Mono.just(new ProcessingOrder()));

        // Act
        Mono<Void> result = orderPurchaseService.orderAcceptance(authHeader, orderIds, status.getState().name());

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        verify(jwtDecodeService).extractUserDataFromBearerHeader(authHeader);
        verify(statusRepository, times(orderIds.size())).findStatusByState(status.getState().name());
        verify(statusOrderRepository, times(orderIds.size())).save(any(StatusOrder.class));
        verify(processingOrderRepository, times(orderIds.size())).save(any(ProcessingOrder.class));
    }
}