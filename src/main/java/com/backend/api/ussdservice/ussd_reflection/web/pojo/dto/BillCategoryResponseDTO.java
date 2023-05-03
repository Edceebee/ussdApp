package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AirtimeDataBillerData;
import lombok.Data;

import java.util.List;

@Data
public class BillCategoryResponseDTO
{
    private List<AirtimeDataBillerData> data;
    private String message;
    private boolean status;
    private String timestamp;
}
