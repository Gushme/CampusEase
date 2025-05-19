package com.CampusEase.service;

import com.CampusEase.dto.OrderPaymentDTO;
import com.CampusEase.dto.Result;
import com.CampusEase.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    void handleVoucherOrder(VoucherOrder voucherOrder);

    Result seckillVoucher(Long voucherId);

    Result limitVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);

    Result payment(OrderPaymentDTO orderPaymentDTO);
}
