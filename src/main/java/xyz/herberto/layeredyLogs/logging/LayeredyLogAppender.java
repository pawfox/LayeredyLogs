package xyz.herberto.layeredyLogs.logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class LayeredyLogAppender extends AbstractAppender {

    private static final String ENDPOINT = "https://app.layeredy.com/api/logs/ingest";
    private static final int BATCH_SIZE = 25;
    private static final long FLUSH_INTERVAL_MS = 1000L;

    private final String bearerToken;
    private final String logToken;
    private final String serviceName;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final LinkedBlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread flushThread;

    public LayeredyLogAppender(String bearerToken, String logToken, String serviceName) {
        super("LayeredyLogAppender", null, null, false, null);
        this.bearerToken = bearerToken;
        this.logToken = logToken;
        this.serviceName = serviceName;
    }

    public void install() {
        start();
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(this);

        flushThread = new Thread(this::flushLoop, "LayeredyLogAppender-Flush");
        flushThread.setDaemon(true);
        flushThread.start();
    }

    public void uninstall() {
        running.set(false);
        if (flushThread != null) {
            flushThread.interrupt();
        }
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(this);
        flushBatch(drainAll());
        stop();
    }

    @Override
    public void append(LogEvent event) {
        JsonObject line = new JsonObject();
        line.addProperty("timestamp", DateTimeFormatter.ISO_INSTANT.format(
                Instant.ofEpochMilli(event.getTimeMillis())));
        line.addProperty("level", event.getLevel().toString().toLowerCase());
        line.addProperty("service", serviceName);
        line.addProperty("message", event.getMessage().getFormattedMessage());

        JsonObject attributes = new JsonObject();
        attributes.addProperty("logger", event.getLoggerName());
        attributes.addProperty("thread", event.getThreadName());
        if (event.getThrown() != null) {
            attributes.addProperty("exception", event.getThrown().toString());
        }
        line.add("attributes", attributes);

        queue.offer(line);

        if (queue.size() >= BATCH_SIZE) {
            flushBatch(drainAll());
        }
    }

    private void flushLoop() {
        while (running.get()) {
            try {
                Thread.sleep(FLUSH_INTERVAL_MS);
                if (!queue.isEmpty()) {
                    flushBatch(drainAll());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private List<JsonObject> drainAll() {
        List<JsonObject> batch = new ArrayList<>();
        JsonObject item;
        while ((item = queue.poll()) != null) {
            batch.add(item);
        }
        return batch;
    }

    private void flushBatch(List<JsonObject> lines) {
        if (lines.isEmpty()) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("token", logToken);
        payload.add("lines", gson.toJsonTree(lines));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    int status = response.statusCode();
                    if (status >= 200 && status < 300) {
                        return;
                    }
                    System.err.println("[LayeredyLogs] ingest failed with " + status + " ("
                            + describeStatus(status) + ") for " + lines.size() + " line(s): "
                            + response.body());
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private String describeStatus(int status) {
        return switch (status) {
            case 400 -> "missing lines[] array";
            case 403 -> "missing/invalid token, or the owner's plan does not include Logs";
            case 413 -> "batch too large (over 500 lines or 1 MB)";
            case 429 -> "rate limit exceeded";
            case 500 -> "internal error";
            default -> "unexpected status";
        };
    }
}