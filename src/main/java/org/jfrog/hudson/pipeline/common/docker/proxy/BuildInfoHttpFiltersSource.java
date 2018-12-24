package org.jfrog.hudson.pipeline.common.docker.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

/**
 * Created by romang on 7/13/16.
 */
public class BuildInfoHttpFiltersSource extends HttpFiltersSourceAdapter {

    private static final AttributeKey<String> CONNECTED_URL = AttributeKey.valueOf("connected_url");

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {

        String uri = originalRequest.uri();
        if (originalRequest.method() == HttpMethod.CONNECT) {
            if (ctx != null) {
                String prefix = "https://" + uri.replaceFirst(":443$", "");
                ctx.channel().attr(CONNECTED_URL).set(prefix);
            }
            return new BuildInfoFilterAdapter(originalRequest, ctx);
        }
        String connectedUrl = ctx.channel().attr(CONNECTED_URL).get();
        if (connectedUrl == null) {
            return new BuildInfoFilterAdapter(originalRequest, ctx);
        }

        return new BuildInfoFilterAdapter(originalRequest, ctx);
    }
}
