package com.CampusEase.service;

import com.CampusEase.dto.Result;
import com.CampusEase.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);
}
