package hu.zoltanb.projects.fraud.controller;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TransactionMockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FraudAppConfig config;

    @Mock
    private FraudAppConfig.Generator generator; // A belső osztály mockolása

    @InjectMocks
    private TransactionMockController controller;

    @BeforeEach
    void setUp() {
        // MockMvc in standalone mode
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(config.getGenerator()).thenReturn(generator);

        when(generator.getMinUserId()).thenReturn(1L);
        when(generator.getMaxUserId()).thenReturn(10L);
        when(generator.getMinMerchantId()).thenReturn(1L);
        when(generator.getMaxMerchantId()).thenReturn(10L);
        when(generator.getMinAmount()).thenReturn(100.0);
        when(generator.getMaxAmount()).thenReturn(200.0);
    }

    @Test
    void getRandomTransaction_ShouldReturnTransactionWithCorrectRanges() throws Exception {
        mockMvc.perform(get("/api/mock/transactions"))
                .andExpect(status().isOk())
                // check the numbers
                .andExpect(jsonPath("$.userId", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.merchantId", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.amount", allOf(greaterThanOrEqualTo(100.0), lessThanOrEqualTo(200.0))));

        // Check the generator and config calls
        verify(config).getGenerator();
        verify(generator, atLeastOnce()).getMinUserId();
    }
}