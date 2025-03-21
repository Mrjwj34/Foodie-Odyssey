package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.ShopType;


public interface IShopTypeService extends IService<ShopType> {

    Result getList();
}
