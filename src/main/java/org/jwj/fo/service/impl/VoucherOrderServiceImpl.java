package org.jwj.fo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jwj.fo.entity.VoucherOrder;
import org.jwj.fo.mapper.VoucherOrderMapper;
import org.jwj.fo.service.IVoucherOrderService;
import org.springframework.stereotype.Service;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

}
