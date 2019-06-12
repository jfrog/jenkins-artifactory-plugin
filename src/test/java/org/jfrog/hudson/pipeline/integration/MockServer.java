package org.jfrog.hudson.pipeline.integration;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;


/**
 * @author Alexei Vainshtein
 */

public class MockServer {

    private static ClientAndServer mockServer;

    public static void start(String jobName) {
        mockServer = ClientAndServer.startClientAndServer(9999);
        mockServer.when(HttpRequest.request().withPath("/api/xray/scanBuild")).respond(HttpResponse.response().withStatusCode(200).withBody("{\n" +
                "  \"summary\" : {\n" +
                "    \"message\" : \"Build declarative:" + jobName + " test number 3 was scanned by Xray and 1 Alerts were generated\",\n" +
                "    \"total_alerts\" : 1,\n" +
                "    \"fail_build\" : true,\n" +
                "    \"more_details_url\" : \"https://ecosysjfrog-xray.jfrog.io/web/#/component/details/build:~2F~2Fdocker%2F11\"\n" +
                "  }\n" +
                "}"));
    }

    public static void stop() {
        mockServer.stop();
    }
}
