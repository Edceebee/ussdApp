package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import lombok.Data;

@Data
public class CableTvRequestDTO
{
    private String phoneNumber;
    private String amount;
    private String categoryId;
    private String billerId;
    private String accountNumber;
    private String plan;
    private String pin;
}
