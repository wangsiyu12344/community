package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode" , defaultValue = "0") int orderMode){
        //方法调用前，DispatcherServlet会自动实例化Model和Page，并将Page自动注入Model中。
        //所以在thymeleaf中可以直接访问page对象中的数据。不需要model.addAttribute("page",page)
        page.setRows(discussPostService.findDiscussPostRows(0)); //首页，不以用户id来查询
        page.setPath("/index?orderMode="+ orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(list != null){
            for(DiscussPost post : list){
                Map<String, Object> map = new HashMap<>();
                //把post和相应的user装入map中，再将map放入discussPosts这个集合中。
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                //查找赞的数量
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";
    }

    //拒绝访问时的提示页面
    @RequestMapping(path = "/denied", method = {RequestMethod.GET, RequestMethod.GET})
    public String getDeniedPage() {
        return "/error/404";
    }

}
