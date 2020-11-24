/**
 * @author Dima Nevelev
 */
function setSpecView(isUsesSpec, legacyDownloadPatternLength, legacyUploadPatternLength, uniqueId) {
    if (!isUsesSpec) {
        isUsesSpec = legacyUploadPatternLength === 0 && legacyDownloadPatternLength === 0 ? "true" : "false";
        getElementByUniqueId("useSpecs_" + isUsesSpec, uniqueId).setAttribute("checked", "true");
    }
    updateViewBySpecsParam(isUsesSpec, uniqueId)
}

function updateViewBySpecsParam(isUsesSpec, uniqueId) {
    // Resolution section
    let downloadSpecArea = getElementByUniqueId("downloadSpecArea", uniqueId);
    let resolveLegacyPatternArea = getElementByUniqueId("resolvePatternArea", uniqueId);
    let resolverSpec = getElementByUniqueId("artifactory-resolver-spec-dd", uniqueId);
    let resolverLegacy = getElementByUniqueId("artifactory-resolver-generic-dd", uniqueId);

    // Deployment section
    let uploadSpecArea = getElementByUniqueId("uploadSpecArea", uniqueId);
    let deployLegacyPatternArea = getElementByUniqueId("deployPatternArea", uniqueId);
    let deployerSpec = getElementByUniqueId("artifactory-deployer-spec-dd", uniqueId);
    let deployerLegacy = getElementByUniqueId("artifactory-deployer-generic-dd", uniqueId);
    
    // Jenkins Core 2.264 and above uses div for layout rather than tables.
    const tagName = resolverSpec.tagName.toLowerCase() === 'div' ? 'div' : 'table'

    let specView = "none", legacyView = "none";
    if (isUsesSpec === "true") {
        // Resolution section
        // By changing the name the configuration will decide which server to use
        resolverSpec.getElementsByTagName(tagName)[0].setAttribute("name", "resolverDetails");
        resolverLegacy.getElementsByTagName(tagName)[0].setAttribute("name", "artifactory-resolver-generic-dd");

        // Deployment section
        // By changing the name the configuration will decide which server to use
        deployerSpec.getElementsByTagName(tagName)[0].setAttribute("name", "deployerDetails");
        deployerLegacy.getElementsByTagName(tagName)[0].setAttribute("name", "artifactory-deployer-generic-dd");

        specView = "";
    } else {
        // Resolution section
        // By changing the name the configuration will decide which server to use
        resolverSpec.getElementsByTagName(tagName)[0].setAttribute("name", "artifactory-resolver-spec-dd");
        resolverLegacy.getElementsByTagName(tagName)[0].setAttribute("name", "resolverDetails");

        // Deployment section
        // By changing the name the configuration will decide which server to use
        deployerSpec.getElementsByTagName(tagName)[0].setAttribute("name", "artifactory-deployer-spec-dd");
        deployerLegacy.getElementsByTagName(tagName)[0].setAttribute("name", "deployerDetails");

        legacyView = "";
    }
    resolverSpec.style.display = downloadSpecArea.style.display = deployerSpec.style.display = uploadSpecArea.style.display = specView;
    resolverLegacy.style.display = resolveLegacyPatternArea.style.display = deployerLegacy.style.display = deployLegacyPatternArea.style.display = legacyView;
}

function fixView(configurationSectionSize, uniqueId) {
    let elementsToFix = ["artifactory-deployer-generic-dd",
        "artifactory-deployer-spec-dd",
        "deployPatternArea",
        "uploadSpecArea",
        "artifactory-resolver-generic-dd",
        "artifactory-resolver-spec-dd",
        "resolvePatternArea",
        "downloadSpecArea"];
    let elementsToSkip = ["Spec", "File Path"];
    let numberOfElements = elementsToFix.length;
    let numberOfInnerElements = 0;
    // 161 is the length of the longest label element in pixels ("Artifactory resolver server")
    let newLabelSize = 100 * 161 / configurationSectionSize;
    // label size lower than 12 look bad next to checkbox
    newLabelSize = newLabelSize < 12 ? 12 : newLabelSize;

    for (let i = 0; i < numberOfElements; i++) {
        let element = getElementByUniqueId(elementsToFix[i], uniqueId);
        numberOfInnerElements = element.parentElement.getElementsByClassName("setting-name").length;
        for (let j = 0; j < numberOfInnerElements; j++) {
            if (elementsToSkip.indexOf(element.parentElement.getElementsByClassName("setting-name")[j].textContent) === -1) {
                try {
                    element.parentElement.getElementsByClassName("setting-name")[j].setAttribute("width", newLabelSize + "%");
                } catch (e) {
                    console.log(e);
                    console.log(element.parentElement.getElementsByClassName("setting-name")[j]);
                }
            }
        }
    }
}
