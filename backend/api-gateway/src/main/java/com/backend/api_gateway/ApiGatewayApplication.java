package com.backend.api_gateway;

import com.backend.common.config.filter.GatewayHeadersFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = {"com.backend.api_gateway", "com.backend.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX, // ✅ Change to REGEX
                // ✅ Change 'classes' to 'pattern' and use the fully qualified class name as a string
                // Remember to escape the dots with \\
                pattern = "com\\.backend\\.common\\.config\\.filter\\.GatewayHeadersFilter"
        )
)
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
