package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class AirtimeBillerResponse
{
    private boolean status;
    private String timeStamp;
    private String message;
    private List<AirtimeDataBillerData> data;
}
