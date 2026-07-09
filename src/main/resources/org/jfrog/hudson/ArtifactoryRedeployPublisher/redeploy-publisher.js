function updateViewWithCredentials() {
    try {
        let credentialsPluginFlag = JSON.parse(document.getElementById('useCredentialsPluginoverridingDeployerCredentials').value);
        let legacyCredentials = document.getElementById('legacyoverridingDeployerCredentials');
        let credentialsPlugin = document.getElementById('overridingDeployerCredentials-${uniqueId}');
        legacyCredentials.style.display = credentialsPluginFlag ? 'none' : '';
        credentialsPlugin.style.display = credentialsPluginFlag ? '' : 'none';
    } catch (exception) {
        console.error(exception);
        setTimeout(updateViewWithCredentials, 1000);
    }
}
updateViewWithCredentials();
