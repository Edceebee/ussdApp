package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FundsTransferDTO
{
    private String sourceAccount;
    private String sourcePhoneNumber;
    private String beneficiaryAccount;
    private String beneficiaryName;
    private String amount;
    private String bankName;
    private String pin;
}
