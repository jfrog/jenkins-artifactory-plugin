package org.jfrog.hudson;

import hudson.model.BuildBadgeAction;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;

public class XrayScanResultAction implements BuildBadgeAction {

    private final Run build;
    private String url;

    public XrayScanResultAction(String xrayUrl, Run build) {
        this.build = build;
        this.url = xrayUrl;
    }

    public Run getBuild() {
        return build;
    }

    public String getIconFileName() {
        return "/plugin/artifactory/images/xray-icon.png";
    }

    public String getDisplayName() {
        return "Xray Scan Report";
    }

    public String getUrlName() {
        if (StringUtils.isNotEmpty(url)) {
            return url;
        }
        return "xray_scan_report";
    }

    public void setUrl(String url) {
        this.url = url;
    }
}