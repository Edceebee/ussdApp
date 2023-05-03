package com.backend.api.ussdservice.pojo;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;

@Getter
@Builder
public class UssdMessageAndData {
    private String message;
    private HashMap<String, Object> customerData;
}