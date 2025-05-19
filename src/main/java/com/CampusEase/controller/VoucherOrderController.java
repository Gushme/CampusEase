package com.CampusEase.controller;


import com.CampusEase.dto.OrderPaymentDTO;
import com.CampusEase.dto.Result;
import com.CampusEase.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @PostMapping("limit/{id}")
    public Result limitVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.limitVoucher(voucherId);
    }

    @PostMapping("/payment")
    public Result payment(@RequestBody OrderPaymentDTO orderPaymentDTO) {
        return voucherOrderService.payment(orderPaymentDTO);
    }
}
