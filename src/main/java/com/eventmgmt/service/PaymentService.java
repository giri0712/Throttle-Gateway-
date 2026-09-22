package com.eventmgmt.service;

import com.eventmgmt.exception.PaymentFailedException;
import com.eventmgmt.model.PaymentTransaction;
import com.eventmgmt.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    public PaymentService(PaymentTransactionRepository paymentTransactionRepository, RazorpayClient razorpayClient) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.razorpayClient = razorpayClient;
    }

    /**
     * Create a Razorpay Order.
     * @param amount The amount in the smallest currency unit (e.g., paise for INR).
     * @param receipt A unique receipt ID for the order.
     * @return The Razorpay Order ID.
     */
    public String createRazorpayOrder(Double amount, String receipt) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) (amount * 100)); // Razorpay expects amount in paise
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);
        orderRequest.put("payment_capture", 1); // Auto-capture

        Order order = razorpayClient.Orders.create(orderRequest);
        return order.get("id");
    }

    /**
     * Verify the Razorpay payment signature using HMAC SHA256.
     * @param orderId The Razorpay Order ID.
     * @param paymentId The Razorpay Payment ID.
     * @param signature The Razorpay Signature.
     */
    public void verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String expectedSignature = generateHmacSha256(payload, razorpayKeySecret);

            if (!expectedSignature.equals(signature)) {
                throw new PaymentFailedException("Payment verification failed: Invalid signature.");
            }
        } catch (Exception e) {
            if (e instanceof PaymentFailedException) {
                throw (PaymentFailedException) e;
            }
            throw new PaymentFailedException("Payment verification error: " + e.getMessage());
        }
    }

    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Save a successful payment transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction saveSuccessfulTransaction(Double amount, String orderId, String paymentId, String userName) {
        String transactionId = "TXN_RP_" + paymentId.substring(0, Math.min(16, paymentId.length())).toUpperCase();
        
        PaymentTransaction transaction = new PaymentTransaction(
                transactionId,
                amount,
                "SUCCESS",
                userName,
                "Razorpay-" + paymentId.substring(Math.max(0, paymentId.length() - 4)),
                LocalDateTime.now()
        );

        return paymentTransactionRepository.save(transaction);
    }
}
