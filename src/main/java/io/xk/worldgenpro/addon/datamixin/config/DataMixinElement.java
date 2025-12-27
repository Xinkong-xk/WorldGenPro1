package io.xk.worldgenpro.addon.datamixin.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.xk.worldgenpro.addon.datamixin.api.DataMixinOp;

public class DataMixinElement {

    public static final int DEFAULT_PRIORITY = 1000;

    protected final DataMixinOp op;
    protected final int priority;
    protected final String opText;
    public JsonObject attributes;

    public DataMixinFile parentFile;
    public int elementIndex;

    // 预处理部分
    public JsonElement target = null;
    public JsonElement value = null;

    public DataMixinElement(String opText, int priority, JsonObject attributes) {
        this.opText = opText;
        this.priority = priority;
        this.op = DataMixinOp.fromString(opText);
        this.attributes = attributes;
        // 自动进行预处理构建
        this.build();
    }

    public void build() {
        // 预处理构建
        if (this.attributes.has("target")) {
            this.target = this.attributes.get("target");
        }
        if (this.attributes.has("value")) {
            this.value = this.attributes.get("value");
        }
    }

    /**
     * 必须设置父文件才能保证错误提示能够正常溯源
     * @param parentFile 产生此 DataMixin 元素的配置文件
     */
    public void setParentFile(DataMixinFile parentFile) {
        this.parentFile = parentFile;
    }

    public void setElementIndex(int elementIndex) {
        this.elementIndex = elementIndex;
    }

    public String getOpText() {return opText;}

    public DataMixinOp getOp() {
        return op;
    }

    public int getPriority() {
        return priority;
    }

    public JsonObject getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "DataMixinElement{" +
                "op=" + op +
                ", priority=" + priority +
                ", attributes=" + attributes +
                '}';
    }
}
