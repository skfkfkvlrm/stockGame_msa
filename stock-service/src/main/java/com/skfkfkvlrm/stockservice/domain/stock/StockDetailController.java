package com.skfkfkvlrm.stockservice.domain.stock;

import com.skfkfkvlrm.stockservice.domain.common.ApiResponse;
import com.skfkfkvlrm.stockservice.domain.stock.StockDetailResponse;
import com.skfkfkvlrm.stockservice.domain.stock.StockPriceHistory;
import com.skfkfkvlrm.stockservice.domain.stock.StockPriceHistoryRepository;
import com.skfkfkvlrm.stockservice.domain.stock.StockDetailService;
import com.skfkfkvlrm.stockservice.domain.stock.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/stock", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class StockDetailController {
    private final StockDetailService stockDetailService;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockListRepository stockListRepository;
    private final com.skfkfkvlrm.stockservice.domain.admin.MarketSettingsRepository marketSettingsRepository;

    @GetMapping("")
    public ApiResponse<List<StockDetailResponse>> getStockList() {
        List<StockDetailResponse> list = stockDetailService.getAllStocks();
        return ApiResponse.success("Stock list", list);
    }

    @GetMapping("/market-index")
    public ApiResponse<List<MarketIndexResponse>> getMarketIndices() {
        List<MarketIndexResponse> indices = stockDetailService.getMarketIndices();
        return ApiResponse.success("Market indices", indices);
    }

    @GetMapping("/{stockId}")
    public ApiResponse<StockDetailResponse> getStockDetail(
            @PathVariable("stockId") int stockId,
            @RequestAttribute(name = "studentId", required = false) String studentId) {
        
        StockDetailResponse response = stockDetailService.getStockDetailInfo(stockId);
        return ApiResponse.success("Stock details", response);
    }

    @GetMapping("/{stockId}/history")
    public ApiResponse<List<StockPriceHistory>> getStockHistory(
            @PathVariable("stockId") int stockId) {
        List<StockPriceHistory> history = stockPriceHistoryRepository.findHistoryByStockId(stockId);
        return ApiResponse.success("Stock price history", history);
    }

    @GetMapping("/{stockId}/orderbook")
    public ApiResponse<Map<String, List<Order>>> getOrderbook(
            @PathVariable("stockId") int stockId) {
        List<Order> sellOrders = stockDetailService.getLiveOrderList(stockId, "매도");
        List<Order> buyOrders = stockDetailService.getLiveOrderList(stockId, "매수");
        
        Map<String, List<Order>> orderbook = new HashMap<>();
        orderbook.put("sell", sellOrders);
        orderbook.put("buy", buyOrders);
        
        return ApiResponse.success("Orderbook", orderbook);
    }

    @GetMapping("/{stockId}/orders/my")
    public ApiResponse<List<Order>> getMyOrders(
            @PathVariable("stockId") int stockId,
            @RequestAttribute(name = "studentId", required = false) String studentId) {
        if (studentId == null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            studentId = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }
        if (studentId == null || "anonymousUser".equals(studentId)) {
            return ApiResponse.error("로그인이 필요합니다.");
        }
        List<Order> myOrders = stockDetailService.getwaitingOrderList(stockId, studentId);
        return ApiResponse.success("My Orders", myOrders);
    }

    @PostMapping("/admin/stocks")
    public ApiResponse<Boolean> createStockAdmin(@RequestBody com.skfkfkvlrm.stockservice.domain.admin.StockRequest request) {
        stockListRepository.insertStock(request);
        return ApiResponse.success("신규 주식 종목이 상장되었습니다.", true);
    }

    @PutMapping("/admin/stocks/{stockId}")
    public ApiResponse<Boolean> updateStockAdmin(@PathVariable("stockId") int stockId, @RequestBody com.skfkfkvlrm.stockservice.domain.admin.StockRequest request) {
        stockListRepository.updateStock(stockId, request);
        return ApiResponse.success("주식 종목 정보가 수정되었습니다.", true);
    }

    @DeleteMapping("/admin/stocks/{stockId}")
    public ApiResponse<Boolean> deleteStockAdmin(@PathVariable("stockId") int stockId) {
        stockListRepository.deleteStock(stockId);
        return ApiResponse.success("주식 종목이 상장폐지되었습니다.", true);
    }

    @GetMapping("/admin/market/status")
    public ApiResponse<Map<String, Object>> getMarketStatus() {
        com.skfkfkvlrm.stockservice.domain.admin.MarketSettings settings = marketSettingsRepository.findById(1).orElse(null);
        Map<String, Object> data = new HashMap<>();
        data.put("marketOpen", settings == null || settings.isMarketOpen());
        return ApiResponse.success("Market status", data);
    }

    @PostMapping("/admin/market/toggle")
    public ApiResponse<Map<String, Object>> toggleMarketStatus() {
        com.skfkfkvlrm.stockservice.domain.admin.MarketSettings settings = marketSettingsRepository.findById(1).orElse(null);
        if (settings == null) {
            settings = com.skfkfkvlrm.stockservice.domain.admin.MarketSettings.builder()
                    .id(1)
                    .isMarketOpen(false)
                    .build();
        } else {
            settings.setMarketOpen(!settings.isMarketOpen());
        }
        marketSettingsRepository.save(settings);

        Map<String, Object> data = new HashMap<>();
        data.put("marketOpen", settings.isMarketOpen());
        return ApiResponse.success("Market status toggled", data);
    }
}
