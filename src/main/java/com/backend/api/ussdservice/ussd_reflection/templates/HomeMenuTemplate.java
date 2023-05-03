package com.backend.api.ussdservice.ussd_reflection.templates;

import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MenuOptionBuilder;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;

/**
 * This class is a template generator for the home screens both for existing and non-existing customers.
 */
public class HomeMenuTemplate
{
    public String getExistingCustomerWelcomeScreen(){
        return new MessageLineBuilder()
                .addLine("Hi There! Welcome to wayabank USSD Banking. Please note that a N5 network charge will be applied to your account for banking services on this channel.")
                .addLine("1. Accept")
                .addLine("2. Cancel")
                .toString();
    }

    public String getNewCustomerWelcomeScreen(){
        return new MessageLineBuilder()
                .addLine("Hi There! Welcome to wayabank USSD Banking.")
                .addLine("kindly press 1 to register or press 0 to cancel.")
                .addLine("However, if you are a registered user seeing this message, contact administrator or support.")
                .toString();
    }

    public String getExistingCustomerHomeMenuListScreen() {
        return new MenuOptionBuilder()
                .addOption(1, "Fund Account")
                .addOption(2, "Trsf - Wayabank")
                .addOption(3, "Trsf - Other Banks")
                .addOption(4, "Airtime - Self")
                .addOption(5, "Airtime - Others")
                .addOption(6, "Data - Self")
                .addOption(7, "Data - Others")
                .addOption(8, "Cable TV")
                .addOption(9, "Betting")
                .addOption(10, "Check Balance")
                .toString();
    }

    public String getNewCustomerHomeMenuListScreen() {
        return new MenuOptionBuilder()
                .addOption(1, "Personal User")
                .addOption(2, "Business User")
                .toString();
    }

    public String getNewCustomerBusinessLinkScreen() {
        return new MessageLineBuilder()
                .addLine("Kindly go to www.wayabank.ng to register as a business user")
                .toString();
    }

    public String getErrorMessage(UssdContext context){
        SessionManager.clearSession(context.getSessionId());
        return new MessageLineBuilder()
                .addLine("Oops!, you entered an invalid input. Please try again.")
                .addLine("Thank you.")
                .toString();
    }
    public String getCustomerWelcomeMessageScreen() {
        return new MessageLineBuilder().addLine("Thanks for banking with us").toString();
    }
}
