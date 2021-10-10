package org.jfrog.hudson.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.filespecs.FileSpec;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by romang on 4/24/16.
 */
public class ArtifactoryDownloadUploadJsonTest {

    @Test
    public void testReadJson() throws IOException {
        InputStream stream = ArtifactoryDownloadUploadJsonTest.class.getClassLoader().getResourceAsStream("jsons/download.json");
        String jsonStr = IOUtils.toString(stream);
        ObjectMapper mapper = new ObjectMapper();
        FileSpec downloadJson = mapper.readValue(jsonStr, FileSpec.class);

        assertEquals("File pattern is incorrect", "my-repo/resolved.my", downloadJson.getFiles().get(0).getPattern());

        String expectedAql = "{\"repo\":\"my-repo\",\"$or\":[{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"*.zip\"}}]}]}";
        assertEquals("Aql is incorrect", expectedAql, downloadJson.getFiles().get(1).getAql());

        assertEquals("File target is incorrect", "my-repo/by-pattern/", downloadJson.getFiles().get(0).getTarget());

        assertNull(downloadJson.getFiles().get(0).getAql());
        assertNull(downloadJson.getFiles().get(1).getPattern());
    }

}