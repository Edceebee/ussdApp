package com.backend.api.ussdservice.ussd_reflection.constants;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ChargeEventId {

    @Value("${eventId.transaction}")
    public  String TRANSFER_AND_TRANSACTION_FEE_EVENT_ID;

    @Value("${eventId.airtimeAndDataQuickTeller}")
    public String AIRTIME_DATA_EVENT_ID_QUICK_TELLER;

    @Value("${eventId.airtimeAndDataBaxi}")
    public String AIRTIME_DATA_EVENT_ID_BAXI;

    @Value("${eventId.airtimeAndDataItex}")
    public String AIRTIME_DATA_EVENT_ID_ITEX;



}
