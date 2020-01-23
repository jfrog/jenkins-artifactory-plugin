/**
 * @author Lior Hasson
 */

function repos(button, jsFunction, uniqueId, artifactoryUrl, credentialsInput, bind) {
    let username, password, credentialsId, overrideCredentials;

    // Start spinner
    button = button._button;
    let spinner = $(button).up("DIV").next();
    spinner.style.display = "block";
    let target = spinner.next();
    target.innerHTML = "";
    let warning = target.next();
    warning.innerHTML = "";

    let legacyInput = $('legacy' + credentialsInput);
    if (legacyInput) {
        overrideCredentials = legacyInput.down('input[type=checkbox]').checked;
        username = legacyInput.down('input[type=text]').value;
        password = legacyInput.down('input[type=password]').value;
    }
    let credentialsPluginInput = $(credentialsInput);
    if (credentialsPluginInput) {
        credentialsId = $(credentialsInput).down('select').value;
    }

    if (jsFunction) {
        jsFunctionsMap[jsFunction](spinner, uniqueId, $(artifactoryUrl).value, credentialsId, username, password, overrideCredentials, bind);
    }
}

// maps a function name to the function object
// noinspection JSUnusedGlobalSymbols
let jsFunctionsMap = {
    artifactoryIvyFreeStyleConfigurator: artifactoryIvyFreeStyleConfigurator,
    artifactoryGenericConfigurator: artifactoryGenericConfigurator,
    artifactoryMaven3NativeConfigurator: artifactoryMaven3NativeConfigurator,
    artifactoryMaven3Configurator: artifactoryMaven3Configurator,
    artifactoryGradleConfigurator: artifactoryGradleConfigurator,
    artifactoryGradleConfigurationResolve: artifactoryGradleConfigurationResolve,
    artifactoryRedeployPublisher: artifactoryRedeployPublisher,
    artifactoryIvyConfigurator: artifactoryIvyConfigurator,
};


function artifactoryIvyFreeStyleConfigurator(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let select = getElementByUniqueId("select_ivyFreeRepositoryKeys-" + artifactoryUrl, uniqueId);
        let oldValue = select.value;
        let oldSelect = select.cloneNode(true);
        removeElements(select);
        fillSelect(select, response.repositories);
        setSelectValue(select, oldValue);

        let oldValueExistsInNewList = compareSelectTags(select, oldSelect);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);
    });
}

function artifactoryGenericConfigurator(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let select = getElementByUniqueId("select_genericRepositoryKeys-" + artifactoryUrl, uniqueId);
        let oldValue = select.value;
        let oldSelect = select.cloneNode(true);
        removeElements(select);
        fillSelect(select, response.repositories);
        setSelectValue(select, oldValue);

        let oldValueExistsInNewList = compareSelectTags(select, oldSelect);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);
    });
}

function artifactoryMaven3NativeConfigurator(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshResolversFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let selectRelease = getElementByUniqueId("select_maven3NativeReleaseRepositoryKeys-" + artifactoryUrl, uniqueId);
        let selectSnapshot = getElementByUniqueId("select_maven3NativeSnapshotRepositoryKeys-" + artifactoryUrl, uniqueId);

        let oldReleaseValue = selectRelease.value;
        let oldSnapshotValue = selectSnapshot.value;

        let oldSelectRelease = selectRelease.cloneNode(true);
        let oldSelectSnapshot = selectSnapshot.cloneNode(true);

        removeElements(selectRelease);
        removeElements(selectSnapshot);

        fillVirtualReposSelect(selectRelease, response.virtualRepositories);
        fillVirtualReposSelect(selectSnapshot, response.virtualRepositories);

        setSelectValue(selectRelease, oldReleaseValue);
        setSelectValue(selectSnapshot, oldSnapshotValue);

        let oldValueExistsInNewList = compareSelectTags(selectRelease, oldSelectRelease) && compareSelectTags(selectSnapshot, oldSelectSnapshot);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);
    });
}

function artifactoryMaven3Configurator(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let selectRelease = getElementByUniqueId("select_maven3RepositoryKeys-" + artifactoryUrl, uniqueId);
        let selectSnapshot = getElementByUniqueId("select_maven3SnapshotsRepositoryKeys-" + artifactoryUrl, uniqueId);

        let oldReleaseValue = selectRelease.value;
        let oldSnapshotValue = selectSnapshot.value;

        let oldSelectRelease = selectRelease.cloneNode(true);
        let oldSelectSnapshot = selectSnapshot.cloneNode(true);

        removeElements(selectRelease);
        removeElements(selectSnapshot);

        fillSelect(selectRelease, response.repositories);
        fillSelect(selectSnapshot, response.repositories);

        setSelectValue(selectRelease, oldReleaseValue);
        setSelectValue(selectSnapshot, oldSnapshotValue);

        let oldValueExistsInNewList = compareSelectTags(selectRelease, oldSelectRelease) && compareSelectTags(selectSnapshot, oldSelectSnapshot);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);

    });
}

