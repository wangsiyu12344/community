package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping(path = "/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    //当前用户，从hostHolder中取出
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model){
        //生成上传文件的名称
        String fileName = CommunityUtil.generateUUID();
        //设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJsonString(0));

        //生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName", fileName);


        return "/site/setting";
    }

    //更新头像的路径
    @RequestMapping(path = "/header/url",method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName){
        if(StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1, "文件名不能为空");
        }
        //头像在七牛云服务器的访问路径
        String url = headerBucketUrl + "/"+fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJsonString(0);

    }


    //上传头像  //废弃，改为上传头像到七牛云云服务器
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){
            model.addAttribute("error", "您还没有选择图片");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件的格式不正确");
            return "/site/setting";
        }

        //生成随机的文件名
        fileName = CommunityUtil.generateUUID()  + suffix;
        //确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            //把文件存储到指定路径
            headerImage.transferTo(dest);
        } catch (IOException e) {
           logger.error("上传文件失败:" + e.getMessage());
           throw new RuntimeException("上传文件失败, 服务器发生异常", e);
        }
        //更新当前用户的头像的路径(web路径）,从hostholder中取出当前用户
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    //获取头像 //废弃，改为从七牛云服务器获取头像
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    //返回的图片是二进制的数据
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //找到服务器上存储的头像图片的路径
        fileName = uploadPath + "/" + fileName;
        //向浏览器输出图片
        //文件的后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        response.setContentType("image/"+ suffix);
        try (FileInputStream fis = new FileInputStream(fileName);
             OutputStream os = response.getOutputStream();
             ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败:" + e.getMessage());
        }


    }

    //修改密码
    @LoginRequired
    @RequestMapping(path = "/updatepassword", method = RequestMethod.POST)
    public String updatePassword(String oldpassword, String npassword, Model model){
        if(oldpassword.equals(npassword)){
            model.addAttribute("oldpasswordMsg", "新旧密码不能一样");
            model.addAttribute("npasswordMsg", "新旧密码不能一样");
            return "/site/setting";
        }
        //得到当前用户,校验用户密码是否正确
        User user = hostHolder.getUser();
        oldpassword = CommunityUtil.md5(oldpassword + user.getSalt());
       if(!oldpassword.equals(user.getPassword())){
            model.addAttribute("oldpasswordMsg", "密码输入错误");
           return "/site/setting";
        }
        userService.updatePassword(user.getId(), npassword);
        return "redirect:/index";
    }
    //个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在");
        }
        //把用户传给页面
        model.addAttribute("user", user);
        //查该用户获得的点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        //查询关注用户的数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //查询该用户粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        //当前登录用户对这个用户是否已关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

}
