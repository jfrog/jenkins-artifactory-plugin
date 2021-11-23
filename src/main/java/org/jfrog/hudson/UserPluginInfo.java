package org.jfrog.hudson;

import java.util.*;

/**
 * @author Noam Y. Tenne
 */
public class UserPluginInfo {

    public static final String NO_PLUGIN_KEY = "None";

    private String pluginName;
    private Map pluginParams;

    public static final UserPluginInfo NO_PLUGIN = new UserPluginInfo(NO_PLUGIN_KEY);

    public UserPluginInfo(Map stagingPluginInfo) {
        pluginName = stagingPluginInfo.get("name").toString();
        Object params = stagingPluginInfo.get("params");
        if (params == null) {
            pluginParams = new HashMap<>();
        } else {
            pluginParams = (Map) params;
        }
    }

    private UserPluginInfo(String name) {
        pluginName = name;
        pluginParams = new HashMap<>();
    }

    public String getPluginName() {
        return pluginName;
    }

    public List<UserPluginInfoParam> getPluginParams() {
        List<UserPluginInfoParam> pluginParamList = new ArrayList<>();
        for (Map.Entry paramEntry : ((Set<Map.Entry>) pluginParams.entrySet())) {
            pluginParamList.add(new UserPluginInfoParam(paramEntry.getKey(), paramEntry.getValue()));
        }
        Collections.sort(pluginParamList, new Comparator<UserPluginInfoParam>() {
            public int compare(UserPluginInfoParam o1, UserPluginInfoParam o2) {
                return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
        });
        return pluginParamList;
    }

    public void addParam(Object key, Object value) {
        pluginParams.put(key, value);
    }
}
