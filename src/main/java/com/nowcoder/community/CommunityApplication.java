package com.nowcoder.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
@MapperScan({"com.nowcoder.community.dao"})
public class CommunityApplication {

	@PostConstruct
	public void init(){
		//解决netty启动冲突问题
		//Netty4Utils类中
		System.setProperty("es.set.netty.runtime.available.processors","false");
	}



	public static void main(String[] args) {

		SpringApplication.run(CommunityApplication.class, args);//创建Ioc容器，扫描bean，然后装配入容器中。
	}

}
