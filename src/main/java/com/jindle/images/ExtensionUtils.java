package com.jindle.images;

import com.ning.http.client.Request;
import com.ning.http.client.Response;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;

public class ExtensionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionUtils.class);

    public static String extractFileExtension(Request request, Response response){
        String contentType = getContentType(request, response);
        if (contentType != null){
            return getExtension(contentType);
        }
        return null;
    }

    private static String getExtension(String contentType){
        try {
            MimeType mime = MimeTypes.getDefaultMimeTypes().forName(contentType);
            return mime.getExtension();
        } catch (MimeTypeException e) {
            LOG.error("Error getting extension for mime " + contentType, e);
        }
        return null;
    }

    private static String getContentType(Request request, Response response){
        String contentType = response.getContentType();
        if (contentType == null){
            contentType = URLConnection.guessContentTypeFromName(request.getUrl());
            if (contentType == null){
                try {
                    byte[] responseBytes = response.getResponseBodyAsBytes();
                    contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(responseBytes));
                } catch (IOException e) {
                    LOG.warn("Error tyring to guest content type from stream", e);
                }
            }
        }
        return contentType;
    }
}
