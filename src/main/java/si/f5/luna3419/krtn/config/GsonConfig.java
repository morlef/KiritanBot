package si.f5.luna3419.krtn.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GsonConfig {
    private String token;
    private String missing;
    private String name;
    private String description;
    private long guild;
    private Map<String, String> messages;
    private Map<String, String> commands;
}
