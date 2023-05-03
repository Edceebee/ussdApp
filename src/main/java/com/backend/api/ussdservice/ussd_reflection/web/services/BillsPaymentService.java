package com.backend.api.ussdservice.ussd_reflection.web.services;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.*;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.web.WebClient;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.WayaPayWebResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.CableTvRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.CableTvRequest;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.CableTvRequestData;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ValidateBillRequestPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class BillsPaymentService
{
    @Autowired
    private Environment env;

    private static final Gson gson = new Gson();

    public CableTvListResponse getCableTvListResponse(){
        String url = env.getProperty("wayabank.apiPaths.billPaymentPayBill");
        String responseJsonGet = WebClient.getForObject(url, null, null);
        this.logResponseFromWayaPayService(responseJsonGet);
        try{
            return gson.fromJson(responseJsonGet, CableTvListResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public List<String> getCableTvBillerNames(){
        if(this.getCableTvListResponse() != null) {
            return this.getCableTvListResponse().getData().stream()
                    .map(CableTvResponseData::getBillerName)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public List<CableTvResponseData> getCableTvObjects(){
        if(this.getCableTvListResponse() != null) {
            return this.getCableTvListResponse().getData();
        }
        return null;
    }

    public String getBillerIdFromBillerName(String billerName){
        AtomicReference<String> result = new AtomicReference<>("");
        List<CableTvResponseData> data = this.getCableTvListResponse().getData();
        data.forEach(d -> {
            if(d.getBillerName().toLowerCase().contains(billerName.toLowerCase())){
                result.set(d.getBillerId());
            }
        });
        return result.get();
    }

    public PackageListResponse getCableTvPackagesByBillerName(String billerName){
        try{
            String billerId = this.getBillerIdFromBillerName(billerName);
            String url = env.getProperty("wayabank.apiPaths.billPaymentPayBill").concat("/biller").concat("/").concat(billerId);
            String responseJsonGet = WebClient.getForObject(url, null, null);
            return gson.fromJson(responseJsonGet, PackageListResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public List<PackageSubItem> getSubItemListForCableTv(String billerName){
        PackageListResponse listResponse = this.getCableTvPackagesByBillerName(billerName);
        if(listResponse != null && listResponse.isStatus()){
            AtomicReference<PackageItem> item = new AtomicReference<>(null);
            List<PackageItem> items  = this.getCableTvPackagesByBillerName(billerName).getData().getItems();
            items.forEach(i -> {
                String paramName = i.getParamName().toLowerCase();
                if(paramName.equalsIgnoreCase("plan")){
                    item.set(i);
                }
            });
            return item.get().getSubItems();
        }
        return null;
    }

    public PackageSubItem getSubItemByPackageName(String packageName, String billerName){
        AtomicReference<PackageSubItem> result = new AtomicReference<>(null);
        List<PackageSubItem> list = this.getSubItemListForCableTv(billerName);
        if(list != null && !list.isEmpty()){
            list.forEach(l -> {
               if(l.getName().equalsIgnoreCase(packageName)){
                   result.set(l);
               }
            });
            return result.get();
        }
        return result.get();
    }

    public List<String> getCableTvPackageNameAndAmountConcatenated(String billerName){
        List<PackageSubItem> subItems = this.getSubItemListForCableTv(billerName);
        if(subItems != null){
            return subItems.stream()
                    .map(subItem -> String.join(" = ", subItem.getName(), "N ".concat(subItem.getAmount())))
                    .collect(Collectors.toList());
        }
        return null;
    }

    public List<String> getPackageNameAndAmountSplit(String concatenated){
        return Stream.of(concatenated.split(" = "))
                .filter(token -> !token.equalsIgnoreCase(""))
                .filter(token -> !token.equalsIgnoreCase(" "))
                .map(String :: trim)
                .collect(Collectors.toList());
    }

    public String getCableTvPackageNameFromConcat(String concatenated){
        return this.getPackageNameAndAmountSplit(concatenated).get(0).trim();
    }

    public String getAmountFromConcat(String concatenated){
        return this.getPackageNameAndAmountSplit(concatenated)
                .get(this.getPackageNameAndAmountSplit(concatenated).size() - 1)
                .replace("N", "").trim();
    }


    // Process the payment of airtime or data.
    public WayaPayWebResponse processCablePayment(CableTvRequestDTO requestDTO){
        final String baseUrl = env.getProperty("wayabank.apiPaths.billerPayment");
        CableTvRequestData requestData = new CableTvRequestData();
        requestData.setName("phone");
        requestData.setValue(requestDTO.getPhoneNumber());

        CableTvRequestData requestData1 = new CableTvRequestData();
        requestData1.setName("amount");
        requestData1.setValue(requestDTO.getAmount());

        CableTvRequestData requestData2 = new CableTvRequestData();
        requestData2.setName("paymentMethod");
        requestData2.setValue("cash");

        CableTvRequestData requestData3 = new CableTvRequestData();
        requestData3.setName("channel");
        requestData3.setValue("USSD");

        CableTvRequestData requestData4 = new CableTvRequestData();
        requestData4.setName("plan");
        requestData4.setValue(requestDTO.getPlan());

        List<CableTvRequestData> data = List.of(requestData, requestData1, requestData2, requestData3);
        CableTvRequest request = new CableTvRequest();
        request.setAmount(Double.parseDouble(requestDTO.getAmount()));
        request.setCategoryId(requestDTO.getCategoryId());
        request.setBillerId(requestDTO.getBillerId());
        request.setData(data);
        request.setSourceWalletAccountNumber(requestDTO.getAccountNumber());

        log.info("CableTv body: {}", gson.toJson(request));
        Map<String, String> headers = new HashMap<>();
        headers.put("pin", requestDTO.getPin());
        String responseJson = WebClient.postForObject(request, baseUrl, headers, null);
        this.logResponseFromWayaPayService(responseJson);
        try{
            return gson.fromJson(responseJson, WayaPayWebResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public AggregatorResponsePayload getAggregators(){
        String url = env.getProperty("wayabank.apiPaths.billPaymentAggregator");
        String responseJsonGet = WebClient.getForObject(url, null, null);
        try {
            return gson.fromJson(responseJsonGet, AggregatorResponsePayload.class);
        }catch(Exception e){
            return null;
        }
    }

    public String getActiveAggregator(){
        AggregatorResponsePayload responsePayload = this.getAggregators();
        String result = "";
        if(responsePayload != null && responsePayload.isStatus()){
            List<AggregatorResponseData> data = responsePayload.getData();
            for(AggregatorResponseData d : data){
                if(d.isActive()){
                    result = d.getAggregator();
                    break;
                }
            }
            return result;
        }
        return null;
    }

    public ValidateBillResponsePayload validateBill(ValidateBillRequestPayload requestPayload, String pin){
        String url = env.getProperty("wayabank.apiPaths.validateBill");
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("pin", pin);
        String responseJsonPost = WebClient.postForObject(requestPayload, url, headers, null);
        try{
            return gson.fromJson(responseJsonPost, ValidateBillResponsePayload.class);
        }catch (Exception e){
            return null;
        }
    }

    private void logResponseFromWayaPayService(String responseJson){
        log.info("Response from Waya pay service: {}", responseJson);
    }

}
