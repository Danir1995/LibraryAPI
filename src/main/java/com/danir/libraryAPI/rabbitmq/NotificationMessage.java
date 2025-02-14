package com.danir.libraryAPI.rabbitmq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage implements Serializable {
    private String toEmail;
    private String subject;
    private String message;
}
