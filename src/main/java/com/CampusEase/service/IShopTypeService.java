package com.CampusEase.service;

import com.CampusEase.dto.Result;
import com.CampusEase.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IShopTypeService extends IService<ShopType> {

    Result queryList();
}
