package io.xk.worldgenpro.addon.datamixin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

// DataMixinFile for Gson parsing
// elements are arbitrary JsonElement

public class DataMixinFile{

    // 文件名
    public String fileName;

    @SerializedName("mixin_type")
    public String mixinType;

    @SerializedName("mixin_target")
    public String mixinTarget;

    public List<JsonElement> elements;

    public DataMixinFile( String mixinType, String mixinTarget, List<JsonElement> elements) {
        this.mixinType = mixinType;
        this.mixinTarget = mixinTarget;
        this.elements = elements;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String mixinType() {
        return mixinType;
    }

    public String getFileName() {
        return fileName;
    }

    public List<JsonElement> elements() {
        return elements;
    }

    public String mixinTarget() {
        return mixinTarget;
    }

    // 读取方法: 从InputStream读取
    public static DataMixinFile read(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, DataMixinFile.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read DataMixinFile", e);
        }
    }

    // 读取方法: 从BufferedReader读取
    public static DataMixinFile read(BufferedReader reader) {
        try {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, DataMixinFile.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read DataMixinFile", e);
        }
    }

    @Override
    public String toString() {
        return "DataMixinFile{" +
                "mixinType='" + mixinType + '\'' +
                ", mixinTarget=" + mixinTarget +
                ", elements=" + elements +
                '}';
    }
}