function artifactoryGradleConfigurator(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }

        let selectPublish = getElementByUniqueId("select_gradlePublishRepositoryKeys-" + artifactoryUrl, uniqueId);
        let selectPlugins = getElementByUniqueId("gradleCustomStagingConfiguration-" + artifactoryUrl, uniqueId);

        let oldPublishValue = selectPublish.value;
        let oldPluginsValue = selectPlugins.value;

        let oldSelectPublish = selectPublish.cloneNode(true);
        let oldSelectPlugins = selectPlugins.cloneNode(true);

        removeElements(selectPublish);
        removeElements(selectPlugins);

        fillSelect(selectPublish, response.repositories);
        fillStagingPluginsSelect(selectPlugins, response.userPlugins);
        createStagingParamsInputs(response.userPlugins, uniqueId);

        setSelectValue(selectPublish, oldPublishValue);
        setSelectValue(selectPlugins, oldPluginsValue);
        setStagingParamsSelectedValue(selectPlugins, uniqueId);

        let oldValueExistsInNewList = compareSelectTags(selectPublish, oldSelectPublish) && compareSelectTags(selectPlugins, oldSelectPlugins);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);

    });
}

function artifactoryGradleConfigurationResolve(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshResolversFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let selectResolution = getElementByUniqueId("select_gradleResolutionRepositoryKeys-" + artifactoryUrl, uniqueId);
        let oldResolutionValue = selectResolution.value;
        let oldSelectResolution = selectResolution.cloneNode(true);
        removeElements(selectResolution);
        fillVirtualReposSelect(selectResolution, response.virtualRepositories);
        setSelectValue(selectResolution, oldResolutionValue);
        let oldValueExistsInNewList = compareSelectTags(selectResolution, oldSelectResolution);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);
    });
}

function artifactoryRedeployPublisher(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let selectRelease = getElementByUniqueId("select_publishRepositoryKey-" + artifactoryUrl, uniqueId);
        let selectSnapshot = getElementByUniqueId("select_publishSnapshotsRepositoryKeys-" + artifactoryUrl, uniqueId);
        let selectPlugins = getElementByUniqueId("customStagingConfiguration-" + artifactoryUrl, uniqueId);

        let oldReleaseValue = selectRelease.value;
        let oldSnapshotValue = selectSnapshot.value;
        let oldPluginsValue = selectPlugins.value;

        let oldSelectRelease = selectRelease.cloneNode(true);
        let oldSelectSnapshot = selectSnapshot.cloneNode(true);
        let oldSelectPlugins = selectPlugins.cloneNode(true);

        removeElements(selectRelease);
        removeElements(selectSnapshot);
        removeElements(selectPlugins);

        fillSelect(selectSnapshot, response.repositories);
        fillSelect(selectRelease, response.repositories);
        fillStagingPluginsSelect(selectPlugins, response.userPlugins);
        createStagingParamsInputs(response.userPlugins, uniqueId);

        setSelectValue(selectRelease, oldReleaseValue);
        setSelectValue(selectSnapshot, oldSnapshotValue);
        setSelectValue(selectPlugins, oldPluginsValue);
        setStagingParamsSelectedValue(selectPlugins, uniqueId);

        let oldValueExistsInNewList = compareSelectTags(selectRelease, oldSelectRelease) &&
            compareSelectTags(selectSnapshot, oldSelectSnapshot) && compareSelectTags(selectPlugins, oldSelectPlugins);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);
    });
}

function artifactoryIvyConfigurator(spinner, uniqueId, artifactoryUrl, credentialsId, username, password, overrideCredentials, bind) {
    // noinspection JSUnresolvedFunction
    bind.refreshFromArtifactory(spinner, artifactoryUrl, credentialsId, username, password, overrideCredentials, function (t) {
        let target = spinner.next();
        let warning = target.next();

        let response = t.responseObject();
        if (!response.success) {
            displayErrorResponse(spinner, target, response.responseMessage);
            return;
        }
        let select = getElementByUniqueId("select_publishRepositoryKey-" + artifactoryUrl, uniqueId);
        let oldValue = select.value;
        let oldSelect = select.cloneNode(true);
        removeElements(select);
        fillSelect(select, response.repositories);
        setSelectValue(select, oldValue);

        let oldValueExistsInNewList = compareSelectTags(select, oldSelect);
        if (!oldValueExistsInNewList) {
            displayWarningMessage(warning);
        }
        displaySuccessMessage(spinner, target);
    });
}

function fillSelect(select, list) {
    let txtId = "txt_" + select.id;
    let txtElement = document.getElementById(txtId);
    // noinspection EqualityComparisonWithCoercionJS
    if (list.length > 0 && txtElement != undefined && txtElement.value === "") {
        txtElement.value = list[0];
    }
    for (let i = 0; i < list.length; i++) {
        let item = list[i];
        let option = document.createElement("option");
        option.text = item.value;
        option.innerText = item.value;
        option.value = item.value;
        select.appendChild(option);
    }
}

