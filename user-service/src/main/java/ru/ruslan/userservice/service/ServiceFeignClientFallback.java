package ru.ruslan.userservice.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ruslan.userservice.model.Bucket;

import java.util.ArrayList;
import java.util.List;

public class ServiceFeignClientFallback implements ServiceFeignClient {

    Logger logger =  LoggerFactory.getLogger(this.getClass());
    private final Throwable cause;

    public ServiceFeignClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public List<Bucket>  getAllEmployeesList () {
        if (cause instanceof FeignException && ((FeignException) cause).status() == 404)  {
            logger.error("404 page not found"
                    + "error message: " + cause.getLocalizedMessage());
        } else {
            logger.error("Other error took place: " + cause.getLocalizedMessage());
        }

        return new ArrayList();
    }



}