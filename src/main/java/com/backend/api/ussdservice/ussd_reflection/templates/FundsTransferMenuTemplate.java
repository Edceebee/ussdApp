package com.backend.api.ussdservice.ussd_reflection.templates;

import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MenuOptionBuilder;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.BankAndIndex;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.BankData;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FundsTransferMenuTemplate
{
    public String getEnterAmountScreen(){
        return new MessageLineBuilder()
                .addLine("Enter amount or reply 0 to go back to previous page")
                .toString();
    }

    public UssdMessageAndData getAccountNumberListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select account");

        for(int i = 1; i <= collection.size(); i++){
            String accountNumber = collection.get(i - 1);
            String accountKey = key.concat(String.valueOf(i));
            customerData.put(accountKey, accountNumber);
            menuOptionBuilder.addOption(i, accountNumber);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("accountNumbers", collection);

        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }

    public UssdMessageAndData getBankListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select bank");

        for(int i = 1; i <= collection.size(); i++){
            String bank = collection.get(i - 1);
            String bankKey = key.concat(String.valueOf(i));
            customerData.put(bankKey, bank);
            menuOptionBuilder.addOption(i, bank);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("beneficiaryBanks", collection);

        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }

    public UssdMessageAndData getBankListUssdMessageAndDataFromBankAndIndex(BankAndIndex bankAndIndex, HashMap<String, Object> customerData, String key){
        List<String> collection = bankAndIndex.getBankData().stream()
                .map(BankData::getBankName).collect(Collectors.toList());

        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select bank");
        int startIndex = bankAndIndex.getIndices().get(0);
        int endIndex = bankAndIndex.getIndices().get(1);
        for(int i = 0; i < collection.size(); i++){
            String bank = collection.get(i);
            String bankKey = key.concat(String.valueOf(startIndex + 1));
            customerData.put(bankKey, bank);
            menuOptionBuilder.addOption(startIndex + 1, bank);
            startIndex++;
        }
        menuOptionBuilder.addOption(0, "Go back");
        if(bankAndIndex.getIndices().get(0) < 16 && bankAndIndex.getIndices().get(1) < 21){
            menuOptionBuilder.addOption("00", "Next");
        }
        if(bankAndIndex.getIndices().get(0) == 0 && bankAndIndex.getIndices().get(1) == 7){
            customerData.put("isBankListJustStarted", "true");
        }else{
            customerData.put("isBankListJustStarted", "false");
        }
        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }

        public String getEnterPinToProceedScreen(){
        return new MessageLineBuilder()
                .addLine("Enter your Four(4) digit PIN to proceed: ")
                .toString();
    }

    public String getEnterBeneficiaryAccountNumber(){
        return new MessageLineBuilder()
                .addLine("Enter beneficiary account number or press 0 to go back")
                .toString();
    }

    public String getProceedToTransferScreen(String bankName, String beneficiaryName, String beneficiaryAccount){
        String message = new MessageLineBuilder()
                .addLine("You are about to make funds transfer associated with the following details:")
                .addLine(String.format("Bank - %s", bankName))
                .addLine(String.format("Account name - %s", beneficiaryName))
                .addLine(String.format("Account number - %s", beneficiaryAccount))
                .addLine("\n")
                .toString();
        return new MenuOptionBuilder()
                .addOption("", message)
                .addOption(1, "Proceed")
                .addOption(0, "Back")
                .toString();
    }

    public String getNameInquiryFailureMessage(UssdContext context){
        String errorPopupMessage = new MessageLineBuilder()
                .addLine("Name enquiry failed in system. Ensure you enter correct account details.")
                .addLine("Please try again. Thank you.")
                .toString();
        SessionManager.clearSession(context.getSessionId());
        return errorPopupMessage;
    }

    public String getErrorMessage(UssdContext context){
        String message = new MessageLineBuilder()
                .addLine("Oops!. You entered an invalid input. Please try again")
                .addLine("Thank you")
                .toString();
        SessionManager.clearSession(context.getSessionId());
        return message;
    }
}
