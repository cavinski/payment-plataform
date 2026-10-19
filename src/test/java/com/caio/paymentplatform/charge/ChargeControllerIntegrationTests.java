package com.caio.paymentplatform.charge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest 
@AutoConfigureMockMvc 
class ChargeControllerIntegrationTests {
    
    @Autowired 
    private MockMvc mockMvc;

    @Autowired 
    private ChargeRepository chargeRepository;

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
}