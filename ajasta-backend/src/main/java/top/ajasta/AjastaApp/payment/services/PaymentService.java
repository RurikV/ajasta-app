package top.ajasta.AjastaApp.payment.services;

import top.ajasta.AjastaApp.payment.dtos.PaymentDTO;
import top.ajasta.AjastaApp.response.Response;

import java.util.List;

public interface PaymentService {

    Response<?> initializePayment(PaymentDTO paymentDTO);
    void updatePaymentForOrder(PaymentDTO paymentDTO);
    Response<List<PaymentDTO>> getAllPayments();
    Response<PaymentDTO> getPaymentById(Long paymentId);

}
