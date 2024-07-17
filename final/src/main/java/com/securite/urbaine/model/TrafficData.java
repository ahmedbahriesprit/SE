package com.securite.urbaine.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrafficData {
    private String city;
    private int zone;
    private int time;
    private int day;
    private int severity;
}
