package com.architecture.first.framework.business.actors.external;

import com.architecture.first.framework.technical.util.SimpleModel;
import com.google.gson.Gson;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Utility class for executing REST calls
 */
@Data
public class RestCall {
    public static String METHOD_GET = "GET";
    public static String METHOD_POST = "POST";

    private Map<String,String> headers;
    private String method;
    private String uri;
    private List<String> params;

    /**
     * Execute an event
     * @param headers - headers to add
     * @param requestBody - body to add to request
     * @return HttpResponse
     */
    public HttpResponse<String> execute(SimpleModel headers, String requestBody) {
        String processedUri = uri;
        try {
            if (params.size() > 0) {
                processedUri = appendParams(processedUri);
            }

            var requestBuilder = HttpRequest
                    .newBuilder()
                    .uri(new URI(processedUri))
                    .header("Content-Type","application/json");

            if (headers.size() > 0) {
                headers.entrySet().stream()
                        .filter(h -> !h.getKey().equals("jwtToken"))
                        .filter(h -> h.getValue() instanceof String)
                        .forEach(h -> requestBuilder.headers(h.getKey(), (String) h.getValue()));
            }

            if (method.equals(METHOD_GET)) {
                requestBuilder.GET();
            }
            else {  // support only GET and POST
                requestBuilder.POST((requestBody == null || requestBody.isEmpty())
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(requestBody));
            }

            var request = requestBuilder.build();

            HttpClient client = HttpClient.newHttpClient();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute an event
     * @param headers - headers to add
     * @param requestBody - body to add to request
     * @return HttpResponse
     */
    public HttpResponse<String> execute(SimpleModel headers, SimpleModel requestBody) {
        var results = requestBody;
        String json = null;

        try {
            json = new Gson().toJson(results);
        }
        catch (Exception e)
        {
            if (requestBody.containsKey("data")) {
                Map.Entry<String,Object> entry = (Map.Entry<String,Object>) ((List)requestBody.get("data")).get(0);
                json = new Gson().toJson(entry.getValue());
            }
            else {
                throw new RuntimeException(e);
            }
        }

        return execute(headers, json);
    }

    /**
     * Add parameters to request
     * @param uri
     * @return
     */
    private String appendParams(String uri) {
        String parms = String.join("&", params);
        return uri + ((uri.indexOf("?") > -1) ? "&" : "?") + parms;
    }
}
