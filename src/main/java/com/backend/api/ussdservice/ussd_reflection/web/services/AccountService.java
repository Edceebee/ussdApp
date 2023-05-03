package com.backend.api.ussdservice.ussd_reflection.web.services;

import com.backend.api.ussdservice.ussd_reflection.web.WebClient;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.AccountBalanceResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.AccountNumbersResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.UserInfoResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * This endpoint encapsulates all account related functionality related to accounts.
 */
@Service
@Slf4j
public class AccountService
{

    @Autowired
    private Environment env;

    @Autowired
    private CustomerService customerService;
    private static final Gson gson = new Gson();

    public AccountNumbersResponseDTO getAccountNumbersForCustomer(String phoneNumber){
        UserInfoResponseDTO responseDTO = customerService.getCustomerDetailsByPhoneNumber(phoneNumber);
        if(responseDTO == null)
            return null;
        String userId = String.valueOf(responseDTO.getData().getUserId());
        log.info("User Id: {}", userId);
        String fullUrl = env.getProperty("wayabank.apiPaths.customerAccountsBase").concat("/").concat(userId);
        log.info("Full URL: {}", fullUrl);
        String responseJson = WebClient.getForObject(fullUrl, null, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, AccountNumbersResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    public AccountBalanceResponseDTO getCustomerAccountBalanceByAccountNumber(String accountNumber, String pin){
        final String url = env.getProperty("wayabank.apiPaths.accountBalanceCheck").concat("/").concat(accountNumber);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("pin", pin);
        log.info("Balance PIN: {}", pin);
        String responseJson = WebClient.getForObject(url, headers, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, AccountBalanceResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    private void logResponseFromWayaPay(String responseJson){
        log.info("Response from WayaPay: {}", responseJson);
    }

}
