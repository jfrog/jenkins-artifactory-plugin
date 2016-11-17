package org.jfrog.hudson.pipeline.docker.proxy;

import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.pipeline.docker.utils.DockerAgentUtils;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class responsible for capturing manifest data between docker daemon to Artifactory docker registry.
 *
 * Created by romang on 7/10/16.
 */
public class BuildInfoFilterAdapter extends HttpFiltersAdapter {

    private static ConcurrentMap<Integer, String> partialManifestString = new ConcurrentHashMap<Integer, String>();

    public BuildInfoFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }

    public BuildInfoFilterAdapter(HttpRequest originalRequest) {
        super(originalRequest);
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        if (httpObject instanceof ByteBufHolder && originalRequest.getMethod() == HttpMethod.PUT
                && originalRequest.getUri().contains("manifest")
                && StringUtils.contains(originalRequest.headers().get("Content-Type"), "manifest.v2")) {

            int origRequestContentLength = Integer.parseInt(originalRequest.headers().get("Content-Length"));
            String contentStr = ((ByteBufHolder) httpObject).content().toString(CharsetUtil.UTF_8);
            if (contentStr.length() >= origRequestContentLength) {
                Properties properties = new Properties();
                properties.put("User-Agent", originalRequest.headers().get("User-Agent"));
                DockerAgentUtils.captureContent(contentStr, properties);
                return null;
            }

            if (!partialManifestString.containsKey(ctx.hashCode())) {
                partialManifestString.put(ctx.hashCode(), contentStr);
                return null;
            }

            String aggregatedString = partialManifestString.get(ctx.hashCode());
            aggregatedString += contentStr;
            if (aggregatedString.length() >= origRequestContentLength) {
                Properties properties = new Properties();
                properties.put("User-Agent", originalRequest.headers().get("User-Agent"));
                DockerAgentUtils.captureContent(aggregatedString, properties);
                partialManifestString.remove(ctx.hashCode());
                return null;
            }

            partialManifestString.put(ctx.hashCode(), aggregatedString);
        }

        return null;
    }
}
