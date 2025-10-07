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
        // --- Step 1: Fetch Raw Data ---
        // Get all the historical cost data from our MongoDB database.
        List<CostData> allCosts = repository.findAll();
        
        if (allCosts.size() < 10) { // Need a minimum amount of data to forecast
            throw new IllegalStateException("Not enough historical data to create a forecast. At least 10 data points are needed.");
        }

        // --- Step 2: Process & Aggregate Data ---
        // Our raw data is a list of individual costs. The ARIMA model needs a clean,
        // regularly spaced series, like one data point (total cost) per day.
        // We will group all costs by day and sum them up.
        Map<java.time.LocalDate, Double> dailyCostsMap = allCosts.stream()
                .collect(Collectors.groupingBy(
                        cost -> cost.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.summingDouble(CostData::getCost)
                ));

        // Sort the map by date to ensure the time series is in the correct order.
        Map<java.time.LocalDate, Double> sortedDailyCosts = new TreeMap<>(dailyCostsMap);

        // --- Step 3: Prepare Data for the Model ---
        // The library needs the data as a simple array of numbers (doubles).
        double[] dailyTotals = sortedDailyCosts.values().stream().mapToDouble(Double::doubleValue).toArray();
        TimeSeries series = TimeSeries.from(dailyTotals);

        // --- Step 4: Train the ARIMA Model ---
        // We define the "order" of the ARIMA model. These (p, d, q) parameters are technical
        // settings. (1, 1, 1) is a common starting point for many time series.
        ArimaOrder order = ArimaOrder.order(1, 1, 1);
        Arima model = Arima.model(series, order);

        // --- Step 5: Generate and Return the Forecast ---
        // We use our trained model to predict the future values.
        Forecast forecast = model.forecast(daysToForecast);
        
        // We'll return the main prediction points as a simple list of numbers.
        return forecast.pointEstimates().asList();
    }
}
