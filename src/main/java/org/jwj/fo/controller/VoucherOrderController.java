package org.jwj.fo.controller;


import org.jwj.fo.dto.Result;
import org.jwj.fo.service.ISeckillVoucherService;
import org.jwj.fo.service.IVoucherOrderService;
import org.jwj.fo.service.IVoucherService;
import org.jwj.fo.service.impl.VoucherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.jwj.fo.utils.RedisConstants.LOCK_SERVICE_ORDER_PREFIX;


@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.secKillVoucher(voucherId);
    }
}
