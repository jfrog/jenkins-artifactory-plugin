package org.jfrog.hudson;

import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class PluginSettings {

    private String pluginName;
    private Map<String, String> paramMap;

    public PluginSettings(String pluginName, Map<String, String> paramMap) {
        this.pluginName = pluginName;
        this.paramMap = paramMap;
    }

    public PluginSettings() {
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public String getParamsString() {
        StringBuilder sb = new StringBuilder();
        if (paramMap != null) {
            for(Map.Entry<String, String> entry : paramMap.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
            }
        }

        return sb.toString();
    }

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    @Override
    public String toString() {
        return pluginName;
    }

    public String getPluginParamValue(String pluginName, String paramKey) {
        if (!pluginName.equals(this.pluginName)) {
            return null;
        }
        return (paramMap != null) ? paramMap.get(paramKey) : null;
    }
}
