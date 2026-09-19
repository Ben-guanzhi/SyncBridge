package com.sync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 前端 SPA 路由回退（生产环境）
 * 所有非 /api/* 的请求都返回 index.html，由 Vue Router 处理前端路由
 */
@Controller
public class FrontendController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
