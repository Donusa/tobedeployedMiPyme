package com.mipyme.mercadolibre.service;

import com.mipyme.mercadolibre.dto.MercadoLibreOrderSummaryDTO;
import com.mipyme.mercadolibre.model.MercadoLibreConfig;
import com.mipyme.mercadolibre.repository.MercadoLibreConfigRepository;
import com.mipyme.orders.model.LocalOrderTracking;
import com.mipyme.orders.repository.LocalOrderTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MercadoLibreServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LocalOrderTrackingRepository localOrderTrackingRepository;

    @Mock
    private MercadoLibreConfigRepository configRepository;

    @InjectMocks
    private MercadoLibreService mercadoLibreService;

    @BeforeEach
    public void setup() throws Exception {

        Field restTemplateField = MercadoLibreService.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(mercadoLibreService, restTemplate);


        MercadoLibreConfig config = new MercadoLibreConfig();
        config.setAccessToken("test-token");
        config.setUserId(123456L);
        config.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(configRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(config));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetOrdersWithLocalOverride() {

        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, Object>> mockOrders = new ArrayList<>();
        Map<String, Object> order1 = new HashMap<>();
        order1.put("id", 2000003456L);
        order1.put("status", "paid");
        order1.put("date_created", "2023-01-01T00:00:00.000-04:00");
        mockOrders.add(order1);
        responseBody.put("results", mockOrders);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(responseEntity);


        LocalOrderTracking tracking = new LocalOrderTracking();
        tracking.setExternalId("2000003456");
        tracking.setSource("mercadolibre");
        tracking.setLocalStatus("PACKED");

        when(localOrderTrackingRepository.findByExternalIdInAndSource(anyList(), eq("mercadolibre")))
                .thenReturn(Collections.singletonList(tracking));


        List<MercadoLibreOrderSummaryDTO> result = mercadoLibreService.getOrders();


        assertEquals(1, result.size());
        assertEquals("PACKED", result.get(0).getOrderStatus());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetOrdersWithDoubleId() {


        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, Object>> mockOrders = new ArrayList<>();
        Map<String, Object> order1 = new HashMap<>();
        order1.put("id", 2000003456.0);
        order1.put("status", "paid");
        order1.put("date_created", "2023-01-01T00:00:00.000-04:00");
        mockOrders.add(order1);
        responseBody.put("results", mockOrders);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(responseEntity);


        LocalOrderTracking tracking = new LocalOrderTracking();
        tracking.setExternalId("2000003456");
        tracking.setSource("mercadolibre");
        tracking.setLocalStatus("PACKED");











        when(localOrderTrackingRepository.findByExternalIdInAndSource(eq(Collections.singletonList("2000003456")),
                eq("mercadolibre")))
                .thenReturn(Collections.singletonList(tracking));


        List<MercadoLibreOrderSummaryDTO> result = mercadoLibreService.getOrders();


        assertEquals(1, result.size());
        assertEquals("PACKED", result.get(0).getOrderStatus(), "Status should be overridden correctly now");
    }
}
