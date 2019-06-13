package org.jfrog.hudson.pipeline.integration;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;


/**
 * @author Alexei Vainshtein
 */

class MockServer {

    private static ClientAndServer mockServer;

    static void start(String jobName, String pipelineType) {
        mockServer = ClientAndServer.startClientAndServer(9999);
        mockServer.when(HttpRequest.request().withPath("/api/xray/scanBuild")).respond(HttpResponse.response().withStatusCode(200).withBody("{\n" +
                "  \"summary\" : {\n" +
                "    \"message\" : \"Build " + pipelineType + ":" + jobName + " test number 3 was scanned by Xray and 1 Alerts were generated\",\n" +
                "    \"total_alerts\" : 1,\n" +
                "    \"fail_build\" : true,\n" +
                "    \"more_details_url\" : \"https://ecosysjfrog-xray.jfrog.io/web/#/component/details/build:~2F~2Fdocker%2F11\"\n" +
                "  }\n" +
                "}"));
    }

    static void stop() {
        mockServer.stop();
    }
}
