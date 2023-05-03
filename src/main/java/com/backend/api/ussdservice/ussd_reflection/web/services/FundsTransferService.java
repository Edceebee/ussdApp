package com.backend.api.ussdservice.ussd_reflection.web.services;

import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.web.WebClient;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.BankAndIndex;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.FundsTransferDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.UserInfoResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.InterbankFundsTransferRequest;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.NameEnquiryRequest;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.*;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FundsTransferService {

    @Autowired
    private Environment  env;

    @Autowired
    private CustomerService customerService;

    private static final Gson gson = new Gson();

    public List<BankData> getBankDataList() {
        String getBankCodeUrl = env.getProperty("wayabank.apiPaths.getBanks");
        String responseJsonGet = WebClient.getForObject(getBankCodeUrl, null, null);
        log.info("response from web get codes ------>>> {}", responseJsonGet);
        BankCodes bankCodes;
        try{
          bankCodes =  gson.fromJson(responseJsonGet, BankCodes.class);
            return bankCodes.getData();
        }catch (Exception ignored){ return null; }
    }

    public NameEnquiryResponse getBeneficiaryNameEnquiry(String bankName, String accountNumber) {

        AtomicReference<String> bankCode = new AtomicReference<>("");

        List<BankData> bankCodes =  this.getBankDataList();

        if(bankCodes != null) {
            bankCodes.forEach(x -> {
                if (x.getBankName().equalsIgnoreCase(bankName)) {
                    bankCode.set(x.getBankCode());
                }
            });
        }
        log.info("bank code -----> {}", bankCode.get());
        log.info("name from path2222 ----> {}", bankName);

        String nameEnquiryUrl = env.getProperty("wayabank.apiPaths.beneficiaryNameEnquiry");
        NameEnquiryRequest nameEnquiryRequest = NameEnquiryRequest.builder()
                .accountNumber(accountNumber).bankCode(bankCode.get()).build();

        String responseJsonPost = WebClient.postForObject(nameEnquiryRequest, nameEnquiryUrl, null, null);
        log.info("response from get name details ------>>> {}", responseJsonPost);
        try{
            return gson.fromJson(responseJsonPost, NameEnquiryResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public NameEnquiryResponse getBeneficiaryNameEnquiryInternal(String accountNumber){
        String wayabankCode = "090590";
        String beneficiaryBank = "WAYA MICROFINANCE BANK";
        String nameEnquiryUrl = env.getProperty("wayabank.apiPaths.beneficiaryNameEnquiry");

        NameEnquiryRequest nameEnquiryRequest = NameEnquiryRequest.builder()
                .accountNumber(accountNumber).bankCode(wayabankCode).build();

        String responseJsonPost = WebClient.postForObject(nameEnquiryRequest, nameEnquiryUrl, null, null);
        log.info("response from get name details ------>>> {}", responseJsonPost);
        try{
            return gson.fromJson(responseJsonPost, NameEnquiryResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public InternalNameEnquiryResponseDTO getBeneficiaryNameEnquiryInternal2(String accountNumber){
        String url = env.getProperty("wayabank.apiPaths.nameEnquiryInternal")
                .replace("{accountNumber}", accountNumber);

        String responseJsonGet = WebClient.getForObject(url, null, null);
        log.info("Internal Name Enquiry Response GET =======> {}", responseJsonGet);
        try{
            return gson.fromJson(responseJsonGet, InternalNameEnquiryResponseDTO.class);
        }catch (Exception e){
            return null;
        }
    }

    public List<BankData> getBanks(){
        String getBankCodeUrl = env.getProperty("wayabank.apiPaths.getBanks");
        String responseJsonGet = WebClient.getForObject(getBankCodeUrl, null, null);
        log.info("response from web get codes ------>>> {}", responseJsonGet);
        BankCodes bankCodes;
        try{
            bankCodes = gson.fromJson(responseJsonGet, BankCodes.class);
            return bankCodes.getData();
        }catch(Exception ignored){
            return null;
        }
    }

    public List<String> getBankNamesForTransfer() {

        String getBankCodeUrl = env.getProperty("wayabank.apiPaths.getBanks");
        String responseJsonGet = WebClient.getForObject(getBankCodeUrl, null, null);
        log.info("response from web get codes ------>>> {}", responseJsonGet);
        BankCodes bankCodes = null;
        try{
          bankCodes = gson.fromJson(responseJsonGet, BankCodes.class);
        }catch(Exception ignored){}

        List<String> bankNamesList = new ArrayList<>();
        if(bankCodes != null) {
            bankCodes.getData().forEach(x -> {
                String eachBankName = x.getBankName();
                bankNamesList.add(eachBankName);
            });
        }
        return bankNamesList;
    }

    public BankAndIndex getNextBanksForSession(String sessionId){
        final String bankIndexArrayKey = "BANK_INDEX_ARRAY";
        String[] bankCodes1 = new String[]{
                "000014", "000005", "100013", "000010", "000007", "100019", "000016", "000013"
        };

        String[] bankCodes2 = new String[]{
                "000020", "090405", "100004", "100033", "000008", "000023", "000012", "000001"
        };

        String[] bankCodes3 = new String[]{
                "000018", "000004", "000011", "000017", "000015"
        };

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(sessionId, HashMap.class);
        Object bankIndexArray = customerData.get(bankIndexArrayKey);
        // Check for starting
        if(bankIndexArray == null){
            bankIndexArray = List.of(0,7);
            customerData.put(bankIndexArrayKey, bankIndexArray);
            SessionManager.updateExtraDataOfSession(sessionId, customerData);
        }
        List<Integer> indexArray = (List<Integer>)bankIndexArray;
        List<BankData> bankData;
        List<Integer> indices;
        BankAndIndex bankAndIndex;
        if(indexArray.get(0) == 0 && indexArray.get(1) == 7){
             bankData = this.getBanksFromBankCodeArray(List.of(bankCodes1));
             indices = List.of(0, 7);
             bankAndIndex = new BankAndIndex(bankData, indices);
             customerData.put(bankIndexArrayKey, List.of(8, 15));
             SessionManager.updateExtraDataOfSession(sessionId, customerData);
             return bankAndIndex;
        }
        if(indexArray.get(0) == 8 && indexArray.get(1) == 15){
            bankData = this.getBanksFromBankCodeArray(List.of(bankCodes2));
            indices = List.of(8, 15);
            bankAndIndex = new BankAndIndex(bankData, indices);
            customerData.put(bankIndexArrayKey, List.of(16, 20));
            SessionManager.updateExtraDataOfSession(sessionId, customerData);
            return bankAndIndex;
        }
        if(indexArray.get(0) == 16 && indexArray.get(1) == 20){
            bankData = this.getBanksFromBankCodeArray(List.of(bankCodes3));
            indices = List.of(16, 20);
            bankAndIndex = new BankAndIndex(bankData, indices);
            customerData.put(bankIndexArrayKey, List.of(0, 7));
            SessionManager.updateExtraDataOfSession(sessionId, customerData);
            return bankAndIndex;
        }
        return null;
    }

    public List<BankData> getBanksFromBankCodeArray(List<String> bankCodes){
        List<BankData> bankDataFromContext;
        try{
            bankDataFromContext  = ContextManager.getItem(Item.USSD_SERVICE_BANKS, List.class);
            if(bankDataFromContext == null){
                bankDataFromContext = this.getBanks();
            }
        }catch (Exception e){ return null; }
        if(bankDataFromContext == null){
            return null;
        }
        return bankDataFromContext.stream()
                .filter(bank -> bankCodes.contains(bank.getBankCode()))
                .collect(Collectors.toList());
    }

    public FundsTransferResponse processInterbankFundsTransfer(FundsTransferDTO requestDTO){
        // Get the bank code from the incoming bank Name.
        AtomicReference<String> bankCode = new AtomicReference<>("");
        if(this.getBankDataList() != null) {
            this.getBankDataList().forEach(data -> {
                if (data.getBankName().equalsIgnoreCase(requestDTO.getBankName())) {
                    bankCode.set(data.getBankCode());
                }
            });
        }
        UserInfoResponseDTO user = customerService.getCustomerDetailsByPhoneNumber(requestDTO.getSourcePhoneNumber());
        log.info("Source customer details: {}", gson.toJson(user));

        String debitAccountName = String.join(" ", user.getData().getFirstName(), user.getData().getMiddleName(), user.getData().getLastName());
        String narration = "USSD Interbank Funds transfer to " + requestDTO.getBeneficiaryName() + " / " + requestDTO.getBankName();

        // Build the funds transfer payload to Waya-Pay service.
        InterbankFundsTransferRequest request = InterbankFundsTransferRequest.builder()
                .amount(requestDTO.getAmount())
                .bankCode(bankCode.get())
                .bankName(requestDTO.getBankName())
                .crAccount(requestDTO.getBeneficiaryAccount())
                .crAccountName(requestDTO.getBeneficiaryName())
                .debitAccountName(debitAccountName)
                .narration(narration)
                .saveBen(true)
                .transRef(getTransactionReference())
                .transactionPin(requestDTO.getPin())
                .userId(String.valueOf(user.getData().getUserId()))
                .walletAccountNo(requestDTO.getSourceAccount())
                .build();

        log.info("Request to WayaPay --------> ".concat(gson.toJson(request)));

        String fundsTransferUrl = env.getProperty("wayabank.apiPaths.fundsTransfer");
        Map<String, String> headers = new HashMap<>();
        headers.put("pin", requestDTO.getPin());
        String responseJsonPost = WebClient.postForObject(requestDTO, fundsTransferUrl, headers, null);

        log.info("Funds Transfer Response From WayaPay -----> ".concat(responseJsonPost));
        try{
            return gson.fromJson(responseJsonPost, FundsTransferResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public FundsTransferResponse processIntraBankFundsTransfer(FundsTransferDTO requestDTO){
        // Get the bank code from the incoming bank Name.

        UserInfoResponseDTO user = customerService.getCustomerDetailsByPhoneNumber(requestDTO.getSourcePhoneNumber());
        log.info("Source customer details: {}", gson.toJson(user));

        String fundsTransferUrl = env.getProperty("wayabank.apiPaths.fundsTransferInternal")
                .replace("{senderId}", String.valueOf(user.getData().getUserId()))
                .replace("{senderAcctNo}", requestDTO.getSourceAccount())
                .replace("{beneficialAcctNo}", requestDTO.getBeneficiaryAccount())
                .replace("{amount}", requestDTO.getAmount());

        log.info("Complete internal FT URL: {}", fundsTransferUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put("pin", requestDTO.getPin());
        String responseJsonPost = WebClient.postForObject(requestDTO, fundsTransferUrl, headers, null);

        log.info("Funds Transfer Response From WayaPay -----> {}", responseJsonPost);

        try{
            return gson.fromJson(responseJsonPost, FundsTransferResponse.class);
        }catch (Exception e){
            return null;
        }
    }

    public String getUserTransactionFee(String accountNo, String amount, String entityId){
        String url = env.getProperty("wayabank.apiPaths.userTransactionFee")
                .replace("{accountNo}", accountNo)
                .replace("{amount}", amount)
                .replace("{entityId}", entityId);
        String fee = WebClient.getForObject(url, null, null);
        log.info("User Transaction Fee: {}", fee);
       return fee == null ? "" : fee;
    }

    public ChargeCustomerResponsePayload chargeCustomer(ChargeCustomerRequestPayload requestPayload, String pin){
        String url = env.getProperty("wayabank.apiPaths.chargeCustomer");
        Map<String, String> headers = new HashMap();
        headers.put("pin", pin);
        String responseJsonPost = WebClient.postForObject(requestPayload, url, headers, null);
        log.info("Response For customer Charge: {}", responseJsonPost);
        try{
            return gson.fromJson(responseJsonPost, ChargeCustomerResponsePayload.class);
        }catch (Exception e){
            return new ChargeCustomerResponsePayload();
        }
    }

    private String getTransactionReference(){
        String now = String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        return "ATFR".concat(now);
    }
}
