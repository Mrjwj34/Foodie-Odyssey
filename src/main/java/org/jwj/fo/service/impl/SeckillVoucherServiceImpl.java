package org.jwj.fo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.SeckillVoucher;
import org.jwj.fo.entity.VoucherOrder;
import org.jwj.fo.mapper.SeckillVoucherMapper;
import org.jwj.fo.service.ISeckillVoucherService;
import org.jwj.fo.utils.RedisIdWorker;
import org.jwj.fo.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;


@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
