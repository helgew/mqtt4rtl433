package org.grajagan.util;

import lombok.Data;

@Data
public class TopicalMessage {
    String topic;
    String message;

    @Override
    public String toString() {
        return getTopic() + " -> " + getMessage();
    }
}
