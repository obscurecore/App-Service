package ru.ruslan.userservice.controller;

import ru.ruslan.userservice.model.Bucket;
import ru.ruslan.userservice.service.ServiceFeignClient;
import ru.ruslan.userservice.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Communicate with gallery-service and will be gotten data from DB through gallery-service.
 * So did't directly communicate with DB
 */
@RestController
@RequestMapping("/")
public class UserController {

    //Reading environment variables
    @Autowired
    private Environment env;
    @Autowired
    private TestService service;
    @Autowired ServiceFeignClient serviceFeignClient;

    @RequestMapping("/")
    public String home() {
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from User-Service running at port: " + env.getProperty("local.server.port");
    }

    // Using Feign Client
    @RequestMapping(path = "/getAllDataFromGalleryService")
    public List<Bucket> getData(Model model) {
        //List<Bucket> list = ServiceFeignClient.FeignHolder.create().getAllEmployeesList();
        List<Bucket> list = serviceFeignClient.getAllEmployeesList();
//        model.addAttribute("employees", list);
//        return "resultlist-employees";
        return list;
    }

    // Using RestTemplate
    @GetMapping("/data")
    public String data(){
        return service.data();
    }

}
