/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.release.maven;

import hudson.maven.ModuleName;
import org.jdom.input.SAXBuilder;
import org.jfrog.build.extractor.maven.transformer.SnapshotNotAllowedException;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests the pom version change transformations.
 *
 * @author Yossi Shaul
 */
public class PomTransformerTest {

    @Test
    public void transformSimplePom() throws Exception {
        File pomFile = getResourceAsFile("/poms/parentonly/pom.xml");
        HashMap<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "parent"), "2.2");

        new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, "", false).invoke(pomFile, null);

        String pomStr = new String(Files.readAllBytes(pomFile.toPath()), Charset.defaultCharset());
        String expectedStr = new String(Files.readAllBytes(getResourceAsFile("/poms/parentonly/pom.expected.xml").toPath()),
                Charset.defaultCharset());

        assertEquals(expectedStr, pomStr);
    }

    @Test
    public void transformMultiPom() throws Exception {
        File pomFile = getResourceAsFile("/poms/multi/pom.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test.nested", "nested1"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "nested2"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "two"), "3.6");

        new PomTransformer(new ModuleName("org.jfrog.test.nested", "two"), modules, "", false).invoke(pomFile, null);

        String pomStr = new String(Files.readAllBytes(pomFile.toPath()), Charset.defaultCharset());
        ;
        String expectedStr = new String(Files.readAllBytes(getResourceAsFile("/poms/multi/pom.expected.xml").toPath()),
                Charset.defaultCharset());

        assertEquals(expectedStr, pomStr);
    }

    @Test
    public void transformScm() throws Exception {
        File pomFile = getResourceAsFile("/poms/scm/pom.xml");
        HashMap<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "parent"), "1");

        new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules,
                "http://subversion.jfrog.org/test/tags/1", false).invoke(pomFile, null);

        String pomStr = new String(Files.readAllBytes(pomFile.toPath()), Charset.defaultCharset());
        String expectedStr = new String(Files.readAllBytes(getResourceAsFile("/poms/scm/pom.expected.xml").toPath()), Charset.defaultCharset());

        assertEquals(expectedStr, pomStr);
    }

    @Test
    public void snapshotsModule() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshot.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "one"), "2.2-SNAPSHOT");
        try {
            new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, "", true).invoke(pomFile, null);
            fail("Pom contains module with snapshot version and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue("Unexpected error message: " + message, message.contains("org.jfrog.test:one:2.2-SNAPSHOT"));
        }
    }

    @Test
    public void snapshotsInParent() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshot-parent.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        try {
            new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, "", true).invoke(pomFile, null);
            fail("Pom contains snapshot in the parent and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue("Unexpected error message: " + message,
                    message.contains("org.jfrog.test:parent:2.1-SNAPSHOT"));
        }
    }

    @Test
    public void snapshotsInDependenciesManagement() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshots-in-dep-management.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test.nested", "nested1"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "nested2"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "four"), "3.6");

        try {
            new PomTransformer(new ModuleName("org.jfrog.test.nested", "four"), modules, "", true).invoke(pomFile,
                    null);
            fail("Pom contains snapshot in the dependency management and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue("Unexpected error message: " + message,
                    message.contains("org.jfrog.test.nested:nestedX:2.0-SNAPSHOT"));
        }
    }

    @Test
    public void snapshotsInDependencies() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshots-in-dependencies.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test.nested", "nested1"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "nested2"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "four"), "3.6");

        try {
            new PomTransformer(new ModuleName("org.jfrog.test.nested", "four"), modules, "", true).invoke(pomFile,
                    null);
            fail("Pom contains snapshot in the dependencies and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue("Unexpected error message: " + message,
                    message.contains("org.jfrog.test.nested:nestedX:3.2-SNAPSHOT"));
        }
    }

    private File getResourceAsFile(String path) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }


    private SAXBuilder createSaxBuilder() {
        SAXBuilder sb = new SAXBuilder();
        // don't validate and don't load dtd
        sb.setValidation(false);
        sb.setFeature("http://xml.org/sax/features/validation", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return sb;
    }
}
