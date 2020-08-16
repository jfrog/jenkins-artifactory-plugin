/**
 * @author Yahav Itzhak
 */

function loadBuild(buildData, doDisplaySuccessMessage) {
    var spinner;
    var target;

    spinner = document.getElementById("spinnerDiv");
    spinner.style.display = "block";
    target = spinner.next();
    target.innerHTML = "";

    var buildId = document.getElementById("buildId").value;
    if (!buildId) {
        displayErrorResponse(spinner, target, "Please choose a build");
    } else {
        res = JSON.parse(buildData);
        if (!res.success) {
            displayErrorResponse(spinner, target, res["responseMessage"]);
            return;
        }
        var repositoryKeys = res["repositories"];
        var plugins = res["plugins"];
        var promotionConfigs = res["promotionConfigs"];
        var promotionConfig

        for (var i = 0; i < promotionConfigs.length; i++) {
            promotionConfig = promotionConfigs[i];
            if (promotionConfig["id"] == buildId) {
                break;
            }
        }

        var selectPlugin = document.getElementById("pluginList");
        var selectTarget = document.getElementById("targetRepositoryKey");
        var selectSource = document.getElementById("sourceRepositoryKey");

        removePlugins(selectPlugin);
        removeElements(selectTarget);
        removeElements(selectSource);

        var pluginNames = extractPluginNames(plugins);
        createPluginElements(plugins);
        fillSelect(selectPlugin, pluginNames);
        fillSelect(selectTarget, repositoryKeys);
        fillSelect(selectSource, repositoryKeys);
        fillNonePluginFormDefaultValues(promotionConfig, selectTarget, selectSource);
        selectPlugin.onchange();
        if (doDisplaySuccessMessage){
            displaySuccessMessage(spinner, target);
        } else {
            spinner.style.display = "none";
        }
    }
}

function removePlugins(selectPlugin) {
    // Remove customized plugins params, e.g. all plugins except the None plugin.
    var customizedPluginFormContainer = document.getElementById("customizedPluginFormContainer");
    if (customizedPluginFormContainer) {
        customizedPluginFormContainer.parentNode.removeChild(customizedPluginFormContainer);
    }
    // Clear the select plugin dropdown.
    removeElements(selectPlugin);
}

function fillNonePluginFormDefaultValues(promotionConfig, selectTarget, selectSource) {
    var promotionComment = document.getElementById("promotionComment");
    var includeDependencies = document.getElementById("includeDependencies");
    var useCopy = document.getElementById("useCopy");
    var failFast = document.getElementById("failFast");

    selectSource.value = promotionConfig["sourceRepo"];
    selectTarget.value = promotionConfig["targetRepo"];
    promotionComment.value = promotionConfig["comment"];
    promotionComment.innerText = promotionConfig["comment"];
    includeDependencies.checked = promotionConfig["includeDependencies"];
    useCopy.checked = promotionConfig["copy"];
    failFast.checked = promotionConfig["failFast"];
    fillTargetStatuses(promotionConfig);
}

function fillTargetStatuses(promotionConfig) {
    var statuses = ["Released", "Rolled-back"];
    var requestedStatus = promotionConfig["status"] ? stringCapitalize(promotionConfig["status"]) : statuses[0];
    if (statuses.indexOf(requestedStatus) == -1) {
        statuses.push(requestedStatus);
    }
    var selectTargetStatus = document.getElementById("targetStatus");
    removeElements(selectTargetStatus);
    var statusesSelectElementValues = prepareForFillSelect(statuses);
    fillSelect(selectTargetStatus, statusesSelectElementValues);
    selectTargetStatus.value = requestedStatus;
}

// Gets array of strings and return an array of [{value: name}]
function prepareForFillSelect(values) {
    var selectElementValues = [];
    for (var i = 0; i < values.length; i++) {
        selectElementValues.push({value: values[i]});
    }
    return selectElementValues
}

