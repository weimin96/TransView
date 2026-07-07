package com.wiblog.transview.core.exception;

/**
 * 预览超时异常
 *
 * @author pwm
 */
public class PreviewTimeoutException extends RuntimeException {

    public PreviewTimeoutException(String message) {
        super(message);
    }
}
