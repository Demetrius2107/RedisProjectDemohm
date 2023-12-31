package com.hmdp.service.impl;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  @Override
  public Result queryList() {
    //先从Redis中查询，常量值是固定前缀 + 店铺id
    List<String> shopTypes = stringRedisTemplate.opsForList().range(CACHE_SHOP_KEY,0,-1);
    //如果不为空，转换ShopType类型直接返回
    if(!shopTypes.isEmpty()){
     /* List<ShopType> tmp = new ArrayList<>();
      for(String types: shopTypes){
        ShopType shopType = JSONUtil.toBean(types, ShopType.class);
        tmp.add(shopType);
      }*/
      List<ShopType> tmp = shopTypes.stream().map(type->JSONUtil.toBean(type,ShopType.class)).collect(
          Collectors.toList());
      return  Result.ok(tmp);
    }
    //否则去数据库查
    List<ShopType> tmp = query().orderByAsc("sort").list();
    if(tmp == null){
      return Result.fail("店铺类型不存在");
    }
    //查到了转为json字符串，存入Redis
    /*for(ShopType shopType:tmp){
      String jsonStr = JSONUtil.toJsonStr(shopType);
      shopTypes.add(jsonStr);
    }*/
    shopTypes = tmp.stream().map(type->JSONUtil.toJsonStr(type)).collect(Collectors.toList());
    stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOP_KEY,shopTypes);
    //最终把查询到的商品分类信息返回给前端
    return Result.ok(tmp);
  }

}
