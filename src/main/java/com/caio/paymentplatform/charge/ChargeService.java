package com.caio.paymentplatform.charge;

import com.caio.paymentplatform.charge.dto.ChargeResponse;
import com.caio.paymentplatform.charge.dto.CreateChargeRequest;
import com.caio.paymentplatform.charge.exceptions.ChargeNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service 
public class ChargeService {
    
    private final ChargeRepository chargeRepository;

    public ChargeService(ChargeRepository chargeRepository) {
        this.chargeRepository = chargeRepository;
    }

    @Transactional 
    public ChargeResponse create(CreateChargeRequest request) {

        Charge charge = Charge.create(request.amount(), request.description());

        Charge savedCharge = chargeRepository.save(charge);

        return ChargeResponse.from(savedCharge);
    }

    @Transactional(readOnly = true)
    public ChargeResponse findById(UUID id) {

        Charge charge = chargeRepository.findById(id)
            .orElseThrow(() -> new ChargeNotFoundException(id));

        return ChargeResponse.from(charge);
    }
}