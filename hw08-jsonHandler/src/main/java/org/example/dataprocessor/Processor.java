package org.example.dataprocessor;

import java.util.List;
import java.util.Map;
import org.example.model.Measurement;

public interface Processor {

    Map<String, Double> process(List<Measurement> data);
}
