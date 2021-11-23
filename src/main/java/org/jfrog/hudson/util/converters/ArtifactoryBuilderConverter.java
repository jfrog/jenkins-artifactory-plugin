package org.jfrog.hudson.util.converters;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.util.XStream2;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.JFrogPlatformInstance;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ArtifactoryBuilderConverter extends XStream2.PassthruConverter<ArtifactoryBuilder.DescriptorImpl> {
    Logger logger = Logger.getLogger(ArtifactoryBuilderConverter.class.getName());
    List<String> converterErrors = new ArrayList<>();

    public ArtifactoryBuilderConverter(XStream2 xstream) {
        super(xstream);
    }

    @Override
    protected void callback(ArtifactoryBuilder.DescriptorImpl artifactoryBuilder, UnmarshallingContext unmarshallingContext) {
        jfrogPlatformMigration(artifactoryBuilder);
        if (!converterErrors.isEmpty()) {
            logger.info(converterErrors.toString());
        }
    }

    private void jfrogPlatformMigration(ArtifactoryBuilder.DescriptorImpl artifactoryBuilder) {
        Class<? extends ArtifactoryBuilder.DescriptorImpl> overriderClass = artifactoryBuilder.getClass();
        Field artifactoryServersField = null;
        try {
            artifactoryServersField = overriderClass.getDeclaredField("artifactoryServers");
            Field jfrogInstancesField = overriderClass.getDeclaredField("jfrogInstances");
            artifactoryServersField.setAccessible(true);
            jfrogInstancesField.setAccessible(true);

            Object artifactoryServersObj = artifactoryServersField.get(artifactoryBuilder);
            Object jfrogServersObj = jfrogInstancesField.get(artifactoryBuilder);

            if (artifactoryServersObj != null && jfrogServersObj == null) {
                List<ArtifactoryServer> artifactoryServers = (List<ArtifactoryServer>) artifactoryServersObj;
                // Must not be null.
                // Once the artifactory builder will get save in the new form(jfrog instances), it is no longer needed to do a conversion.
                // In order to identify if a conversion has already made for the first time, we validate if 'jfrogInstances' is found.
                List<JFrogPlatformInstance> jfrogInstances = new ArrayList<>();
                for (ArtifactoryServer artifactoryServer : artifactoryServers) {
                    jfrogInstances.add(new JFrogPlatformInstance(artifactoryServer));
                }
                jfrogInstancesField.set(artifactoryBuilder, jfrogInstances);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            converterErrors.add(getConversionErrorMessage(artifactoryBuilder, e));
        }
    }

    private String getConversionErrorMessage(ArtifactoryBuilder.DescriptorImpl artifactoryBuilder, Exception e) {
        return String.format("Could not convert the class '%s' to use the new overriding" +
                "format. Cause: %s", artifactoryBuilder.getClass().getName(), e.getCause());
    }
}
