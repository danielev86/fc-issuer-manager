package com.redcatdev86.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redcatdev86.model.CareerSave;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonDataStore {

    private final ObjectMapper mapper;

    public JsonDataStore() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<CareerSave> load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return new ArrayList<>();
        }
        return mapper.readValue(
                Files.readString(file),
                mapper.getTypeFactory().constructCollectionType(List.class, CareerSave.class)
        );
    }

    public void save(Path file, List<CareerSave> saves) throws IOException {
        if (file == null) throw new IllegalArgumentException("file is null");
        if (saves == null) saves = new ArrayList<>();

        Files.createDirectories(file.getParent());
        String json = mapper.writeValueAsString(saves);
        Files.writeString(file, json);
    }
}