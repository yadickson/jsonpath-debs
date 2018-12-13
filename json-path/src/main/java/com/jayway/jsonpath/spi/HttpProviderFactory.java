package com.jayway.jsonpath.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: kallestenflo
 * Date: 3/10/12
 * Time: 7:39 AM
 */
public abstract class HttpProviderFactory {

    public static HttpProviderFactory factory = new HttpProviderFactory() {
        @Override
        protected HttpProvider create() {
            return new HttpProvider() {
                @Override
                public InputStream get(URL url) throws IOException {
                    URLConnection connection = url.openConnection();
                    connection.setRequestProperty("Accept", "application/json");
                    return connection.getInputStream();
                }
            };
        }
    };

    public static HttpProvider getProvider(){
        return factory.create();
    }


    protected abstract HttpProvider create();
}
