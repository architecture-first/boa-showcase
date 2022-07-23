package com.architecture.first.framework.business.hub;

import com.architecture.first.framework.business.vicinity.queue.Queue;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.util.SimpleModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * A bridge class to communicate with a client
 */
@Slf4j
@Component
public class ClientCommunication {
    public static String METHOD_GET = "GET";
    public static String METHOD_POST = "POST";
    public static String CLIENT = "Client";

    @Autowired
    private Queue queue;

    @Value("${hub.host:none}")
    private String host;

    /**
     * say a message/event to a client
     * @param event
     * @return event
     */
    public ArchitectureFirstEvent say(ArchitectureFirstEvent event) {
        if (host.equals("none")) {
            throw new RuntimeException("Cannot use hub without hub.host set");
        }

        if (event.toFirst().equals(CLIENT)) {
            sendMessage((String)event.header().get("boa-conn"), event.payload());
            return event;
        }

        throw new RuntimeException("Client: " + event.toFirst() + " not supported");
    }

    /**
     * Send a message to a specific user
     * @param conn
     * @param payload
     */
    private void sendMessage(String conn, SimpleModel payload) {
        payload.put("boa-conn", conn);
        queue.push("Hub-messages", payload, SimpleModel.class);
    }

    /**
     * Send a message using a websocket
     * @param path
     * @param jsonMessage
     */
    private void sendMessageByChat(String path, String jsonMessage) {
        var fullPath = String.format("%s/publisher/%s", host, path);
        var httpClient = HttpClient.newHttpClient();
        var wsBuilder = httpClient.newWebSocketBuilder();

        var listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<Void> onText(WebSocket webSocket, CharSequence data, boolean last) {
                webSocket.request(1);
                return CompletableFuture.completedFuture(data)
                        .thenAccept(o -> log.info("received message"));
            }
        };

        try {
            var ws = wsBuilder
                    .buildAsync(URI.create(fullPath), listener)
                    .get(30, TimeUnit.SECONDS);
            var run = ws.sendText(jsonMessage, false);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a message and wait for reply
     * @param path
     * @param method
     * @param headers
     * @param requestBody
     * @return
     */
    private HttpResponse<String> sendMessageAndWait(String path, String method, SimpleModel headers, String requestBody) {
        var fullPath = String.format("%s/%s", host, path);

        var requestBuilder = HttpRequest
                .newBuilder()
                .uri(URI.create(fullPath))
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

        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
