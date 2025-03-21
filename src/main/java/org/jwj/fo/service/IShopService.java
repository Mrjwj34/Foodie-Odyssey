package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.Shop;


public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);
}
