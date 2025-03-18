package com.spot.taxi.map.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.constant.SystemConstant;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.util.LocationUtil;
import com.spot.taxi.driver.client.DriverInfoFeignClient;
import com.spot.taxi.map.repository.OrderServiceLocationRepository;
import com.spot.taxi.map.service.LocationService;
import com.spot.taxi.model.entity.driver.DriverSet;
import com.spot.taxi.model.entity.map.OrderServiceLocation;
import com.spot.taxi.model.form.map.OrderServiceLocationForm;
import com.spot.taxi.model.form.map.SearchNearbyDriverForm;
import com.spot.taxi.model.form.map.UpdateDriverLocationForm;
import com.spot.taxi.model.form.map.UpdateOrderLocationForm;
import com.spot.taxi.model.vo.map.NearByDriverVo;
import com.spot.taxi.model.vo.map.OrderLocationVo;
import com.spot.taxi.model.vo.map.OrderServiceLastLocationVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;

import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {
    
    private final RedisTemplate redisTemplate;
    
    private final DriverInfoFeignClient driverInfoFeignClient;
    
    private final OrderServiceLocationRepository orderServiceLocationRepository;
    
    private final MongoTemplate mongoTemplate;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        System.out.println(updateDriverLocationForm);
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());
        System.out.println(point);
        Long add = redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
        System.out.println(add);
        // todo 这里始终返回的是true
        return true;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        // todo 这里始终返回的是true
        return true;
    }

    @Override
    public List<NearByDriverVo> searchNearbyDriver(SearchNearbyDriverForm searchNearbyDriverForm) {
        // 获取附近司机
        Point point = new Point(searchNearbyDriverForm.getLongitude().doubleValue(),
                searchNearbyDriverForm.getLatitude().doubleValue());
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS);
        Circle circle = new Circle(point, distance);
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates().sortAscending();
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        assert results != null;
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (CollectionUtils.isEmpty(content)) {
            return List.of();
        }
        Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();

        List<NearByDriverVo> list = new ArrayList<>();
        while (iterator.hasNext()) {

            GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();
            Long driverId = Long.parseLong(item.getContent().getName());
            // todo这里循环调用要改一下，可以传入一个driverId list，然后直接在远程返回一个driverSettingResult list
            // todo如果redis中有一个司机，但是driverset中没有的话，就会报错，要改进一下
            Result<DriverSet> driverSettingResult = driverInfoFeignClient.getDriverSetting(driverId);
            DriverSet driverSet = driverSettingResult.getData();

            BigDecimal orderMileageLimit = driverSet.getOrderMileageLimit();

            if (orderMileageLimit.compareTo(BigDecimal.valueOf(0)) != 0 &&
                    orderMileageLimit.compareTo(BigDecimal.valueOf(searchNearbyDriverForm.getMileageDistance().doubleValue())) < 0) {
                continue;
            }

            BigDecimal acceptDistanceLimit = driverSet.getDriverPassengerDistanceLimit();
            BigDecimal currentDistance = BigDecimal.valueOf(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);

            if (acceptDistanceLimit.compareTo(BigDecimal.valueOf(0)) != 0 &&
                    acceptDistanceLimit.compareTo(currentDistance) < 0) {
                continue;
            }

            //封装复合条件数据
            NearByDriverVo nearByDriverVo = new NearByDriverVo();
            nearByDriverVo.setDriverId(driverId);
            nearByDriverVo.setDistance(currentDistance);
            list.add(nearByDriverVo);
        }

        return list;
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());

        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        // todo这里不用转成字符串吗，之前有的地方使用jsonobject转成字符串
        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(orderLocationVo), RedisConstant.UPDATE_ORDER_LOCATION_EXPIRES_TIME, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
        // todo这里不用转成字符串吗，之前有的地方使用jsonobject转成字符串
        String OrderLocationVoJson = (String) redisTemplate.opsForValue().get(key);
        return JSONObject.parseObject(OrderLocationVoJson, OrderLocationVo.class);
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        List<OrderServiceLocation> list = new ArrayList<>();
        //OrderServiceLocation
        orderLocationServiceFormList.forEach(orderServiceLocationForm -> {
            //orderServiceLocationForm -- OrderServiceLocation
            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            BeanUtils.copyProperties(orderServiceLocationForm, orderServiceLocation);
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(new Date()); // todo这个时间应该是现在的时间吗，应该是走到这个位置的时间吧，目前是用于下边的方法找到最新一条数据

            list.add(orderServiceLocation);
            //orderServiceLocationRepository.save(orderServiceLocation);
        });
        //批量添加到MongoDB
        orderServiceLocationRepository.saveAll(list);
        return true;
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        Query query = new Query();
        System.out.println(orderId);
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Order.desc("createTime")));
        query.limit(1);
        OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        if (orderServiceLocation == null) {
            // todo司机没有开始行程之前，mongodb中是没有数据的
            log.error("订单服务位置信息未从mongodb中查出");
            return orderServiceLastLocationVo;
//            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        BeanUtils.copyProperties(orderServiceLocation, orderServiceLastLocationVo);
        return orderServiceLastLocationVo;
    }

    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        //第二种方式
        //MongoRepository只需要 按照规则 在MongoRepository把查询方法创建出来就可以了
        // 总体规则：
        //1 查询方法名称 以 get  |  find  | read开头
        //2 后面查询字段名称，满足驼峰式命名，比如OrderId
        //3 字段查询条件添加关键字，比如Like  OrderBy   Asc
        List<OrderServiceLocation> list = orderServiceLocationRepository.findByOrderIdOrderByCreateTimeAsc(orderId);

        double totalMileage = 0;
        if (!CollectionUtils.isEmpty(list)) {
            for (int i = 0, step = list.size() - 1; i < step; i++) {
                OrderServiceLocation location0 = list.get(i);
                OrderServiceLocation location1 = list.get(i + 1);

                double stepDistance = LocationUtil.getDistance(location0.getLatitude().doubleValue(), location0.getLongitude().doubleValue(),
                        location1.getLatitude().doubleValue(), location1.getLongitude().doubleValue());
                totalMileage += stepDistance;
            }
        }
        return new BigDecimal(totalMileage);
    }
}
