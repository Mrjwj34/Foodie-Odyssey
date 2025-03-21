package org.jwj.fo.controller;


import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.ShopType;
import org.jwj.fo.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 通过使用缓存将响应时间从200ms减少到50ms
     * @return
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.getList();
    }
}
