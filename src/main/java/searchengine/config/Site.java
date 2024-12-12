package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConstructorBinding;

@Slf4j
@Setter
@Getter
public class Site {
    private String url;
    private String name;
}
