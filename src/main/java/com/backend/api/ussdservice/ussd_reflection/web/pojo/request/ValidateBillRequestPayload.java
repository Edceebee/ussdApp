package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class ValidateBillRequestPayload
{
    private String amount;
    private String billerId;
    private String categoryId;
    private List<ValidateBillDataParam> data;
    private String sourceWalletAccountNumber;
}
