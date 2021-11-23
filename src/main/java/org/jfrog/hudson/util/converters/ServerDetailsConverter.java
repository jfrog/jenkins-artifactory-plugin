package org.jfrog.hudson.util.converters;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.util.XStream2;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Lior Hasson
 */
public class ServerDetailsConverter extends XStream2.PassthruConverter<ServerDetails> {
    Logger logger = Logger.getLogger(ServerDetailsConverter.class.getName());
    List<String> converterErrors = new ArrayList<>();
    // mapping of the old ServerDetails field to the corresponding new field
    private static final Map<String, String> newToOldFields;

    static {
        newToOldFields = new HashMap<String, String>();
        newToOldFields.put("repositoryKey", "deployReleaseRepository");
        newToOldFields.put("snapshotsRepositoryKey", "deploySnapshotRepository");
        newToOldFields.put("downloadSnapshotRepositoryKey", "resolveSnapshotRepository");
        newToOldFields.put("downloadReleaseRepositoryKey", "resolveReleaseRepository");
    }

    public ServerDetailsConverter(XStream2 xstream) {
        super(xstream);
    }

    public void convertToReleaseAndSnapshotRepository(ServerDetails server) {
        Class<? extends ServerDetails> overrideClass = server.getClass();

        try {
            Field oldReleaseRepositoryField = overrideClass.getDeclaredField("downloadRepositoryKey");
            oldReleaseRepositoryField.setAccessible(true);
            Object oldReleaseRepositoryValue = oldReleaseRepositoryField.get(server);

            if (oldReleaseRepositoryValue != null && StringUtils.isNotBlank((String) oldReleaseRepositoryValue)) {
                Field newReleaseRepositoryField = overrideClass.getDeclaredField("downloadReleaseRepositoryKey");
                newReleaseRepositoryField.setAccessible(true);
                newReleaseRepositoryField.set(server, oldReleaseRepositoryValue);

                Field newSnapshotRepositoryField = overrideClass.getDeclaredField("downloadSnapshotRepositoryKey");
                newSnapshotRepositoryField.setAccessible(true);
                newSnapshotRepositoryField.set(server, oldReleaseRepositoryValue);
            }
        } catch (NoSuchFieldException e) {
            converterErrors.add(getConversionErrorMessage(server, e));
        } catch (IllegalAccessException e) {
            converterErrors.add(getConversionErrorMessage(server, e));
        }
    }

    public void convertToDynamicReposSelection(ServerDetails server) {
        Class<? extends ServerDetails> overrideClass = server.getClass();
        for (Map.Entry<String, String> e : newToOldFields.entrySet()) {
            setNewReposFieldFromOld(server, overrideClass, e.getKey(), e.getValue());
        }
    }

    private void setNewReposFieldFromOld(ServerDetails reflectedObject, Class classToChange, String oldFieldName,
                                         String newFieldName) {
        try {
            Field oldField = classToChange.getDeclaredField(oldFieldName);
            oldField.setAccessible(true);
            String oldValue = (String) oldField.get(reflectedObject);
            if (StringUtils.isNotBlank(oldValue)) {
                Field newField = classToChange.getDeclaredField(newFieldName);
                RepositoryConf newValue = new RepositoryConf(oldValue, oldValue, false);
                newField.setAccessible(true);
                newField.set(reflectedObject, newValue);
            }
        } catch (NoSuchFieldException e) {
            converterErrors.add(getConversionErrorMessage(reflectedObject, e));
        } catch (IllegalAccessException e) {
            converterErrors.add(getConversionErrorMessage(reflectedObject, e));
        }
    }

    @Override
    protected void callback(ServerDetails server, UnmarshallingContext context) {
        convertToReleaseAndSnapshotRepository(server);
        convertToDynamicReposSelection(server);

        if (!converterErrors.isEmpty()) {
            logger.info(converterErrors.toString());
        }
    }

    private String getConversionErrorMessage(ServerDetails serverDetails, Exception e) {
        return String.format("Could not convert the class '%s' to use the new overriding Resolve repositories." +
                " Cause: %s", serverDetails.getClass().getName(), e.getCause());
    }
}
