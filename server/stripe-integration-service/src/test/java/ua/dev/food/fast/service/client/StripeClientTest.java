package ua.dev.food.fast.service.client;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import ua.dev.food.fast.service.exception.PaymentProcessingException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeClientTest {

    @Mock
    private StripeApiWrapper stripeApiWrapper;

    @InjectMocks
    private StripeClient stripeClient;

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.publishable-key}")
    private String publishableKey;


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeClient, "secretKey", secretKey);
        ReflectionTestUtils.setField(stripeClient, "publishableKey", publishableKey);

        stripeClient.init();
    }

    @Test
    void testCreatePaymentIntentSuccess() throws StripeException {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 100L);

        PaymentIntent mockPaymentIntent = mock(PaymentIntent.class);
        when(mockPaymentIntent.getClientSecret()).thenReturn("test_client_secret");
        when(stripeApiWrapper.createPaymentIntent(any(PaymentIntentCreateParams.class))).thenReturn(mockPaymentIntent);

        // Act
        Map<String, String> result = stripeClient.createPaymentIntent(request);

        // Assert
        assertNotNull(result);
        assertEquals("test_client_secret", result.get("client_secret"));
        verify(stripeApiWrapper, times(1)).createPaymentIntent(any(PaymentIntentCreateParams.class));
    }

    @Test
    void testCreatePaymentIntentException() throws StripeException {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 100L);


        when(stripeApiWrapper.createPaymentIntent(any(PaymentIntentCreateParams.class))).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            stripeClient.createPaymentIntent(request));

        assertEquals("Unexpected error while processing payment", exception.getMessage());
        verify(stripeApiWrapper, times(1)).createPaymentIntent(any(PaymentIntentCreateParams.class));
    }

    @Test
    void testGetPublishableKey() {
        // Act
        Map<String, String> result = stripeClient.getPublishableKey();

        // Assert
        assertNotNull(result);
        assertEquals(publishableKey, result.get("publishableKey"));
    }
}
