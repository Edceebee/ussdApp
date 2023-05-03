package com.backend.api.ussdservice.ussd_reflection.context.helper;

import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdPayload;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UssdPayloadConverter
{
    public static UssdPayload buildPayloadFromMap(Map<String, String> map){
        String text = String.valueOf(map.get("text"));
        String input = getUsableText(text);
        String sessionId = map.get("sessionId");
        String phoneNumber = map.get("phoneNumber");
        String serviceCode = map.get("serviceCode");
        String networkCode = map.get("networkCode");
        String telco = getNetworkFromNetworkCode(networkCode);
        return UssdPayload.builder()
                .mobileNumber(phoneNumber)
                .sessionType("")
                .sessionId(sessionId)
                .input(input)
                .shortCodeString(serviceCode)
                .sessionOperation("")
                .telco(telco)
                .build();
    }

    public static String getUsableText(String text){
        if(text == null)
            return null;
        text = text.replaceAll("\"", "");
        if(text.equalsIgnoreCase(""))
            return null;
        if(text.isEmpty() || text.isBlank())
            return null;
        if(text.trim().equalsIgnoreCase("null"))
            return null;
        if(text.contains("*")){
            List<String> tokens = List.of(text.split("\\*")).stream()
                    .filter(token -> !token.equalsIgnoreCase(""))
                    .filter(token -> !token.equalsIgnoreCase(" "))
                    .collect(Collectors.toList());
            Collections.reverse(tokens);
            return tokens.stream().findFirst().get();
        }
        return text;
    }

    public static String getNetworkFromNetworkCode(String networkCode) {
        String telco = "";
        switch (networkCode){
            case "62120": { telco = "AIRTEL"; break; }
            case "62130": { telco = "MTN"; break; }
            case "62150": { telco = "GLO"; break; }
            case "62160": {telco = "9MOBILE"; break; }
            default: {telco = "";}
        }
        return telco;
    }
}
