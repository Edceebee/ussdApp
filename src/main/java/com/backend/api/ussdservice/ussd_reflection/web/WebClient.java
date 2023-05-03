package com.backend.api.ussdservice.ussd_reflection.web;


import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.WayaPayWebResponse;
import kong.unirest.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * This class provides the functionality that handles the handshake between the USSD application and
 * the Waya-Bank existing microservice.
 */
public class WebClient
{
    private final static Gson gson = new Gson();

    @Value("${fallback.token}")
    private static String fallBackToken;


    /**
     * This web client method is used to make GET request to the WAYA-PAY microservice.
     * @param url
     * @param headers
     * @param queryParams
     * @return
     */
    public static String getForObject(String url, Map<String, String> headers, Map<String, Object> queryParams){
        HttpResponse<String> response;
        String responseJson;
        String message;
        WayaPayWebResponse errorResponse = new WayaPayWebResponse();
        errorResponse.setStatus(false);
        errorResponse.setMessage("Oops! application couldn't process your request. Please contact support");

        try{
            Unirest.config().verifySsl(false);
            GetRequest getRequest = Unirest.get(url);
            if(headers != null)
                getRequest.headers(headers);
            if(queryParams != null)
                getRequest.queryString(queryParams);
            getRequest.header("Authorization", getToken());
            response = getRequest.asString();
            if(response != null)
                responseJson = response.getBody();
            else
                responseJson = gson.toJson(errorResponse);
        }catch (UnirestException exception){
            message = exception.getMessage();
            System.out.println("HttpError: ".concat(message));
            responseJson = gson.toJson(errorResponse);
        }
        return  responseJson;
    }


    /**
     * This method is used to make a POST request to the WAYA-PAY microservice.
     * @param requestBody
     * @param url
     * @param headers
     * @param queryParams
     * @return
     */
    public static String postForObject(Object requestBody, String url, Map<String, String> headers, Map<String, Object> queryParams){
        HttpResponse<String> response;
        String responseJson;
        String message;
        WayaPayWebResponse errorResponse = new WayaPayWebResponse();
        errorResponse.setStatus(false);
        errorResponse.setMessage("Oops! application couldn't process your request. Please contact support");
        try{
            Unirest.config().verifySsl(false);
            response = Unirest
                    .post(url)
                    .body(gson.toJson(requestBody))
                    .header("Authorization", getToken())
                    .headers(headers)
                    .contentType("application/json")
                    .accept("application/json")
                    .asString();
            System.out.printf("Body to POST::: %s", gson.toJson(requestBody));
            if(response != null){
                responseJson = response.getBody();
            }
            else
                responseJson = gson.toJson(errorResponse);
        }catch (UnirestException exception){
            message = exception.getMessage();
            System.out.println("HttpError: ".concat(message));
            responseJson = gson.toJson(errorResponse);
        }

        System.out.println("ResponseJson: " + responseJson);
        return  responseJson;
    }

    private static String getToken(){
        String fallbackToken = fallBackToken;
        String contextToken = ContextManager.getItem(Item.USSD_APP_USER_TOKEN, String.class);
        return contextToken == null ? fallbackToken : contextToken;
    }
}
