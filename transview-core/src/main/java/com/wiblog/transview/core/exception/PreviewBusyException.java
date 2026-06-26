package com.wiblog.transview.core.exception;

/**
 * 预览服务繁忙异常（线程池拒绝）
 */
public class PreviewBusyException extends RuntimeException {

    public PreviewBusyException(String message) {
        super(message);
    }
}
