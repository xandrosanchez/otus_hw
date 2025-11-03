package org.example.dataprocessor;

import java.util.*;
import org.example.model.Measurement;

public class ProcessorAggregator implements Processor {

    @Override
    public Map<String, Double> process(List<Measurement> data) {
        Map<String, Double> result = new HashMap<>();
        for (Measurement measurement : data) {
            String name = measurement.name();
            double value = measurement.value();

            result.merge(name, value, Double::sum);
        }

        List<Map.Entry<String, Double>> list = new ArrayList<>(result.entrySet());
        list.sort(Map.Entry.comparingByValue());
        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
