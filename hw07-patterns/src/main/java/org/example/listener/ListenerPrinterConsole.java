package org.example.listener;

import org.example.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenerPrinterConsole implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(ListenerPrinterConsole.class);

    @Override
    public void onUpdated(Message msg) {
        logger.info("oldMsg:{}", msg);
    }
}
