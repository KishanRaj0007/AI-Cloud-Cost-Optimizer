package com.costoptimizer.forecasting_service.service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.costoptimizer.forecasting_service.model.CostData;
import com.costoptimizer.forecasting_service.repository.CostDataRepository;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

@Service
public class ForecastingService {
    @Autowired
    private CostDataRepository repository;

    public List<Double> createForecast(int daysToForecast) {
        List<CostData> allCosts = repository.findAll();
        
        if (allCosts.size() < 10) {
            throw new IllegalStateException("Not enough historical data to create a forecast. At least 10 data points are needed.");
        }

        Map<java.time.LocalDate, Double> dailyCostsMap = allCosts.stream()
                .collect(Collectors.groupingBy(
                        cost -> cost.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.summingDouble(CostData::getCost)
                ));

        Map<java.time.LocalDate, Double> sortedDailyCosts = new TreeMap<>(dailyCostsMap);
        double[] dailyTotals = sortedDailyCosts.values().stream().mapToDouble(Double::doubleValue).toArray();
        TimeSeries series = TimeSeries.from(dailyTotals);
        ArimaOrder order = ArimaOrder.order(1, 1, 1);
        Arima model = Arima.model(series, order);
        Forecast forecast = model.forecast(daysToForecast);
        
        return forecast.pointEstimates().asList();
    }
}