// Gets array of UserPluginInfo (see UserPluginInfo.java) and return an array of [{value: name}]
function extractPluginNames(plugins) {
    var pluginNames = [];
    for (var i = 0; i < plugins.length; i++) {
        pluginNames.push({value: plugins[i]["pluginName"]});
    }
    return pluginNames;
}

function stringCapitalize(string) {
    return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
}

function onPluginChange() {
    var options = this.options; // options of the "pluginList" element
    for (var i = 0; i < options.length; i++) {
        var option = options[i];
        // Each promotion plugin has it's own body.
        // On plugin select we need to show only the selected plugin fields.
        // The 'None' plugin is the only one without the "plugin_" prefix
        // The 'None' plugin is always first.
        var pluginId = (i === 0) ? option.value : "plugin_" + option.value;
        var pluginForm = document.getElementById(pluginId);
        if (option.selected) {
            pluginForm.removeAttribute("field-disabled");
            pluginForm.removeAttribute("style");
        } else {
            pluginForm.setAttribute("field-disabled", "true");
            pluginForm.setAttribute("style", "display: none;");
        }
    }
}

function createPluginElements(plugins) {
    var pluginListContainer = document.getElementById("pluginList-container");
    // We need some separation between the customized and None plugins params.
    // All of the customized plugin params will be in the customized plugin form container div.
    var customizedPluginFormContainer = document.createElement("div");
    customizedPluginFormContainer.setAttribute("id", "customizedPluginFormContainer");
    // The first element in pluginList-container is the None plugin form.
    // Therefore, we need to append only the customized user plugins to the pluginList-container.
    for (var i = 1; i < plugins.length; i++) {
        var pluginName = plugins[i]["pluginName"];
        var pluginId = "plugin_" + pluginName;
        var pluginFormContainer = createPluginFormContainer(pluginName, pluginId);
        var pluginParams = plugins[i]["pluginParams"];
        appendPluginParams(pluginParams, pluginFormContainer);
        customizedPluginFormContainer.appendChild(pluginFormContainer);
    }
    pluginListContainer.appendChild(customizedPluginFormContainer);
}

function appendPluginParams(pluginParams, pluginFormContainer) {
    for (var j = 0; j < pluginParams.length; j++) {
        var paramName = pluginParams[j].key;
        var defaultValue = pluginParams[j].defaultValue;
        var pluginParamsWrapper = document.createElement("tr");
        pluginParamsWrapper.appendChild(createIndentationElement());
        pluginParamsWrapper.appendChild(createParamNameElement(paramName));
        pluginParamsWrapper.appendChild(createInputElement(paramName, defaultValue));
        pluginFormContainer.appendChild(pluginParamsWrapper);
    }
}

function createPluginFormContainer(pluginName, pluginId) {
    var formContainer = document.createElement("tbody");
    formContainer.setAttribute("id", pluginId);
    formContainer.setAttribute("field-disabled", "true");
    formContainer.setAttribute("style", "display: none;");
    var pluginTr = document.createElement("tr");
    var input = document.createElement("input");
    input.setAttribute("name", "pluginName");
    input.setAttribute("type", "hidden");
    input.setAttribute("value", pluginName);
    pluginTr.appendChild(input);
    formContainer.appendChild(pluginTr);
    return formContainer;
}

function createIndentationElement() {
    var element = document.createElement("td");
    element.setAttribute("class", "setting-leftspace");
    element.setAttribute("value", "&nbsp;");
    return element;
}

function createParamNameElement(paramName) {
    var element = document.createElement("td");
    element.setAttribute("class", "setting-name");
    element.innerText = paramName;
    return element
}

function createInputElement(paramName, defaultValue) {
    var InputContainer = document.createElement("td");
    InputContainer.setAttribute("class", "setting-main");
    var input = document.createElement("input");
    input.setAttribute("name", paramName);
    input.setAttribute("type", "text");
    input.setAttribute("class", "setting-input");
    input.setAttribute("value", defaultValue);
    InputContainer.appendChild(input);
    return InputContainer;
}