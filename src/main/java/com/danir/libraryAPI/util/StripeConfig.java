package com.danir.libraryAPI.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stripe")
@Data
public class StripeConfig {
    private String publishableKey;
    private String secretKey;
    private String currency;
}
