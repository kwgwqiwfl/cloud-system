package com.ring.cloud.facade.common;

public enum TeamEnum {
    yingchao("英超", "https://liansai.500.com/zuqiu-6556/", "https://liansai.500.com/zuqiu-6556/jifen-18819/"),
    xijia("西甲", "https://liansai.500.com/zuqiu-6556/", "https://liansai.500.com/zuqiu-6556/jifen-18819/"),
    yijia("意甲", "https://liansai.500.com/zuqiu-6556/", "https://liansai.500.com/zuqiu-6556/jifen-18819/"),
    dejia("德甲", "https://liansai.500.com/zuqiu-6556/", "https://liansai.500.com/zuqiu-6556/jifen-18819/"),
    other("其他", "", "");

    private final String name;
    private final String team;
    private final String url;

    TeamEnum(String name, String team, String url){
        this.name = name;
        this.team = team;
        this.url = url;
    }

    public static TeamEnum byName(String name) {
        for (TeamEnum teamEnum : TeamEnum.values()) {
            if (teamEnum.name.equalsIgnoreCase(name)) {
                return teamEnum;
            }
        }
        return other;
    }

}
