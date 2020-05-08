package ru.ruslan.userservice.service;

import feign.hystrix.FallbackFactory;
import ru.ruslan.userservice.model.Bucket;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StatisticFallbackFactory implements FallbackFactory<ServiceFeignClient> {


    @Override
    public ServiceFeignClient create(Throwable throwable) {
        return new  ServiceFeignClientFallback(throwable);
    }
}
