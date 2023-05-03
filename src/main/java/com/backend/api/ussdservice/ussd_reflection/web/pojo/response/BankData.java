package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankData {
    private String bankName;
    private String bankCode;
}
