/**
 * Created by alexeiv on 01/03/2017.
 */
function populateDropDownList() {
    debugger;
    var artifactoryServers = document.getElementsByName("artifactoryServer");
    var t = document.getElementsByName("artifactoryServer")[artifactoryServers.length - 1];
    var button = t.nextElementSibling.nextElementSibling;

    button.onclick = function () {
        var retryValue = document.getElementsByName('connectionRetry');
        for (var i = 0; i < retryValue.length; i++) {
            if (retryValue[i].length == 0) {
                // populate the selection fields and set 3 as default.
                for (var j = 0; j < 10; j++) {
                    var el = document.createElement("option");
                    el.textContent = j;
                    el.value = j;
                    retryValue[i].appendChild(el);
                    if (j == 3) {
                        retryValue[i].selectedIndex = j;
                    }
                }
            }
        }

        // Populates the timeout field
        var timeout = document.getElementsByName("_.artifactory.timeout");
        var value = 300;
        for (var i = 0; i < timeout.length; i++) {
            if (!timeout[i].value) {
                timeout[i].value = value;
            }
        }
    }
}
