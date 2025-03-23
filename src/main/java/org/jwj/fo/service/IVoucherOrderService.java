package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.VoucherOrder;
import org.springframework.transaction.annotation.Transactional;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result secKillVoucher(Long voucherId, String serviceType);

    Result createVoucherOrder(Long voucherId);
}
