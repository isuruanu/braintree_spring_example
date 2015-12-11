package service;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class CheckoutControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void checkoutReturnsOK() throws Exception {
        mockMvc.perform(get("/checkouts"))
            .andExpect(status().isOk());
    }

    @Test
    public void rendersNewView() throws Exception {
        mockMvc.perform(get("/checkouts"))
            .andExpect(view().name("checkouts/new"))
            .andExpect(model().hasNoErrors())
            .andExpect(model().attributeExists("clientToken"))
            .andExpect(xpath("//script[@src='https://js.braintreegateway.com/v2/braintree.js']").exists());
    }

    @Test
    public void redirectsOnTransactionNotFound() throws Exception {
        mockMvc.perform(post("/checkouts/invalid-transaction"))
            .andExpect(status().isFound());
    }
}