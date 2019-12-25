package org.jfrog.hudson.util.converters;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.util.XStream2;

import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.util.Credentials;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

public class CredentialsConfigConverter extends XStream2.PassthruConverter<CredentialsConfig> {
    Logger logger = Logger.getLogger(CredentialsConfigConverter.class.getName());
    List<String> converterErrors = Lists.newArrayList();

    public CredentialsConfigConverter(XStream2 xstream) {
        super(xstream);
    }

    @Override
    protected void callback(CredentialsConfig credentialsConfig, UnmarshallingContext context) {
        if (credentialsConfig == null)
            return;

        credentialsConversion(credentialsConfig);

        if (!converterErrors.isEmpty()) {
            logger.info(converterErrors.toString());
        }
    }

    private void credentialsConversion(CredentialsConfig credentialsConfig) {
        try {
            Class<? extends CredentialsConfig> credentialsConfigClass = credentialsConfig.getClass();
            Field credentialsField = credentialsConfigClass.getDeclaredField("credentials");
            credentialsField.setAccessible(true);
            Object credentialsObj = credentialsField.get(credentialsConfig);

            if (credentialsObj != null) {
                Credentials credentials = (Credentials) credentialsObj;
                Field usernameField = credentialsConfigClass.getDeclaredField("username");
                usernameField.setAccessible(true);
                usernameField.set(credentialsConfig, credentials.getUsername());
                Field passwordField = credentialsConfigClass.getDeclaredField("password");
                passwordField.setAccessible(true);
                passwordField.set(credentialsConfig, credentials.getPassword());
            }
        } catch (NoSuchFieldException e) {
            converterErrors.add(getConversionErrorMessage(credentialsConfig, e));
        } catch (IllegalAccessException e) {
            converterErrors.add(getConversionErrorMessage(credentialsConfig, e));
        }
    }

    private String getConversionErrorMessage(CredentialsConfig credentialsConfig, Exception e) {
        return String.format("Could not convert the class '%s' to use the new format. " +
                "Cause: %s", credentialsConfig.getClass().getName(), e.getCause());
    }
}
