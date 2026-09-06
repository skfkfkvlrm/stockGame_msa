package com.skfkfkvlrm.stockservice.domain.stock;

import com.skfkfkvlrm.stockservice.domain.stock.StockDetailResponse;
import com.skfkfkvlrm.stockservice.domain.stock.Order;
import com.skfkfkvlrm.stockservice.domain.stock.StockDetailRepository;
import com.skfkfkvlrm.stockservice.domain.stock.StockDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.skfkfkvlrm.stockservice.domain.stock.StockListRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockDetailServiceImpl implements StockDetailService {
    private final StockDetailRepository stockDetailRepository;
    private final StockListRepository stockListRepository;

    // 1. 二쇱 湲곕낯 ?蹂?議고
    @Override
    public StockDetailResponse getStockDetailInfo(int stockId) {
        Map<String, Object> stockInfo = stockDetailRepository.getStockInfo(stockId);
        if (stockInfo == null) {
            throw new com.skfkfkvlrm.stockservice.exception.StockGameException(com.skfkfkvlrm.stockservice.exception.ErrorCode.STOCK_NOT_FOUND);
        }

        // 2. 二쇱 諛? ?蹂?議고
        Map<String, Object> stockPubInfo = stockDetailRepository.getStockPubInfo(stockId);

        int pubPrice = getIntOrDefault(stockPubInfo, "pubPrice");
        int pubAmount = getIntOrDefault(stockPubInfo, "pubAmount");
        // 3. ?„???œ??媛€寃?議고šŒ
        int nowPrice = stockDetailRepository.getStockPrice(stockId);
        nowPrice = nowPrice == 0 ? pubPrice : nowPrice;
        // 4. 이전 날 가격 및 유저 간 거래량(발행 잔량 제외) 조회
        int prevPrice = stockDetailRepository.getPervPrice(stockId);
        int tradeVolume = stockDetailRepository.getTradeVolume(stockId);

        String status = (String) stockInfo.get("status");
        if (status == null) status = "LISTED";

        // 5. response 빌드 후 반환
        return new StockDetailResponse(
                stockId,
                (String) stockInfo.get("name"),
                (String) stockInfo.get("content"),
                nowPrice,
                prevPrice,
                pubPrice,
                pubAmount,
                tradeVolume,
                status
        );
    }

    @Override
    public List<StockDetailResponse> getAllStocks() {
        List<Stock> stocks = stockListRepository.getAllStocks();
        if (stocks == null) return Collections.emptyList();
        return stocks.stream().map(s -> {
            int nowPrice = stockDetailRepository.getStockPrice(s.getStockId());
            nowPrice = nowPrice == 0 ? s.getPublicationPrice() : nowPrice;
            int tradeVol = stockDetailRepository.getTradeVolume(s.getStockId());
            return new StockDetailResponse(
                    s.getStockId(),
                    s.getName(),
                    s.getContent(),
                    nowPrice,
                    s.getPrevPrice(),
                    s.getPublicationPrice(),
                    s.getPublicationBalance(),
                    tradeVol,
                    s.getStatus() != null ? s.getStatus() : "LISTED"
            );
        }).collect(Collectors.toList());
    }

    private int getIntOrDefault(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return 0;
        }
        return ((Number) map.get(key)).intValue();
    }

    /**
     * 주식 거래 호가창 실시간 주문 목록 조회
     * 
     * @param stockId 주식 종목 ID
     * @param type    호가 주문 구분 ("매도" / "SELL" 또는 "매수" / "BUY")
     * @return 호가창에 표시할 대기 주문 목록
     */
    @Override
    public List<Order> getLiveOrderList(int stockId, String type) {
        if ("SELL".equalsIgnoreCase(type) || "매도".equalsIgnoreCase(type)) {
            List<Order> sellOrders = stockDetailRepository.getTotalSellOrder(stockId);
            return sellOrders != null ? sellOrders : Collections.emptyList();
        } else {
            List<Order> buyOrders = stockDetailRepository.getTotalBuyOrder(stockId);
            return buyOrders != null ? buyOrders : Collections.emptyList();
        }
    }

    @Override
    public List<Order> getwaitingOrderList(int stockId, String studentId) {
        List<Order> myOrders = stockDetailRepository.getTotalMyOrder(stockId, studentId);
        return myOrders != null ? myOrders : Collections.emptyList();
    }

    @Override
    public List<MarketIndexResponse> getMarketIndices() {
        List<StockDetailResponse> stocks = getAllStocks();
        if (stocks == null || stocks.isEmpty()) {
            return List.of(
                MarketIndexResponse.builder().name("KOSPI").value(2750.24).change(12.45).changeRate(0.45).build(),
                MarketIndexResponse.builder().name("KOSDAQ").value(845.12).change(-3.20).changeRate(-0.38).build()
            );
        }

        double totalNow = 0;
        double totalPrev = 0;
        for (StockDetailResponse s : stocks) {
            totalNow += s.getNowPrice();
            totalPrev += (s.getPrevPrice() > 0 ? s.getPrevPrice() : s.getNowPrice());
        }

        double kospiBase = 2750.0;
        double kosdaqBase = 845.0;

        double overallChangeRate = totalPrev > 0 ? ((totalNow - totalPrev) / totalPrev) : 0;
        double kospiPrev = 2737.79;
        double kosdaqPrev = 848.32;

        double kospiValue = Math.round((kospiBase * (1 + overallChangeRate)) * 100.0) / 100.0;
        double kospiChange = Math.round((kospiValue - kospiPrev) * 100.0) / 100.0;
        double kospiRate = Math.round(((kospiValue - kospiPrev) / kospiPrev * 100.0) * 100.0) / 100.0;

        double kosdaqValue = Math.round((kosdaqBase * (1 + (overallChangeRate * 0.8))) * 100.0) / 100.0;
        double kosdaqChange = Math.round((kosdaqValue - kosdaqPrev) * 100.0) / 100.0;
        double kosdaqRate = Math.round(((kosdaqValue - kosdaqPrev) / kosdaqPrev * 100.0) * 100.0) / 100.0;

        return List.of(
            MarketIndexResponse.builder()
                .name("KOSPI")
                .value(kospiValue)
                .change(kospiChange)
                .changeRate(kospiRate)
                .prevClose(kospiPrev)
                .openPrice(Math.round((kospiPrev + 2.3) * 100.0) / 100.0)
                .highPrice(Math.round((Math.max(kospiValue, kospiPrev) + 8.5) * 100.0) / 100.0)
                .lowPrice(Math.round((Math.min(kospiValue, kospiPrev) - 6.2) * 100.0) / 100.0)
                .high52w(2890.50)
                .low52w(2273.97)
                .volume(458290000L)
                .tradingValue(9820300000000L)
                .chartHistory(List.of(2720.5, 2735.2, 2741.0, 2738.4, 2745.8, 2737.79, kospiValue))
                .build(),
            MarketIndexResponse.builder()
                .name("KOSDAQ")
                .value(kosdaqValue)
                .change(kosdaqChange)
                .changeRate(kosdaqRate)
                .prevClose(kosdaqPrev)
                .openPrice(Math.round((kosdaqPrev - 1.1) * 100.0) / 100.0)
                .highPrice(Math.round((Math.max(kosdaqValue, kosdaqPrev) + 4.2) * 100.0) / 100.0)
                .lowPrice(Math.round((Math.min(kosdaqValue, kosdaqPrev) - 5.8) * 100.0) / 100.0)
                .high52w(920.10)
                .low52w(735.40)
                .volume(892400000L)
                .tradingValue(7450200000000L)
                .chartHistory(List.of(855.2, 852.0, 849.5, 847.2, 849.8, 848.32, kosdaqValue))
                .build()
        );
    }
}
