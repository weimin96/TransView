package com.wiblog.viewer.handler;

import java.io.InputStream;

/**
 * describe:
 *
 * @author panwm
 * @since 2024/6/28 14:48
 */
public interface ViewerHandler {

    void preview(InputStream inputStream, String type);
}
