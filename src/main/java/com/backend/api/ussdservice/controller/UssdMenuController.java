package com.backend.api.ussdservice.controller;

import com.backend.api.ussdservice.ussd_reflection.bootstrap.UssdGlobalServiceHandler;
import com.backend.api.ussdservice.ussd_reflection.context.helper.UssdPayloadConverter;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdPayload;
import com.backend.api.ussdservice.pojo.UssdMenuPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@Slf4j
@RequestMapping("/ussd/session")
public class UssdMenuController
{
    @Autowired
    private UssdGlobalServiceHandler ussdGlobalServiceHandler;

    @PostMapping("/callback")
    public String ussdCallBack(@RequestBody UssdMenuPayload ussdMenuPayload) throws Exception {
        UssdPayload ussdPayload = UssdPayload.builder()
                .input(UssdPayloadConverter.getUsableText(ussdMenuPayload.getText()))
                .telco(UssdPayloadConverter.getNetworkFromNetworkCode(ussdMenuPayload.getNetworkCode()))
                .mobileNumber(ussdMenuPayload.getPhoneNumber())
                .sessionOperation("")
                .sessionType("")
                .sessionId(ussdMenuPayload.getSessionId())
                .shortCodeString(ussdMenuPayload.getServiceCode())
                .build();
        return ussdGlobalServiceHandler.submitForContinuation(ussdPayload);
    }

    @PostMapping(value = "/form/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String ussdFormCallBack(@RequestParam Map<String, String> ussdMenuPayload) throws Exception {
        log.info("Map: " + ussdMenuPayload);
        UssdPayload ussdPayload = UssdPayloadConverter.buildPayloadFromMap(ussdMenuPayload);
        return ussdGlobalServiceHandler.submitForContinuation(ussdPayload);
    }
}
