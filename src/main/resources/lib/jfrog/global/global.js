/**
 * Created by alexeiv on 01/03/2017.
 */
function populateDropDownList() {

    const DEFAULT_OPTIONS_NUMBER = 10;

    let artifactoryServers = document.getElementsByName("artifactoryServer");
    let t = document.getElementsByName("artifactoryServer")[artifactoryServers.length - 1];
    let button = t.nextElementSibling.nextElementSibling;

    button.onclick = function () {
        let retryValue = document.getElementsByName('connectionRetry');
        for (let i = 0; i < retryValue.length; i++) {
            if (retryValue[i].length === 0) {
                // populate the selection fields and set 3 as default.
                for (let j = 0; j < DEFAULT_OPTIONS_NUMBER; j++) {
                    let el = document.createElement("option");
                    el.textContent = j;
                    el.value = j;
                    retryValue[i].appendChild(el);
                    if (j === 3) {
                        retryValue[i].selectedIndex = j;
                    }
                }
            }
        }

        // Populates the timeout field
        let timeout = document.getElementsByName("_.artifactory.timeout");
        let value = 300;
        for (let i = 0; i < timeout.length; i++) {
            if (!timeout[i].value) {
                timeout[i].value = value;
            }
        }

        // Populates the deploymentThreads field
        let deploymentThreadsValue = document.getElementsByName("deploymentThreads");
        for (let i = 0; i < deploymentThreadsValue.length; i++) {
            if (deploymentThreadsValue[i].length === 0) {
                // populate the selection fields and set 3 as default.
                for (let j = 1; j < DEFAULT_OPTIONS_NUMBER; j++) {
                    let el = document.createElement("option");
                    el.textContent = j;
                    el.value = j;
                    deploymentThreadsValue[i].appendChild(el);
                    if (j === 3) {
                        deploymentThreadsValue[i].selectedIndex = j;
                    }
                }
            }
        }
    }
}
