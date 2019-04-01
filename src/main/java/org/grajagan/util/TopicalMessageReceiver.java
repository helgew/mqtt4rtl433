package org.grajagan.util;

public interface TopicalMessageReceiver {
    void consume(TopicalMessage message);
}
