package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import lombok.Data;

@Data
public class AirtimeDataRequestDTO
{
    private String phoneNumber;
    private String amount;
    private String categoryId;
    private String billerId;
    private String accountNumber;
    private String pin;
}
