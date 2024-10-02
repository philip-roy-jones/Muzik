package xyz.philipjones.muzik.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;

public class ResponseHandler {

    public static void handleResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() == 200) {
            // Parse the response body to extract the access token
            String responseBody = response.body();
            // Assuming the response is in JSON format
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String accessToken = jsonNode.get("access_token").asText();
            // Store or use the access token as needed
            System.out.println("Access Token: " + accessToken);
        } else {
            System.err.println("Error: " + response.statusCode() + " - " + response.body());
        }
    }
}