function fillVirtualReposSelect(select, list) {
    let txtId = "txt_" + select.id;
    let txtElement = document.getElementById(txtId);
    // noinspection EqualityComparisonWithCoercionJS
    if (list.length > 0 && txtElement != undefined && txtElement.value === "") {
        txtElement.value = list[0].value;
    }
    for (let i = 0; i < list.length; i++) {
        let item = list[i];
        let option = document.createElement("option");
        option.text = item.displayName;
        option.innerText = item.displayName;
        option.value = item.value;
        select.appendChild(option);
    }
}

function fillStagingPluginsSelect(select, list) {
    for (let i = 0; i < list.length; i++) {
        let item = list[i];
        let option = document.createElement("option");
        option.text = item.pluginName;
        option.innerText = item.pluginName;
        option.value = item.pluginName;
        select.appendChild(option);
    }
}

function createStagingParamsInputs(list, uniqueId) {
    let str = "";
    for (let i = 0; i < list.length; i++) {
        let item = list[i];
        str += "<input class='setting-input' " +
            "style='display:none' " +
            "id='stagingParams-" + item.pluginName + "-" + uniqueId + "' " +
            "type='text' " +
            "value='" + item.paramsString + "' />";
    }

    getElementByUniqueId("stagingParamsDiv", uniqueId).innerHTML = str;
}

function setStagingParamsSelectedValue(select, uniqueId) {
    for (let i = 0; i < select.options.length; i++) {
        let display = (i === select.selectedIndex) ? "" : "none";
        let inputName = "stagingParams-" + select.options[i].value + "-" + uniqueId;
        let input = document.getElementById(inputName);
        input.style.display = display;
        input.setAttribute("name", display ? "userPluginParams" : "");
        if (!display) {
            let div = getElementByUniqueId("stagingParamsDiv", uniqueId);
            div.style.display = input.value ? "" : "none";
        }
    }
}

function displaySuccessMessage(spinner, target) {
    spinner.style.display = "none";
    target.innerHTML = "Items refreshed successfully";
    target.removeClassName('error');
    target.style.color = "green";
}

function displayErrorResponse(spinner, target, message) {
    spinner.style.display = "none";
    target.innerHTML = message;
    target.addClassName('error');
    target.style.color = "red";
}

function displayWarningMessage(warning) {
    warning.innerHTML = "Warning! One of your previously configured items does not exist.";
    warning.style.color = "orange"
}

function removeElements(e) {
    while (e.firstChild) {
        e.removeChild(e.firstChild);
    }
}

function setSelectValue(select, value) {
    for (let i = 0; i < select.options.length; i++) {
        if (select.options[i].value === value) {
            select.selectedIndex = i;
            return;
        }
    }
}

function compareSelectTags(newRepos, oldRepos) {
    if (oldRepos.options.length === 0) {
        return true;
    }

    for (let i = 0; i < oldRepos.length; i++) {
        let itemOld = oldRepos[i].value;
        let flag = false;
        for (let j = 0; j < newRepos.length; j++) {
            let itemNew = newRepos[j].value;
            if (itemNew.value === itemOld.value) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            return false;
        }
    }
    return true;
}

// toggle button onClick callback
// noinspection JSUnusedGlobalSymbols
function toggleTxtAndSelect(txtId, txtModeId) {
    let select = document.getElementById('select_' + txtId);
    let txt = document.getElementById(txtId);
    let shouldUseText = document.getElementById(txtModeId);
    let button = document.getElementById('btn_' + txtId);

    let currentValue = shouldUseText.value;
    if (currentValue === undefined || currentValue === "") {
        currentValue = false;
    }

    shouldUseText.value = !JSON.parse(currentValue);
    swapHiddenValue(txt, select, button);
}

function swapHiddenValue(txt, select, button) {
    if (txt.style.display === '') {
        txt.style.display = 'none';
        select.style.display = '';
        button.firstChild.firstChild.innerHTML = "Different Value";
    } else {
        select.style.display = 'none';
        txt.style.display = '';
        // noinspection SqlNoDataSourceInspection,SqlResolve
        button.firstChild.firstChild.innerHTML = "Select from List";
    }
}

function initTextAndSelectOnLoad(label, txtValue, selectValue) {
    let select = document.getElementById('select_' + label);
    let txt = document.getElementById(label);
    let button = document.getElementById('btn_' + label);
    // noinspection EqualityComparisonWithCoercionJS
    if (select != undefined && txt != undefined) {
        txt.style.display = txtValue;
        select.style.display = selectValue;
        // noinspection EqualityComparisonWithCoercionJS
        if (button != undefined) {
            if (txtValue === '') {
                // noinspection SqlNoDataSourceInspection,SqlResolve
                button.value = "Select from List";
            } else {
                button.value = "Different Value";
            }
        }
    }
}

/**
 * Get element by id and unique id.
 * @param elementId - The element id
 * @param uniqueId - Unique id generated in the Jelly
 * @returns {HTMLElement} - The element
 */
function getElementByUniqueId(elementId, uniqueId) {
    return document.getElementById(elementId + "-" + uniqueId)
}