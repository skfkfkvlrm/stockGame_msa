package com.skfkfkvlrm.stockservice.domain.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketIndexResponse {
    private String name;
    private double value;
    private double change;
    private double changeRate;
    private double prevClose;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double high52w;
    private double low52w;
    private long volume;
    private long tradingValue;
    private List<Double> chartHistory;
}
