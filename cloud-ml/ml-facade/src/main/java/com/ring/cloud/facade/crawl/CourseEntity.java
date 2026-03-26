package com.ring.cloud.facade.crawl;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CourseEntity {
    private String round;
    private String vs;
    private String let;
}
