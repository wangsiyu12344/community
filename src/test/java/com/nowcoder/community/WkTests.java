package com.nowcoder.community;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd = "d:\\develop\\wkhtmltopdf\\bin\\wkhtmltoimage.exe https://www.nowcoder.com d:/develop/wkhtmlimage/3.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
