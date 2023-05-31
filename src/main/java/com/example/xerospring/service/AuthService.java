package com.example.xerospring.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.xerospring.util.TokenStorage;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.xero.api.ApiClient;
import com.xero.api.client.IdentityApi;
import com.xero.models.identity.Connection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AuthService {


    @Value(value = "${xero.clientId}")
    String clientId;

    @Value(value = "${xero.clientSecret}")
    String clientSecret;

    @Value(value = "${xero.redirectURI}")
    String redirectURI;

    @Value(value = "${xero.token.server.url}")
    String tokenServerUrl;

    @Value(value = "${xero.auth.server.url}")
    String authServerUrl;
    NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    JsonFactory JSON_FACTORY = new JacksonFactory();
    String secretState = "secret" + new Random().nextInt(999_999);
    ApiClient defaultClient = new ApiClient();
    ArrayList<String> scopeList = new ArrayList<String>();

    private AuthService() {
        scopeList.add("openid");
        scopeList.add("email");
        scopeList.add("profile");
//        scopeList.add("offline_access");
//        scopeList.add("accounting.settings");
        scopeList.add("accounting.transactions");
        scopeList.add("accounting.contacts");
        scopeList.add("accounting.journals.read");
        scopeList.add("accounting.reports.read");
//        scopeList.add("accounting.attachments");
    }

    public String buildAuthUrl(HttpServletResponse response) throws IOException {

        // Save your secretState variable and compare in callback to prevent CSRF
        TokenStorage store = new TokenStorage();
        store.saveItem(response, "state", secretState);

        DataStoreFactory DATA_STORE_FACTORY = new MemoryDataStoreFactory();
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                HTTP_TRANSPORT, JSON_FACTORY, new GenericUrl(tokenServerUrl),
                new ClientParametersAuthentication(clientId, clientSecret), clientId, authServerUrl)
                .setScopes(scopeList).setDataStoreFactory(DATA_STORE_FACTORY).build();
        return flow.newAuthorizationUrl().setClientId(clientId).setScopes(scopeList).setState(secretState)
                .setRedirectUri(redirectURI).build();
    }

    public boolean callBack(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String code = "123";
        if (request.getParameter("code") != null) {
            code = request.getParameter("code");
        }

        // Retrieve your stored secretState variable
        TokenStorage store = new TokenStorage();
        String secretState = store.get(request, "state");

        if (request.getParameter("state") != null && secretState.equals(request.getParameter("state").toString())) {

            DataStoreFactory DATA_STORE_FACTORY = new MemoryDataStoreFactory();

            AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                    HTTP_TRANSPORT, JSON_FACTORY, new GenericUrl(tokenServerUrl),
                    new ClientParametersAuthentication(clientId, clientSecret), clientId, authServerUrl)
                    .setScopes(scopeList).setDataStoreFactory(DATA_STORE_FACTORY).build();

            TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();


            try {
                DecodedJWT verifiedJWT = defaultClient.verify(tokenResponse.getAccessToken());

                ApiClient defaultIdentityClient = new ApiClient("https://api.xero.com", null, null, null, null);
                IdentityApi idApi = new IdentityApi(defaultIdentityClient);
                List<Connection> connection = idApi.getConnections(tokenResponse.getAccessToken(), null);

//                store.saveItem(response, "token_set", tokenResponse.toPrettyString());
//                store.saveItem(response, "access_token", verifiedJWT.getToken());
//                store.saveItem(response, "refresh_token", tokenResponse.getRefreshToken());
//                store.saveItem(response, "expires_in_seconds", tokenResponse.getExpiresInSeconds().toString());
//                store.saveItem(response, "xero_tenant_id", connection.get(0).getTenantId().toString());

                request.getSession(true).setAttribute("access_token", verifiedJWT.getToken());
                request.getSession(true).setAttribute("refresh_token", tokenResponse.getRefreshToken());
                request.getSession(true).setAttribute("xero_tenant_id", connection.get(0).getTenantId().toString());
                request.getSession(true).setAttribute("id_token", tokenResponse.get("id_token").toString());

                response.sendRedirect("/home?token=");
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
