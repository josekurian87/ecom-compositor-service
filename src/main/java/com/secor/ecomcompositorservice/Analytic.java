package com.secor.ecomcompositorservice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class Analytic {

    private String objectid;
    private String type;
    private String principal;
    private LocalTime timestamp;
    private String description;

}