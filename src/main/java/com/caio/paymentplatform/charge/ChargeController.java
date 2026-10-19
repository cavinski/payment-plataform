package com.caio.paymentplatform.charge;

import com.caio.paymentplatform.charge.dto.ChargeResponse;
import com.caio.paymentplatform.charge.dto.CreateChargeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController 
@RequestMapping("/api/v1/charges")
public class ChargeController {
 
    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @PostMapping 
    public ResponseEntity<ChargeResponse> create(@Valid @RequestBody CreateChargeRequest request) {

        ChargeResponse response = chargeService.create(request);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargeResponse> findById(@PathVariable UUID id) {

        return ResponseEntity.ok(chargeService.findById(id));
    }
}