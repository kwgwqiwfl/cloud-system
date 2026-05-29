package com.ring.cloud.gateway.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RouteDefinitionDTO {
    private String id;
    private String uri;
    private Integer order;
    private java.util.List<PredicateDTO> predicates;
    private java.util.List<FilterDTO> filters;

    @Data
    public static class PredicateDTO {
        private String name;
        private Map<String, String> args; // 关键：是 Map，不是 List！
    }

    @Data
    public static class FilterDTO {
        private String name;
        private Map<String, String> args;
    }
}