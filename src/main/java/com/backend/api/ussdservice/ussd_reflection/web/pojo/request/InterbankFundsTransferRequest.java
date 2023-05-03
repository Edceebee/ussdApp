package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterbankFundsTransferRequest
{
    private String amount;
    private String bankCode;
    private String bankName;
    private String crAccount;
    private String crAccountName;
    private String debitAccountName;
    private String narration;
    private boolean saveBen;
    private String transRef;
    private String transactionPin;
    private String userId;
    private String walletAccountNo;
}
