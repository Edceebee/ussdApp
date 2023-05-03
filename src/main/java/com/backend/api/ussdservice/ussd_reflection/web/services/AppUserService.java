package com.backend.api.ussdservice.ussd_reflection.web.services;

import com.backend.api.ussdservice.ussd_reflection.web.WebClient;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.AppUserHandshakeRequest;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AppUserHandshakeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppUserService
{
    @Autowired
    private Environment env;

    private static final Gson gson = new Gson();

    public AppUserHandshakeResponse getAppUserCredentials(){
        String url = env.getProperty("wayabank.apiPaths.appUserAuth");

        AppUserHandshakeRequest request = new AppUserHandshakeRequest();
        request.setOtp("");
        request.setPassword(env.getProperty("user.password"));
        request.setEmailOrPhoneNumber(env.getProperty("user.emailOrPhone"));

        String responseJsonPost = WebClient.postForObject(request, url, null, null);
        try{
            return gson.fromJson(responseJsonPost, AppUserHandshakeResponse.class);
        }catch (Exception e){
            return null;
        }
    }
}
