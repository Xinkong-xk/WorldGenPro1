package io.xk.worldgenpro.addon.datamixin;

import com.google.gson.JsonElement;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Pair;

import java.util.HashMap;

public class DataMixinValidate {

    /**
     * 检验 DataMixin 项是否冲突,并且返回冲突的错误提示
     * @param mixinElements 此优先级下的所有 DataMixin 项
     * @return Pair<检验结果(true则表示通过检验),错误提示>
     */
    public static Pair<Boolean, String> validateMixins(ObjectArrayList<DataMixinElement> mixinElements) {

        // key = target(JsonElement), value = 第一个占用该 target 的 element
        HashMap<JsonElement, DataMixinElement> targetMap = new HashMap<>();

        for (DataMixinElement element : mixinElements) {

            // 1. 没有 target 的操作不可能产生冲突（如 add）
            if (element.target == null) {
                continue;
            }

            // 2. 检查是否已有相同 target
            DataMixinElement existing = targetMap.get(element.target);
            if (existing != null) {

                // 构造错误信息（你已经有 parentFile + index，非常好）
                String message =
                        "DataMixin conflict detected at priority (" + element.getPriority() + ")\n" +
                                "Target: " + element.target + "\n" +
                                "First:  " + describe(existing) + "\n" +
                                "Second: " + describe(element) + "\n";

                return new Pair<>(false, message);
            }

            targetMap.put(element.target, element);
        }

        // 检验成功,没有发现冲突
        return new Pair<>(true, "");
    }

    private static String describe(DataMixinElement e) {
        return String.format(
                "\n   -op=%s\n   -file=%s\n   -index=%d\n   -%s",
                e.getOp(),
                e.parentFile != null ? e.parentFile.fileName : "<unknown>",
                e.elementIndex,
                e.getAttributes()
        );
    }

}
