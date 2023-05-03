package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankCodes {

    private String status;
    private String message;
    private List<BankData> data;
}
