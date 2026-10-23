package com.monobogdan.simpletelegram.transport;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** All R1K1.SYS HTTP/JSON traffic is routed through this class. */
public class ClientManager {
    public interface CredentialsResponse { void onReady(boolean isValid); }
    public interface Response { void onReady(String str); }
    public interface NetworkErrorHandler { void onError(String str, int code); }
    private static ClientManager instance;
    private static final String USER_AGENT = "R1K1.SYS/Android";
    private String token, nodeAddress;
    private NetworkErrorHandler errorHandler;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public static synchronized ClientManager getCurrent() {
        if (instance == null) instance = new ClientManager();
        return instance;
    }
    private ClientManager() {}
    public void setAuthorizationToken(String value) { token = value; }
    public void setNodeAddress(String value) {
        if (value == null) { nodeAddress = ""; return; }
        value = value.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) value = "http://" + value;
        while (value.endsWith("/")) value = value.substring(0, value.length()-1);
        nodeAddress = value;
    }
    public String getNodeAddress() { return nodeAddress == null ? "" : nodeAddress; }
    public void setErrorHandler(NetworkErrorHandler handler) { errorHandler = handler; }

    public void get(String path, Response callback) { request("GET", path, null, callback); }
    public void postJson(String path, JSONObject body, Response callback) { request("POST", path, body, callback); }
    private void request(final String method, final String path, final JSONObject body, final Response callback) {
        executorService.submit(new Runnable() { @Override public void run() {
            HttpURLConnection conn = null;
            try {
                String base = getNodeAddress();
                if (path.startsWith("http://") || path.startsWith("https://")) base = "";
                String p = path.startsWith("/") ? path : "/" + path;
                conn = (HttpURLConnection)new URL(base + p).openConnection();
                conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
                conn.setRequestMethod(method); conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                if (token != null && token.length() > 0) conn.setRequestProperty("X-Auth-Key", token);
                if (body != null) {
                    conn.setDoOutput(true); conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                    OutputStream out = conn.getOutputStream(); out.write(data); out.flush(); out.close();
                }
                int status = conn.getResponseCode();
                java.io.InputStream stream = status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder result = new StringBuilder();
                if (stream != null) { BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)); String line; while ((line=reader.readLine()) != null) result.append(line).append('\n'); reader.close(); }
                if (status < 200 || status >= 400) { notifyError("HTTP " + status + (result.length() > 0 ? ": " + result.toString().trim() : ""), status); return; }
                if (callback != null) callback.onReady(result.toString().trim());
            } catch (Exception e) { notifyError(e.getMessage() == null ? "Network error" : e.getMessage(), 0); }
            finally { if (conn != null) conn.disconnect(); }
        }});
    }
    private void notifyError(String message, int code) { NetworkErrorHandler h = errorHandler; if (h != null) h.onError(message, code); }
    public void checkCredentialsValidity(String node, String accessKey, final CredentialsResponse response) {
        String base = node == null ? "" : node.trim();
        if (!base.startsWith("http://") && !base.startsWith("https://")) base = "http://" + base;
        while (base.endsWith("/")) base = base.substring(0, base.length()-1);
        final String url = base + "/CheckCredentials?auth_key=" + encode(accessKey);
        executorService.submit(new Runnable() { @Override public void run() {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection)new URL(url).openConnection(); conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
                conn.setRequestMethod("GET"); conn.setRequestProperty("User-Agent", USER_AGENT);
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder b = new StringBuilder(); String line; while ((line=r.readLine()) != null) b.append(line.trim()); r.close();
                if (response != null) response.onReady("OK".equals(b.toString()));
            } catch (Exception e) { notifyError(e.getMessage() == null ? "Network error" : e.getMessage(), 0); if (response != null) response.onReady(false); }
            finally { if (conn != null) conn.disconnect(); }
        }});
    }
    private static String encode(String s) { try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; } }
    private String withAuth(String path) { return path + (path.contains("?") ? "&" : "?") + "auth_key=" + encode(token == null ? "" : token); }
    public void queryChats(int count, Response response) { get(withAuth("/QueryChats?count=" + count), response); }
    public void queryMessages(Packets.Chat chat, long count, Response response) { get(withAuth("/QueryMessages?chat_id=" + chat.ID + "&last_message_id=" + chat.MsgId + "&count=" + count), response); }
    public void sendTextMessage(long chat, String msg, long replyTo, Response response) { get(withAuth("/SendMessage?chat_id=" + chat + "&text=" + encode(msg) + "&reply_to=" + replyTo), response); }
    public String getPhotoUrl(String photo) { return getNodeAddress() + "/photos/" + photo; }
    public boolean isResponseSucceeded(String response) { return response != null && (response.equals("OK") || response.contains("\"success\":true") || response.contains("\"status\":\"ok\"")); }
}
