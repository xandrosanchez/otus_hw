package org.example.dataprocessor;

import java.util.List;
import org.example.model.Measurement;

public interface Loader {

    List<Measurement> load();
}
