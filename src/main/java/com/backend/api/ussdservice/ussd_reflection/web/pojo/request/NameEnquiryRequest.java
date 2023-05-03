package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NameEnquiryRequest {

    private String accountNumber;
    private String bankCode;
}
