package com.minio.controller;


import cn.hutool.http.HttpStatus;
import com.minio.entity.MessageVo;
import com.minio.entity.ResponseEntry;
import com.minio.service.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author WangXiaoLong
 * @Date 2022/7/1 13:51
 * @Version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/sse")
public class SseEmitterController {

    @Resource
    private SseEmitterService sseEmitterService;

    @Autowired
    private ResponseEntry res;

    @GetMapping("/index")
    public ModelAndView modelAndView(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("sse-work");
        return modelAndView;
    }


//    @CrossOrigin
    @GetMapping("/createConnect")
    public SseEmitter createConnect(String clientId) throws IOException {
        res.put("clientId", clientId);
        SseEmitter connect = sseEmitterService.createConnect(clientId);
        sendMessage();
        return connect;
    }


    private void sendMessage() throws IOException {
        sseEmitterService.sendResMapToOneClient(String.valueOf(res.get("clientId")), res);

    }

//    @CrossOrigin
    @PostMapping("/broadcast")
    public void sendMessageToAllClient(@RequestBody(required = false) String msg) {
        sseEmitterService.sendMessageToAllClient(msg);
    }




//    @CrossOrigin
    @GetMapping("/closeConnect")
    public void closeConnect(@RequestParam(required = true) String clientId) {
        sseEmitterService.closeConnect(clientId);
    }

}
