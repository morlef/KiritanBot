package si.f5.luna3419.krtn.gson;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GenericHashMapDeserializer<T> implements JsonDeserializer<Map<String, T>> {
    @Override
    @SuppressWarnings("unchecked")
    public HashMap<String, T> deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        if (!jsonElement.isJsonObject()) {
            return null;
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> jsonEntrySet = jsonObject.entrySet();
        HashMap<String, T> deserializedMap = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonEntrySet) {
            try {
                if (entry.getValue().isJsonNull()) {
                    deserializedMap.put(entry.getKey(), null);
                } else if (entry.getValue().isJsonArray()) {
                    deserializedMap.put(entry.getKey(), (T) entry.getValue());
                } else if (entry.getValue().isJsonObject()) {
                    deserializedMap.put(entry.getKey(), (T) entry.getValue());
                } else if (entry.getValue().isJsonPrimitive()) {
                    deserializedMap.put(entry.getKey(), context.deserialize(entry.getValue(), String.class));
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return deserializedMap;
    }
}
