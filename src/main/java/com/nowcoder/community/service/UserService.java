package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

   // @Autowired
    //private LoginTicketMapper loginTicketMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //这个方法被调用的频率非常频繁，如果把用户信息缓存到redis中的话，可以提高效率。
    public User findUserById(int userId){
        //return userMapper.selectById(userId);
        User user = getCache(userId);
        if(user == null){
            user = initCache(userId);
        }
        return user;
    }

    //注册方法
    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        //对空值处理
        if(user == null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        //若都不为空，可以做下一步处理，验证账号是否在库里存在
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }
        //注册用户:把用户信息存到数据库，保存之前要把用户的密码加密
        //salt就是生成的随机字符串的前五位
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        //对用户输入的密码+salt进行md5算法加密
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        //激活码
        user.setActivationCode(CommunityUtil.generateUUID());
        //设置系统自带头像
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        //把改用户实例插入到数据库中。
        userMapper.insertUser(user);

        //发送激活邮件
        Context context = new Context();
        //context.setVariable("email", user.getEmail());
        //http://localhost:8080/community/activation/101/code
         String url =  "/activation/" + user.getId() + "/" + user.getActivationCode();
         //context.setVariable("url", url);
        // String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "Activation", url);
        return map;

    }
    //返回用户激活的状态
    public int activation(int userId, String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1 );
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, long expiredSeconds){
        Map<String, Object> map = new HashMap<>();
        //处理空值
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        //验证账号
        User user = userMapper.selectByName(username);
        if(user == null){
            map.put("usernameMsg", "该账号不存在");
            return map;
        }
        //验证状态
        if(user.getStatus() == 0){
            map.put("usernameMsg", "该账号未激活");
            return map;
        }
        //验证密码
       password = CommunityUtil.md5(password + user.getSalt());
       if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码错误");
            return map;
        }
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //保存凭证到数据库，取而代之的是保存凭证到redis中
        //loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //redis会把loginTicket对象序列化为一个Json格式的字符串保存下来。
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    public void logout(String ticket){
        //loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket)redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket){
        //return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(redisKey);
    }
    //上传头像，清理缓存
    public int updateHeader(int userId, String headerUrl){

        int  rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    //修改密码
    public int updatePassword(int userId, String password){
        User user = userMapper.selectById(userId);
        int rows = userMapper.updatePassword(userId, CommunityUtil.md5(password+ user.getSalt()));
        clearCache(userId);
        return  rows;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //1.优先从缓存中取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2.如果从缓存中取不到值，就初始化缓存数据
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    //3.当数据变更时，清除缓存数据
    private void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    //查询某个用户的权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority(){

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });


        return list;
    }


}
