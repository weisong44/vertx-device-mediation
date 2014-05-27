package com.weisong.test.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    private static final ObjectMapper mapper;
    
    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String toJsonStringRaw(Object obj)
            throws JsonGenerationException, JsonMappingException, IOException {
        Writer sw = new StringWriter();
        mapper.writerWithDefaultPrettyPrinter().writeValue(sw, obj);
        return sw.toString();
    }

    public static <T> T toObjectRaw(String json, Class<T> valueType)
            throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(json, valueType);
    }

    public static String toJsonString(Object obj) {
        try {
            return toJsonStringRaw(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T toObject(String json, Class<T> valueType) {
        try {
            return toObjectRaw(json, valueType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JsonUtil() {
        throw new AssertionError("JsonUtil should never be instantiated");
    }

}