package com.backend.api.ussdservice.ussd_reflection.web.pojo.dto;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.BankData;
import lombok.Data;

import java.util.List;

@Data
public class BankAndIndex
{
    List<BankData> bankData;
    List<Integer> indices;

    public BankAndIndex(List<BankData> bankData, List<Integer> indices) {
        this.bankData = bankData;
        this.indices = indices;
    }
}
