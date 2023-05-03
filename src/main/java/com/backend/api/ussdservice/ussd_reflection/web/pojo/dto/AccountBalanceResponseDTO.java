package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AccountBalanceResponseData;
import lombok.Data;

@Data
public class AccountBalanceResponseDTO
{
    private String message;
    private boolean status;
    private String timeStamp;
    private AccountBalanceResponseData data;
}
