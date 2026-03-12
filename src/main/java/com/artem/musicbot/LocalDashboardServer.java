package com.artem.musicbot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class LocalDashboardServer {
    private final int port;
    private final Supplier<MusicController.MetricsSnapshot> metricsSupplier;
    private final Supplier<String> healthSupplier;
    private final Supplier<String> jdaStatusSupplier;
    private final Instant startedAt;

    private HttpServer server;

    public LocalDashboardServer(
            int port,
            Supplier<MusicController.MetricsSnapshot> metricsSupplier,
            Supplier<String> healthSupplier,
            Supplier<String> jdaStatusSupplier,
            Instant startedAt
    ) {
        this.port = port;
        this.metricsSupplier = metricsSupplier;
        this.healthSupplier = healthSupplier;
        this.jdaStatusSupplier = jdaStatusSupplier;
        this.startedAt = startedAt;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handleDashboard);
        server.createContext("/metrics", this::handleMetrics);
        server.start();
    }

    public synchronized void stop() {
        if (server == null) {
            return;
        }

        server.stop(0);
        server = null;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondText(exchange, 405, "Method Not Allowed");
            return;
        }

        MusicController.MetricsSnapshot snapshot = metricsSupplier.get();
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>ModernMusicBot Dashboard</title>
                  <style>
                    :root { --bg:#0b1220; --card:#111a2b; --text:#e7edf7; --muted:#98a6c2; --ok:#36c174; --warn:#f0b429; }
                    body { margin:0; font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Arial; background: radial-gradient(circle at top, #1a2744 0%%, var(--bg) 55%%); color:var(--text); }
                    .wrap { max-width: 920px; margin: 32px auto; padding: 0 16px; }
                    .title { font-size: 28px; margin: 0 0 8px; }
                    .sub { color: var(--muted); margin: 0 0 20px; }
                    .grid { display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:12px; }
                    .card { background: var(--card); border:1px solid #223455; border-radius: 12px; padding: 14px; }
                    .label { color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: 0.06em; }
                    .value { font-size: 24px; margin-top: 6px; }
                    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 13px; }
                    .ok { color: var(--ok); }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <h1 class="title">ModernMusicBot Dashboard</h1>
                    <p class="sub">Local-only runtime panel. Auto-refresh every 5s.</p>
                    <div class="grid">
                      <div class="card"><div class="label">Bot Status</div><div id="status" class="value ok">%s</div></div>
                      <div class="card"><div class="label">Tracked Guilds</div><div id="trackedGuilds" class="value">%d</div></div>
                      <div class="card"><div class="label">Active Players</div><div id="activePlayers" class="value">%d</div></div>
                      <div class="card"><div class="label">Queued Tracks</div><div id="queuedTracks" class="value">%d</div></div>
                      <div class="card"><div class="label">Load Success</div><div id="loadSuccess" class="value">%d</div></div>
                      <div class="card"><div class="label">Load Failures</div><div id="loadFailures" class="value">%d</div></div>
                      <div class="card"><div class="label">No Matches</div><div id="noMatches" class="value">%d</div></div>
                      <div class="card"><div class="label">Uptime</div><div id="uptime" class="value">%s</div></div>
                    </div>
                                        <div class="card" style="margin-top:12px;">
                                            <div class="label">Player</div>
                                            <div id="playerTitle" class="value" style="font-size:20px;">%s</div>
                                            <div id="playerState" class="mono" style="margin-top:6px; color:var(--muted);">State: %s</div>
                                            <div id="playerPosition" class="mono" style="margin-top:4px;">Position: %s / %s</div>
                                        </div>
                    <div class="card" style="margin-top:12px;">
                      <div class="label">Health Summary</div>
                      <pre id="health" class="mono">%s</pre>
                    </div>
                                        <div class="card" style="margin-top:12px;">
                                            <div class="label">Queue Preview</div>
                                            <div id="playerGuild" class="mono" style="margin-bottom:8px; color:var(--muted);">Guild: %s</div>
                                            <pre id="queuePreview" class="mono">%s</pre>
                                        </div>
                    <script>
                                            function formatMs(ms) {
                                                if (!Number.isFinite(ms) || ms <= 0) return '00:00';
                                                const totalSeconds = Math.floor(ms / 1000);
                                                const hours = Math.floor(totalSeconds / 3600);
                                                const minutes = Math.floor((totalSeconds %% 3600) / 60);
                                                const seconds = totalSeconds %% 60;
                                                if (hours > 0) {
                                                    return String(hours).padStart(2, '0') + ':' + String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
                                                }
                                                return String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
                                            }

                      async function refresh() {
                        const r = await fetch('/metrics', {cache: 'no-store'});
                        const m = await r.json();
                        document.getElementById('status').textContent = m.botStatus;
                        document.getElementById('trackedGuilds').textContent = m.trackedGuilds;
                        document.getElementById('activePlayers').textContent = m.activePlayers;
                        document.getElementById('queuedTracks').textContent = m.queuedTracks;
                        document.getElementById('loadSuccess').textContent = m.loadSuccess;
                        document.getElementById('loadFailures').textContent = m.loadFailures;
                        document.getElementById('noMatches').textContent = m.noMatches;
                        document.getElementById('uptime').textContent = m.uptime;
                                                document.getElementById('playerTitle').textContent = m.nowPlayingTitle;
                                                document.getElementById('playerState').textContent = 'State: ' + m.nowPlayingState;
                                                document.getElementById('playerPosition').textContent = 'Position: ' + formatMs(m.nowPlayingPositionMs) + ' / ' + formatMs(m.nowPlayingDurationMs);
                                                document.getElementById('playerGuild').textContent = 'Guild: ' + m.nowPlayingGuild;
                                                document.getElementById('queuePreview').textContent = m.queuePreview;
                        document.getElementById('health').textContent = m.healthSummary;
                      }
                      setInterval(refresh, 5000);
                    </script>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(jdaStatusSupplier.get()),
                snapshot.trackedGuilds(),
                snapshot.activePlayers(),
                snapshot.queuedTracks(),
                snapshot.loadSuccess(),
                snapshot.loadFailures(),
                snapshot.noMatches(),
                formatUptime(),
                escapeHtml(snapshot.nowPlayingTitle()),
                escapeHtml(snapshot.nowPlayingState()),
                formatMs(snapshot.nowPlayingPositionMs()),
                formatMs(snapshot.nowPlayingDurationMs()),
                escapeHtml(healthSupplier.get()),
                escapeHtml(snapshot.nowPlayingGuild()),
                escapeHtml(snapshot.queuePreview())
        );

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondText(exchange, 405, "Method Not Allowed");
            return;
        }

        MusicController.MetricsSnapshot metrics = metricsSupplier.get();
        String json = "{" +
                "\"botStatus\":\"" + escapeJson(jdaStatusSupplier.get()) + "\"," +
                "\"trackedGuilds\":" + metrics.trackedGuilds() + "," +
                "\"activePlayers\":" + metrics.activePlayers() + "," +
                "\"queuedTracks\":" + metrics.queuedTracks() + "," +
                "\"loadSuccess\":" + metrics.loadSuccess() + "," +
                "\"loadFailures\":" + metrics.loadFailures() + "," +
                "\"noMatches\":" + metrics.noMatches() + "," +
                "\"nowPlayingTitle\":\"" + escapeJson(metrics.nowPlayingTitle()) + "\"," +
                "\"nowPlayingPositionMs\":" + metrics.nowPlayingPositionMs() + "," +
                "\"nowPlayingDurationMs\":" + metrics.nowPlayingDurationMs() + "," +
                "\"nowPlayingState\":\"" + escapeJson(metrics.nowPlayingState()) + "\"," +
                "\"nowPlayingGuild\":\"" + escapeJson(metrics.nowPlayingGuild()) + "\"," +
                "\"queuePreview\":\"" + escapeJson(metrics.queuePreview()) + "\"," +
                "\"uptime\":\"" + formatUptime() + "\"," +
                "\"healthSummary\":\"" + escapeJson(healthSupplier.get()) + "\"" +
                "}";

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void respondText(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String formatUptime() {
        Duration duration = Duration.between(startedAt, Instant.now());
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format(Locale.ROOT, "%dd %02d:%02d:%02d", days, hours, minutes, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String formatMs(long milliseconds) {
        if (milliseconds <= 0L) {
            return "00:00";
        }

        long totalSeconds = milliseconds / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}
