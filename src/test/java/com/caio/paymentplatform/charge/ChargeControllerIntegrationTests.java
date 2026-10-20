package com.caio.paymentplatform.charge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.UUID;
import com.caio.paymentplatform.charge.dto.ChargeResponse;
import com.caio.paymentplatform.charge.dto.CreateChargeRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers 
@SpringBootTest 
@AutoConfigureMockMvc 
class ChargeControllerIntegrationTests {
    
    @Autowired 
    private MockMvc mockMvc;

    @Autowired 
    private ChargeRepository chargeRepository;

    @Autowired
    private ChargeService chargeService;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @BeforeEach 
    void cleanDatabase() {
        chargeRepository.deleteAll();
    }

    @Test
    void shouldCreateChargeAndReturnCreated() throws Exception {

        String request = """
                {
                    "amount": 149.90,
                    "description": "Portfolio integration test"
                }
                """;

        mockMvc.perform(post("/api/v1/charges")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.amount").value(149.90))
            .andExpect(jsonPath("$.currency").value("BRL"))
            .andExpect(jsonPath("$.description")
            .value("Portfolio integration test"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void shouldRetrieveExistingCharge() throws Exception {

        String request = """
                {
                    "amount": 149.90,
                    "description": "Charge to retrieve"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/charges")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = JsonMapper.builder().build().readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/charges/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.amount").value(149.90))
            .andExpect(jsonPath("$.currency").value("BRL"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundWhenChargeDoesNotExist() throws Exception {

        mockMvc.perform(get("/api/v1/charges/01906b83-86b6-44b7-b489-82f7d541c979"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CHARGE_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonPositiveAmount() throws Exception {

        String request = """
                {
                    "amount": 0,
                    "description": "Invalid charge"
                }
                """;

        mockMvc.perform(post("/api/v1/charges")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {

        String request = """
                {
                    "description": "Charge without amount"
                }
                """;

        mockMvc.perform(post("/api/v1/charges")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() throws Exception {

        String request = """
                {
                    "amount": 10.123,
                    "description": "Invalid precision"
                }
                """;

        mockMvc.perform(post("/api/v1/charges")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDescriptionLongerThan255Characters() throws Exception {

        String description = "a".repeat(256);
        String request = """
                {
                    "amount": 10.00,
                    "description": "%s"
                }
                """.formatted(description);

        mockMvc.perform(post("/api/v1/charges")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPayChargeAndReturnUpdatedStatus() throws Exception {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge to pay through API"
            )
        );

        mockMvc.perform(post("/api/v1/charges/{id}/pay", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(created.id().toString()))
            .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/v1/charges/{id}", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void shouldReturnConflictWhenCancellingPaidCharge() throws Exception {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Already paid charge"
            )
        );

        chargeService.markAsPaid(created.id());

        mockMvc.perform(post("/api/v1/charges/{id}/cancel", created.id()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code")
            .value("INVALID_CHARGE_STATE_TRANSITION"));

        mockMvc.perform(get("/api/v1/charges/{id}", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void shouldReturnNotFoundWhenPayingNonexistentCharge() throws Exception {
        
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/charges/{id}/pay", unknownId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CHARGE_NOT_FOUND"));
    }

    @Test
    void shouldCancelPendingChargeThroughApi() throws Exception {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge to cancel through API"
            )
        );

        mockMvc.perform(post("/api/v1/charges/{id}/cancel", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/charges/{id}", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldExpirePendingChargeThroughApi() throws Exception {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge to expire through API"
            )
        );

        mockMvc.perform(post("/api/v1/charges/{id}/expire", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXPIRED"));

        mockMvc.perform(get("/api/v1/charges/{id}", created.id()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXPIRED"));
    }
}