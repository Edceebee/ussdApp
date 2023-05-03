package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class AggregatorResponseData
{
    private String id;
    private String aggregator;
    private boolean active;
}
