package com.backend.api.ussdservice.ussd_reflection.web.services;


import com.backend.api.ussdservice.ussd_reflection.web.WebClient;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.WayaPayWebResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.*;
import com.google.gson.Gson;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.AirtimeDataRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.AirtimeDataRequest;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.AirtimeDataRequestData;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.BillCategoryResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class AirtimeDataService
{
    @Autowired
    private Environment env;

    private final Gson gson = new Gson();


    // Process the payment of airtime or data.
    public WayaPayWebResponse processAirtimeOrDataPayment(AirtimeDataRequestDTO requestDTO){
        final String baseUrl = env.getProperty("wayabank.apiPaths.billerPayment");
        AirtimeDataRequestData requestData = new AirtimeDataRequestData();
        requestData.setName("phone");
        requestData.setValue(requestDTO.getPhoneNumber());

        AirtimeDataRequestData requestData1 = new AirtimeDataRequestData();
        requestData1.setName("amount");
        requestData1.setValue(requestDTO.getAmount());

        AirtimeDataRequestData requestData2 = new AirtimeDataRequestData();
        requestData2.setName("bundles");
        requestData2.setValue(requestDTO.getAmount());

        List<AirtimeDataRequestData> data = List.of(requestData, requestData1, requestData2);
        AirtimeDataRequest request = new AirtimeDataRequest();
        request.setAmount(Double.parseDouble(requestDTO.getAmount()));
        request.setCategoryId(requestDTO.getCategoryId());
        request.setBillerId(requestDTO.getBillerId());
        request.setData(data);
        request.setSourceWalletAccountNumber(requestDTO.getAccountNumber());

        log.info("Airtime body: {}", gson.toJson(request));
        Map<String, String> headers = new HashMap<>();
        headers.put("pin", requestDTO.getPin());
        String responseJson = WebClient.postForObject(request, baseUrl, headers, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, WayaPayWebResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public WayaPayWebResponse processAirtimePayment(AirtimeDataRequestDTO requestDTO){
        final String baseUrl = env.getProperty("wayabank.apiPaths.billerPayment");
        AirtimeDataRequestData requestData = new AirtimeDataRequestData();
        requestData.setName("phone");
        requestData.setValue(requestDTO.getPhoneNumber());

        AirtimeDataRequestData requestData1 = new AirtimeDataRequestData();
        requestData1.setName("amount");
        requestData1.setValue(requestDTO.getAmount());

        AirtimeDataRequestData requestData2 = new AirtimeDataRequestData();
        requestData2.setName("plan");
        requestData2.setValue("prepaid");

        List<AirtimeDataRequestData> data = List.of(requestData, requestData1, requestData2);
        AirtimeDataRequest request = new AirtimeDataRequest();
        request.setAmount(Double.parseDouble(requestDTO.getAmount()));
        request.setCategoryId(requestDTO.getCategoryId());
        request.setBillerId(requestDTO.getBillerId());
        request.setData(data);
        request.setSourceWalletAccountNumber(requestDTO.getAccountNumber());

        log.info("Airtime body: {}", gson.toJson(request));
        Map<String, String> headers = new HashMap<>();
        headers.put("pin", requestDTO.getPin());
        String responseJson = WebClient.postForObject(request, baseUrl, headers, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, WayaPayWebResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public BillCategoryResponseDTO getDataBillCategory(){
        final String baseUrl = env.getProperty("wayabank.apiPaths.dataBiller");
        String responseJson = WebClient.getForObject(baseUrl, null, null);
        this.logResponseFromWayaPay(responseJson);
        try{
            return gson.fromJson(responseJson, BillCategoryResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    public AirtimeDataBillerData getDataBillerInfoFromNetwork(String network){
        AtomicReference<AirtimeDataBillerData> result = new AtomicReference<>(null);
        BillCategoryResponseDTO billCategory = this.getDataBillCategory();
        if(billCategory != null && billCategory.isStatus()){
            List<AirtimeDataBillerData> dataBills = billCategory.getData();
            dataBills.forEach(data -> {
                if(data.getBillerId().equalsIgnoreCase(network.toLowerCase())){
                    result.set(data);
                }
            });
            return result.get();
        }
        return result.get();
    }

    public PackageListResponse getDataPackageListResponseFromNetwork(String network){
        AirtimeDataBillerData dataBiller = this.getDataBillerInfoFromNetwork(network);
        if(dataBiller != null){
            String billerId = dataBiller.getBillerId();
            String fullUrl = env.getProperty("wayabank.apiPaths.dataBiller").concat("/biller/").concat(billerId);
            String responseJsonGet = WebClient.getForObject(fullUrl, null, null);
            try{
                return gson.fromJson(responseJsonGet, PackageListResponse.class);
            }catch (Exception e){
                return null;
            }
        }
        return null;
    }

    public PackageItem getDataPackageFromNetwork(String network){
        AtomicReference<PackageItem> result = new AtomicReference<>(null);
        PackageListResponse packageListResponse = this.getDataPackageListResponseFromNetwork(network);
        if(packageListResponse != null && packageListResponse.isStatus()){
            PackageResponseData packageResponseData = packageListResponse.getData();
            if(packageResponseData.getItems() != null && !packageResponseData.getItems().isEmpty()){
                List<PackageItem> packageItems = packageResponseData.getItems();
                packageItems.forEach(packageItem -> {
                    String paramName = packageItem.getParamName();
                    if(paramName.equalsIgnoreCase("bundles")){
                        result.set(packageItem);
                    }
                });
                return result.get();
            }
            return null;
        }
        return null;
    }

    public List<PackageSubItem> getDataPackageSubItemsFromNetwork(String network){
        PackageItem packageItem = this.getDataPackageFromNetwork(network);
        if(packageItem != null){
            return packageItem.getSubItems();
        }
        return null;
    }

    public List<String> getDataPlansFromNetwork(String network){
        List<String> dataPlans = new ArrayList<String>();
        List<PackageSubItem> subItems = this.getDataPackageSubItemsFromNetwork(network);
        if(subItems != null && !subItems.isEmpty()){
            subItems.forEach(subItem -> {
                String dataPlanName = subItem.getName();
                String amount = subItem.getAmount();
                String completePlanInfo = dataPlanName.concat(" = ").concat("N ").concat(amount);
                dataPlans.add(completePlanInfo);
            });
            return dataPlans;
        }
        return null;
    }

    public List<String> getPackageNameAndAmountSplit(String concatenated){
        return Stream.of(concatenated.split("="))
                .filter(token -> !token.equalsIgnoreCase(""))
                .filter(token -> !token.equalsIgnoreCase(" "))
                .map(String :: trim)
                .collect(Collectors.toList());
    }

    public String getAmountFromConcat(String concatenated){
        return this.getPackageNameAndAmountSplit(concatenated)
                .get(this.getPackageNameAndAmountSplit(concatenated).size() - 1)
                .replace("N", "").trim();
    }

    public AirtimeBillerResponse getAirtimeBillerList(){
        String url = env.getProperty("wayabank.apiPaths.airtimeBiller");
        String responseJsonGet = WebClient.getForObject(url, null, null);
        this.logResponseFromWayaPay(responseJsonGet);
        try{
            return gson.fromJson(responseJsonGet, AirtimeBillerResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public AirtimeDataBillerData getAirtimeBillerInfoFromNetwork(String network){
        AtomicReference<AirtimeDataBillerData> result = new AtomicReference<>(null);
        if(this.getAirtimeBillerList() != null) {
            this.getAirtimeBillerList().getData()
                    .forEach(data -> {
                        if (data.getBillerId().equalsIgnoreCase(network.trim().toLowerCase())) {
                            result.set(data);
                        }
                    });
        }
        return  result.get();
    }

    public List<String> getNetworkList() {
        return List.of(
                "MTN",
                "AIRTEL",
                "9MOBILE",
                "GLO"
        );
    }
    private void logResponseFromWayaPay(String responseJson){
        log.info("Response from WayaPay: {}", responseJson);
    }

}
