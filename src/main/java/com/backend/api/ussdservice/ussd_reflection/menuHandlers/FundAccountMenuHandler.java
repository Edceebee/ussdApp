package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.constants.CustomerTypes;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.FundsTransferMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AccountNumbersResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.InternalNameEnquiryResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.NameEnquiryResponse;
import com.backend.api.ussdservice.ussd_reflection.web.services.AccountService;
import com.backend.api.ussdservice.ussd_reflection.web.services.FundsTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.continueSessionMessage;
import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.endSessionMessage;

@Slf4j
@UssdMenuHandler
public class FundAccountMenuHandler
{
    @Autowired
    private AccountService accountService;

    @Autowired
    private FundsTransferService fundsTransferService;

    private final FundsTransferMenuTemplate template = new FundsTransferMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();


    @UssdSubMenuHandler("*1*1#")
    public String showAccountDetailsForFundingPersonalWallet(UssdContext context){
        List<String> accountNumbers = new ArrayList<String>();
        try{
            accountNumbers = accountService.getAccountNumbersForCustomer(context.getMobileNumber())
                    .getData().stream()
                    .map(AccountNumbersResponse::getAccountNo)
                    .collect(Collectors.toList());
        }catch (Exception e){
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Sorry you don't have any account associated with your profile though you are a registered customer.")
                    .addLine("Please contact administrator or support.")
                    .addLine("Thank you.")
                    .toString());
        }

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("customerType", CustomerTypes.EXISTING.name());

        UssdMessageAndData messageAndData = template
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    // User decides to go back to the main menu
    @UssdSubMenuHandler("*1*1*0#")
    public String goBackToMainMenuForInternalFunds(UssdContext context){
        return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
    }

    @UssdSubMenuHandler("*1*1*{input}#")
    public String showEnterSelfAccountMsgForInternalFunds(UssdContext context, String input){

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        // Check if the user enters 0. Then go back to the previous menu.
        if(ContextManager.isGoBackOption(input)){
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");
            UssdMessageAndData messageAndData = template
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Wrong input
        Object selectedAccount = customerData.get("Account".concat(input));
        if(selectedAccount == null){
            return endSessionMessage(template.getErrorMessage(context));
        }

        String selectedCustomerAccountNumber = String.valueOf(selectedAccount);

        // Do internal name enquiry
        String beneficiaryBank = "WAYA MICROFINANCE BANK";
        NameEnquiryResponse nameEnquiryResponse = fundsTransferService
                .getBeneficiaryNameEnquiryInternal(selectedCustomerAccountNumber);

        InternalNameEnquiryResponseDTO internalNameEnquiryResponseDTO = fundsTransferService
                .getBeneficiaryNameEnquiryInternal2(selectedCustomerAccountNumber);

        // First check from their system
        if(internalNameEnquiryResponseDTO.isStatus()){
            String accountName = internalNameEnquiryResponseDTO.getData().getAccountName();
            String accountNumber = internalNameEnquiryResponseDTO.getData().getAccountNumber();
            String walletBankName = internalNameEnquiryResponseDTO.getData().getDefaultWalletBank();
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Fund your Wayabank Account via Bank Transfer. Here is your Account Details.")
                    .addLine(String.format("Account Name: %s", accountName))
                    .addLine(String.format("Account Number: %s", accountNumber))
                    .addLine(String.format("Bank Name: %s", walletBankName))
                    .toString());
        }

        // Check from the first normal NIP name enquiry.
        else if(nameEnquiryResponse != null &&
                nameEnquiryResponse.isStatus() &&
                !nameEnquiryResponse.getData().isEmpty() &&
                !nameEnquiryResponse.getData().isBlank())
        {
            String accountName = nameEnquiryResponse.getData();
            String walletBankName = "WAYA MICROFINANCE BANK";
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Fund your Wayabank Account via Bank Transfer. Here is your Account Details.")
                    .addLine(String.format("Account Name: %s", accountName))
                    .addLine(String.format("Account Number: %s", selectedCustomerAccountNumber))
                    .addLine(String.format("Bank Name: %s", walletBankName))
                    .toString());
        }

        // Here name enquiry fails. Display error message and cancel session
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(template.getNameInquiryFailureMessage(context));
    }

}
