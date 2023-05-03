package com.backend.api.ussdservice.ussd_reflection.web.services;

import com.backend.api.ussdservice.ussd_reflection.web.WebClient;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.UserInfoResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.UserCreationRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.UserEmailOrPhoneVerifyRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.GenericResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.UserCreationResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.WayaPayUserWebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 *  This service encapsulates all customer related functionality for USSD
 */
@Service
@Slf4j
public class CustomerService
{
    @Autowired
    private Environment env;

    private static final Gson gson = new Gson();

    public WayaPayUserWebResponse authenticateCustomer(String phoneNumber){
        final String baseUrl = env.getProperty("wayabank.apiPaths.authCustomer");
        String responseJson = WebClient.getForObject(baseUrl.concat("/".concat(phoneNumber)), null, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, WayaPayUserWebResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public UserInfoResponseDTO getCustomerDetailsByPhoneNumber(String phoneNumber) {
        String fullUrl = env.getProperty("wayabank.apiPaths.customerDetailsByPhone").concat("/").concat(phoneNumber);
        String responseJson = WebClient.getForObject(fullUrl, null, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, UserInfoResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    public UserCreationResponseDTO processNewPersonalUserCreation(UserCreationRequestDTO requestDTO){
        String url = env.getProperty("wayabank.apiPaths.createNewCustomer");
        String responseJsonPost = WebClient.postForObject(requestDTO, url, null, null);
        try{
            return gson.fromJson(responseJsonPost, UserCreationResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    public GenericResponseDTO verifyNewCustomerEmailOrPhone(UserEmailOrPhoneVerifyRequestDTO requestDTO){
        String emailOrPhone = requestDTO.getPhoneOrEmail();
        String url = "";
        if(emailOrPhone.contains("@")){
            url = env.getProperty("wayabank.apiPaths.verifyEmail");
        }else{
            url = env.getProperty("wayabank.apiPaths.verifyOtp");
        }
        String responseJsonPost = WebClient.postForObject(requestDTO, url, null, null);
        try{
            return gson.fromJson(responseJsonPost, GenericResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    private void logResponseFromWayaPay(String responseJson){
        log.info("Response from WayaPay: {}", responseJson);
    }

}
