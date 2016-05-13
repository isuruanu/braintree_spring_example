package springexample;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.braintreegateway.*;
import com.braintreegateway.Transaction.Status;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

    private BraintreeGateway gateway = Application.gateway;

     private Status[] TRANSACTION_SUCCESS_STATUSES = new Status[] {
        Transaction.Status.AUTHORIZED,
        Transaction.Status.AUTHORIZING,
        Transaction.Status.SETTLED,
        Transaction.Status.SETTLEMENT_CONFIRMED,
        Transaction.Status.SETTLEMENT_PENDING,
        Transaction.Status.SETTLING,
        Transaction.Status.SUBMITTED_FOR_SETTLEMENT
     };

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String root(Model model) {
        return "redirect:checkouts";
    }

    @RequestMapping(value = "/checkouts", method = RequestMethod.GET)
    public String checkout(Model model) {
        String clientToken = gateway.clientToken().generate();
        model.addAttribute("clientToken", clientToken);

        return "checkouts/new";
    }

    @RequestMapping(value = "/checkouts", method = RequestMethod.POST)
    public String postForm(@RequestParam("payment_method_nonce") String nonce, Model model, final RedirectAttributes redirectAttributes) {
        CustomerRequest customerRequest = new CustomerRequest().paymentMethodNonce(nonce);
        Result<Customer> customerResult = gateway.customer().create(customerRequest);

        List<PayPalAccount> payPalAccounts = customerResult.getTarget().getPayPalAccounts();
        List<CreditCard> creditCards = customerResult.getTarget().getCreditCards();
        String token;
        if(payPalAccounts != null && payPalAccounts.size() != 0) {
            token = payPalAccounts.get(0).getToken();
        } else {
            token = creditCards.get(0).getToken();
        }
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest().planId("Apptizer-Mobile-Store").
                trialDuration(10).
                trialPeriod(true).
                trialDurationUnit(Subscription.DurationUnit.DAY).
                paymentMethodToken(token);

        Result<Subscription> subscriptionResult = gateway.subscription().create(subscriptionRequest);

        if (subscriptionResult.isSuccess()) {
            Subscription transaction = subscriptionResult.getTarget();
            return "redirect:checkouts/" + transaction.getId();
        } else if (subscriptionResult.getTransaction() != null) {
            Transaction transaction = subscriptionResult.getTransaction();
            return "redirect:checkouts/" + transaction.getId();
        } else {
            String errorString = "";
            for (ValidationError error : subscriptionResult.getErrors().getAllDeepValidationErrors()) {
               errorString += "Error: " + error.getCode() + ": " + error.getMessage() + "\n";
            }
            redirectAttributes.addFlashAttribute("errorDetails", errorString);
            return "redirect:checkouts";
        }
    }

    @RequestMapping(value = "/checkouts/{transactionId}")
    public String getTransaction(@PathVariable String transactionId, Model model) {
        Transaction transaction;
        CreditCard creditCard;
        Customer customer;

        try {
            transaction = gateway.transaction().find(transactionId);
            creditCard = transaction.getCreditCard();
            customer = transaction.getCustomer();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            return "redirect:/checkouts";
        }

        model.addAttribute("isSuccess", Arrays.asList(TRANSACTION_SUCCESS_STATUSES).contains(transaction.getStatus()));
        model.addAttribute("transaction", transaction);
        model.addAttribute("creditCard", creditCard);
        model.addAttribute("customer", customer);

        return "checkouts/show";
    }
}
