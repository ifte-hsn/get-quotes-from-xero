package com.example.xerospring.controller;

import com.example.xerospring.util.TokenRefresh;
import com.xero.api.ApiClient;
import com.xero.api.XeroException;
import com.xero.api.client.AccountingApi;
import com.xero.models.accounting.Quotes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/home")
    public String home(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws IOException {

        String status = "sent";
        if (request.getParameter("status") != null) {
            status = request.getParameter("status");
        }

        String savedAccessToken = (String) request.getSession(true).getAttribute("access_token");
        String savedRefreshToken = (String) request.getSession(true).getAttribute("refresh_token");
        String xeroTenantId = (String) request.getSession(true).getAttribute("xero_tenant_id");

        if (savedAccessToken == null) {
            response.sendRedirect("/login");
            return null;
        }

        // Check expiration of token and refresh if necessary
        // This should be done prior to each API call to ensure your accessToken is valid
        String accessToken = new TokenRefresh().checkToken(savedAccessToken, savedRefreshToken, response);

        // Init AccountingApi client
        ApiClient defaultClient = new ApiClient();
        // Get Singleton - instance of accounting client
        AccountingApi accountingApi = AccountingApi.getInstance(defaultClient);

        try {


            Integer page = 1;
            String order = "Status ASC";


            Quotes result = accountingApi.getQuotes(accessToken, xeroTenantId, null, null, null, null, null, null, status, page, order, null);
            modelMap.put("quotes", result.getQuotes());
            modelMap.put("status", status);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "home";
    }

    @GetMapping("getQuoteById")
    public String getQuoteById(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws IOException {
        UUID quoteID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        if (request.getParameter("quoteID") != null) {
            quoteID = UUID.fromString(request.getParameter("quoteID"));
        }


        String savedAccessToken = (String) request.getSession(true).getAttribute("access_token");
        String savedRefreshToken = (String) request.getSession(true).getAttribute("refresh_token");
        String xeroTenantId = (String) request.getSession(true).getAttribute("xero_tenant_id");

        if (savedAccessToken == null) {
            response.sendRedirect("/login");
            return null;
        }

        // Check expiration of token and refresh if necessary
        // This should be done prior to each API call to ensure your accessToken is valid
        String accessToken = new TokenRefresh().checkToken(savedAccessToken, savedRefreshToken, response);

        // Init AccountingApi client
        ApiClient defaultClient = new ApiClient();
        // Get Singleton - instance of accounting client
        AccountingApi accountingApi = AccountingApi.getInstance(defaultClient);


        try {
            Quotes result = accountingApi.getQuote(accessToken, xeroTenantId, quoteID);
            System.out.println(result);
            modelMap.put("quotes", result.getQuotes());
        } catch (XeroException e) {
            System.err.println("Exception when calling AccountingApi#getQuote");
            e.printStackTrace();
        }

        return "getQuoteById";
    }
}
