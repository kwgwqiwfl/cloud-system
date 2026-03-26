package com.ring.cloud.facade.util;

import java.util.Random;

public class RandomUtil {
    public static Random random = new Random();
    //4000-8000
    public static int random48() {
        return random.nextInt(4000) + 4000;
    }
}
