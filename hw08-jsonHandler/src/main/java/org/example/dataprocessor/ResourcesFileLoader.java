package org.example.dataprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import org.example.model.Measurement;

public class ResourcesFileLoader implements Loader {

    private final String fileName;
    private final ObjectMapper objectMapper;

    public ResourcesFileLoader(String fileName) {
        this.fileName = fileName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Measurement> load() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

            if (inputStream == null) {
                throw new FileNotFoundException("Файл не найден в ресурсах: " + fileName);
            }

            List<Measurement> measurements = objectMapper.readValue(
                    inputStream, TypeFactory.defaultInstance().constructCollectionType(List.class, Measurement.class));

            inputStream.close();
            return measurements;
        } catch (Exception e) {
            throw new FileProcessException(e);
        }
    }
}
