package com.cinex.cinema.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tích hợp PayOS (cổng thanh toán QR).
 * Cấu hình qua biến môi trường PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY.
 */
@Service
public class PayOSService {

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payos.base-url:https://api-merchant.payos.vn}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final java.util.concurrent.atomic.AtomicLong ORDER_SEQ =
        new java.util.concurrent.atomic.AtomicLong();

    /**
     * Sinh orderCode duy nhất toàn cục cho PayOS.
     * Không dùng id trong DB vì id có thể trùng giữa các môi trường hoặc sau khi tạo lại dữ liệu.
     */
    public static long generateOrderCode() {
        return System.currentTimeMillis() * 1000 + (ORDER_SEQ.incrementAndGet() % 1000);
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && apiKey != null && !apiKey.isBlank()
            && checksumKey != null && !checksumKey.isBlank();
    }

    /** Tạo link thanh toán PayOS. Trả về checkoutUrl, qrCode, paymentLinkId. */
    @SuppressWarnings("unchecked")
    public PayOSLink createPaymentLink(long orderCode, long amount, String description,
                                      String returnUrl, String cancelUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("PayOS chưa được cấu hình (thiếu PAYOS_CLIENT_ID/API_KEY/CHECKSUM_KEY)");
        }
        // Chữ ký: HMAC-SHA256(checksumKey, "amount=..&cancelUrl=..&description=..&orderCode=..&returnUrl=..")
        String data = "amount=" + amount
            + "&cancelUrl=" + cancelUrl
            + "&description=" + description
            + "&orderCode=" + orderCode
            + "&returnUrl=" + returnUrl;
        String signature = hmacSha256(checksumKey, data);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("returnUrl", returnUrl);
        body.put("cancelUrl", cancelUrl);
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> resp = restTemplate.postForObject(baseUrl + "/v2/payment-requests", entity, Map.class);
        if (resp == null || !"00".equals(resp.get("code"))) {
            throw new RuntimeException("PayOS tạo link thất bại: " + (resp == null ? "null" : resp.get("desc")));
        }
        Map<String, Object> dataObj = (Map<String, Object>) resp.get("data");
        return new PayOSLink(
            (String) dataObj.get("checkoutUrl"),
            (String) dataObj.get("qrCode"),
            (String) dataObj.get("paymentLinkId")
        );
    }

    /**
     * Lấy lại link thanh toán đã tạo (dùng khi người dùng tải lại trang thanh toán).
     * API tra cứu của PayOS không trả lại QR, nên chỉ dựng lại URL cổng thanh toán.
     * Trả về null nếu link không còn ở trạng thái chờ thanh toán.
     */
    public PayOSLink getPaymentLink(String paymentLinkId) {
        Map<String, Object> data = fetchPaymentLink(paymentLinkId);
        if (data == null) {
            return null;
        }
        Object status = data.get("status");
        if (status != null && !"PENDING".equals(String.valueOf(status))) {
            return null;
        }
        return new PayOSLink(
            "https://pay.payos.vn/web/" + paymentLinkId,
            null,
            paymentLinkId
        );
    }

    /**
     * Trạng thái hiện tại của link thanh toán bên PayOS (PENDING/PAID/CANCELLED/EXPIRED).
     * Trả về null nếu không lấy được.
     */
    public String getPaymentStatus(String paymentLinkId) {
        Map<String, Object> data = fetchPaymentLink(paymentLinkId);
        if (data == null || data.get("status") == null) {
            return null;
        }
        return String.valueOf(data.get("status"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPaymentLink(String paymentLinkId) {
        if (!isConfigured() || paymentLinkId == null || paymentLinkId.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> resp = restTemplate.exchange(
                baseUrl + "/v2/payment-requests/" + paymentLinkId, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null || !"00".equals(body.get("code"))) {
                return null;
            }
            return (Map<String, Object>) body.get("data");
        } catch (Exception e) {
            return null;
        }
    }

    /** Xác minh chữ ký webhook PayOS. */
    public boolean verifyWebhookSignature(Map<String, Object> webhookData) {
        if (!isConfigured()) return false;
        Object signatureObj = webhookData.get("signature");
        if (signatureObj == null) return false;
        String receivedSig = signatureObj.toString();

        Object dataObj = webhookData.get("data");
        if (!(dataObj instanceof Map)) return false;
        Map<String, Object> data = (Map<String, Object>) dataObj;

        // Sắp xếp key và build chuỗi "key=value&..."
        TreeMap<String, Object> sorted = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (e.getValue() == null) continue;
            if (!first) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        String computed = hmacSha256(checksumKey, sb.toString());
        return computed.equalsIgnoreCase(receivedSig);
    }

    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi HMAC-SHA256", e);
        }
    }

    /** Kết quả tạo link PayOS. */
    public static class PayOSLink {
        public final String checkoutUrl;
        public final String qrCode;
        public final String paymentLinkId;

        public PayOSLink(String checkoutUrl, String qrCode, String paymentLinkId) {
            this.checkoutUrl = checkoutUrl;
            this.qrCode = qrCode;
            this.paymentLinkId = paymentLinkId;
        }
    }
}
