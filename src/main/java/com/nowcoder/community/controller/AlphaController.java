package com.nowcoder.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/alpha")
public class AlphaController {
    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello(){
        return "Hello, SpringBoot.";
    }

    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response){
        //获取请求对象、响应对象
        //然后获取请数据
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());

        Enumeration<String> enumeration = request.getHeaderNames();
        while(enumeration.hasMoreElements()){
            String name = enumeration.nextElement();
            String value = request.getHeader(name);
            System.out.println(name + ":" + value);
        }

        System.out.println(request.getParameter("code")); //带参数名，返回参数值

        //返回响应数据到浏览器， 先设置返回的类型，再打印出来
        response.setContentType("text/html;charset=utf-8");
        try(PrintWriter writer = response.getWriter();
        ){
            writer.write("<h1>牛客网</h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //更简便的方式  获取从浏览器传来的请求数据:request; 向浏览器返回响应数据 : response
    //GET请求(默认的)
    //查询所有的学生     /students?current=1&limit=20
    @RequestMapping(path = "/students", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false, defaultValue = "10")int limit){  //这样也能取。
        System.out.println(current);
        System.out.println(limit);

        return "some students";
    }

    // /student/123    参数是路径的一部分
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){
        System.out.println(id);
        return "a student";
    }

    //POST请求： 浏览器向服务器提交请求

    //响应HTML数据到浏览器
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher(){
        ModelAndView mav = new ModelAndView();
        mav.addObject("name", "张三");
        mav.addObject("age", 30);
        //设置模板的路径和名字
        mav.setViewName("/demo/view");
        return mav;
    }

    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model){
        model.addAttribute("name", "北京大学");
        model.addAttribute("age", 80);
        return "/demo/view";
    }

    //向浏览器响应json数据（一般是在异步请求当中)
    //Java对象 -> JSON字符串 -> JS对象   （在跨语言的环境下，JSON字符串很通用)

    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody  //返回的是JSON字符串而不是html
    public Map<String, Object> getEmp(){
        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("age", 23);
        map.put("salary", 8000.00);
        return map;
        //会把map转换为Json字符串
    }

    


}
