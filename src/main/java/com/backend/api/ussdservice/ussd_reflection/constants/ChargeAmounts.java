package com.backend.api.ussdservice.ussd_reflection.constants;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

public class ChargeAmounts {

    @Value("${ussd.service.charge}")
    public static String USSD_SERVICE_CHARGE;
}
