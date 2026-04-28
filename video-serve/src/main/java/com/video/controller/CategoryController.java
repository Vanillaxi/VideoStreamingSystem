package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.Result;
import com.video.service.CategoryService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/category/*")
public class CategoryController extends BaseController {
    @MyAutowired
    private CategoryService categoryService;

    @MyMapping(value = "/list", method = "GET")
    public Result listCategories() {
        return Result.success(categoryService.listCategories());
    }
}
