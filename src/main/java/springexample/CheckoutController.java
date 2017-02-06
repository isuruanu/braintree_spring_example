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

        Result<Customer> customerResult = gateway.customer().create(new CustomerRequest().id("iso-test4").paymentMethodNonce(nonce));


        PaymentMethod paymentMethod = customerResult.getTarget().getPaymentMethods().get(0);
        String customerId = paymentMethod.getCustomerId();
        String token = paymentMethod.getToken();

        TransactionRequest request = new TransactionRequest();
        request.amount(new BigDecimal(10)).paymentMethodToken(token);
        Result<Transaction> sale = gateway.transaction().sale(request);


        SubscriptionRequest subscriptionRequest = new SubscriptionRequest().planId("Apptizer-Bronze").
                paymentMethodToken(token);
        Result<Subscription> subscriptionResult = gateway.subscription().create(subscriptionRequest);

        if (subscriptionResult.isSuccess()) {
            Subscription transaction = subscriptionResult.getTarget();
            Subscription subscription = subscriptionResult.getTarget();
            Result<Subscription> subscriptionUpdate = gateway.subscription().update(subscription.getId(), new SubscriptionRequest().addOns().add().inheritedFromId("Apptizer-POS-API").done().done());
            subscriptionUpdate.isSuccess();
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

    @RequestMapping(value = "/checkouts/{subscriptionId}")
    public String getTransaction(@PathVariable String subscriptionId, Model model) {
        Subscription subscription;
        String planId;

        try {
            subscription = gateway.subscription().find(subscriptionId);
            subscriptionId = subscription.getId();
            planId = subscription.getPlanId();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            return "redirect:/checkouts";
        }

        model.addAttribute("isSuccess", subscription.getStatus().equals(Subscription.Status.PENDING));
        model.addAttribute("subscription", subscription);
        model.addAttribute("subscriptionId", subscriptionId);
        model.addAttribute("planId", planId);

        return "checkouts/show";
    }
}
