package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    //要替换敏感词的符号
    private static final String REPLACEMENT = "***";

    //初始化根节点,根节点不含字符，为null;
    private TrieNode root = new TrieNode();

    @PostConstruct  //这是一个初始化方法，当容器实例化这个bean，调用bean的构造器方法后，这个方法自动被调用
    public void init(){
        //获取类加载器，从类路径中读取sensetive-words文件
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensetive-words.txt");
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                ){
            String keyword;
            while((keyword = bufferedReader.readLine()) != null){
                //读到一行敏感词，添加到前缀树中去
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败：" + e.getMessage());
        }
    }
    //将一个敏感词添加到前缀树中去
    private void addKeyword(String keyword){
        TrieNode tempNode = root;
        //遍历单词中的字符
        for(int i=0 ; i< keyword.length() ; i++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if(subNode == null){
                //初始化子节点
                subNode = new TrieNode();
                //因为该子节点是新的，所以把该子节点挂到当前节点的下方
                tempNode.addSubNode(c, subNode);
            }

            //指针指向子节点，进入下一轮循环
            tempNode = subNode;
            //设置结束的标识
            if(i == keyword.length() - 1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    //返回过滤后的结果
    public String filter(String text){
        if(StringUtils.isBlank(text)) {
            return null;
        }
        //字符串不为空，那么需要过滤了。
        //指针1，指向树根
        TrieNode tempNode = root;
        //指针2, 指向text首位
        int begin = 0;
        //指针3，指向text首位
        int position = 0;
        //变量记录最终结果
        StringBuilder stringBuilder = new StringBuilder();

        while(begin < text.length()) {
            if (position < text.length()) {
                char c = text.charAt(position);

                //跳过符号
                if (isSymbol(c)) {
                    //若指针1指向root,将此符号计入结果。
                    if (tempNode == root) {
                        stringBuilder.append(c);
                        begin++;
                    }
                    position++;
                    continue;
                }
                //检查下级节点
                tempNode = tempNode.getSubNode(c);
                if (tempNode == null) { //找不到下级节点，说明以begin开头的字符没有敏感词
                    stringBuilder.append(text.charAt(begin));
                    //进入下一个位置
                    position = ++begin;
                    tempNode = root;
                } else if (tempNode.isKeywordEnd()) {
                    //发现了一个 以begin开头，以position结尾的敏感词
                    stringBuilder.append(REPLACEMENT);
                    //让指针进入下一个位置
                    begin = ++position;
                    //重新指向根节点
                    tempNode = root;
                } else {
                    //继续检查下一个字符
                        position++;
                }
            }else{
                //position越界了，但还未匹配到敏感词
                stringBuilder.append(text.charAt(begin));
                position = ++ begin;
                tempNode = root;
            }
        }
        return stringBuilder.toString();

    }
    //判断是否为符号的方法
    private boolean isSymbol(Character c){
        // 0x2E80~0x9FFF是东亚文字范围
        return CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF ); //abc,123返回的是true,如果是特殊字符返回false
    }

    //内部类：前缀树的节点
    private  class TrieNode{
        //关键词结束的标识
        private boolean isKeywordEnd = false;
        //节点的孩子(key是下级节点的字符， value是下级节点。
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
        //添加子节点的方法
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c, node);
        }
        //获取子节点的办法
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
}
