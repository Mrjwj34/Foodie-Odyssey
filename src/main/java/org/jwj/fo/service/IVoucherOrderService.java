package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.VoucherOrder;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result secKillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
    Result createVoucherOrder(VoucherOrder voucherOrder);

    void orderOperation(List<MapRecord<String, Object, Object>> read, String queueName);
}
