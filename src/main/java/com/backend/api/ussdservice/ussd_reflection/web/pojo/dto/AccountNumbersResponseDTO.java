package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AccountNumbersResponse;
import lombok.Data;

import java.util.List;

@Data
public class AccountNumbersResponseDTO
{
    private boolean status;
    private String message;
    private long timestamp;
    private List<AccountNumbersResponse> data;
}
