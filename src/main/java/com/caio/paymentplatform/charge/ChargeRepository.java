package com.caio.paymentplatform.charge;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  ChargeRepository extends JpaRepository<Charge, UUID>{
    
}
