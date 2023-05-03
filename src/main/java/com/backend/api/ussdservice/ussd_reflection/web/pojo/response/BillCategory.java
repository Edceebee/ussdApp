package com.backend.api.ussdservice.ussd_reflection.web.pojo.response;

import lombok.Data;

@Data
public class BillCategory
{
    private String billerId;
    private String billerName;
    private String billerWayaPayName;
    private String categoryId;
}
