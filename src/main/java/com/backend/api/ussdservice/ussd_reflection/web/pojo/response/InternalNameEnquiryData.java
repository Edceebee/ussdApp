package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class InternalNameEnquiryData
{
    private Long id;

    @JsonProperty("accountNo")
    @SerializedName("accountNo")
    private String accountNumber;

    @JsonProperty("acct_name")
    @SerializedName("acct_name")
    private String accountName;

    private String defaultWalletBank = "WAYA MICROFINANCE BANK";
}
