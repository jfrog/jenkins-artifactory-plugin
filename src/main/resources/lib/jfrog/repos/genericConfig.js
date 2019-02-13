/**
 * @author Dima Nevelev
 */

function setSpecView(isUsesSpec, legacyDownloadPatternLength, legacyUploadPatternLength) {
    if (isUsesSpec === null || isUsesSpec.length === 0) {
      if (legacyUploadPatternLength === 0 && legacyDownloadPatternLength === 0) {
          document.getElementById("useSpecs_true").setAttribute("checked", "true");
          updateViewBySpecsParam("true");
      }  else {
          document.getElementById("useSpecs_false").setAttribute("checked", "true");
          updateViewBySpecsParam("false");
      }
    } else {
        updateViewBySpecsParam(isUsesSpec)
    }
}

function updateViewBySpecsParam(isUsesSpec) {
    if (isUsesSpec === "true") {
        // Resolution section
        // By changing the name the configuration will decide which server to use
        document.getElementById("artifactory-resolver-spec-dd").getElementsByTagName("table")[0].setAttribute("name", "resolverDetails");
        document.getElementById("artifactory-resolver-generic-dd").getElementsByTagName("table")[0].setAttribute("name", "artifactory-resolver-generic-dd");
        document.getElementById("artifactory-resolver-spec-dd").style.display = "";
        document.getElementById("artifactory-resolver-generic-dd").style.display = "none";
        document.getElementById("downloadSpecArea").style.display = "";
        document.getElementById("resolvePatternArea").style.display = "none";


        // Deployment section
        // By changing the name the configuration will decide which server to use
        document.getElementById("artifactory-deployer-spec-dd").getElementsByTagName("table")[0].setAttribute("name", "deployerDetails");
        document.getElementById("artifactory-deployer-generic-dd").getElementsByTagName("table")[0].setAttribute("name", "artifactory-deployer-generic-dd");
        document.getElementById("artifactory-deployer-spec-dd").style.display = "";
        document.getElementById("artifactory-deployer-generic-dd").style.display = "none";
        document.getElementById("uploadSpecArea").style.display = "";
        document.getElementById("deployPatternArea").style.display = "none";

    } else {
        // Resolution section
        // By changing the name the configuration will decide which server to use
        document.getElementById("artifactory-resolver-spec-dd").getElementsByTagName("table")[0].setAttribute("name", "artifactory-resolver-spec-dd");
        document.getElementById("artifactory-resolver-generic-dd").getElementsByTagName("table")[0].setAttribute("name", "resolverDetails");
        document.getElementById("artifactory-resolver-spec-dd").style.display = "none";
        document.getElementById("artifactory-resolver-generic-dd").style.display = "";
        document.getElementById("downloadSpecArea").style.display = "none";
        document.getElementById("resolvePatternArea").style.display = "";

        // Deployment section
        // By changing the name the configuration will decide which server to use
        document.getElementById("artifactory-deployer-spec-dd").getElementsByTagName("table")[0].setAttribute("name", "artifactory-deployer-spec-dd");
        document.getElementById("artifactory-deployer-generic-dd").getElementsByTagName("table")[0].setAttribute("name", "deployerDetails");
        document.getElementById("artifactory-deployer-spec-dd").style.display = "none";
        document.getElementById("artifactory-deployer-generic-dd").style.display = "";
        document.getElementById("uploadSpecArea").style.display = "none";
        document.getElementById("deployPatternArea").style.display = "";
    }
}

function fixView(configurationSectionSize) {
    var elementsToFix = ["artifactory-deployer-generic-dd",
                         "artifactory-deployer-spec-dd",
                         "deployPatternArea",
                         "uploadSpecArea",
                         "artifactory-resolver-generic-dd",
                         "artifactory-resolver-spec-dd",
                         "resolvePatternArea",
                         "downloadSpecArea"];
    var elementsToSkip = ["Spec",
                          "File Path"];
    var numberOfElements = elementsToFix.length;
    var numberOfInnerElements = 0;
    // 161 is the length of the longest label element in pixels ("Artifactory resolver server")
    var newLabelSize = 100 * 161 / configurationSectionSize;
    // label size lower than 12 look bad next to checkbox
    newLabelSize = newLabelSize < 12 ? 12 : newLabelSize;

    for (var i = 0; i < numberOfElements; i++) {
        numberOfInnerElements = document.getElementById(elementsToFix[i]).parentElement.getElementsByClassName("setting-name").length;
        for (var j = 0; j < numberOfInnerElements; j++) {
            if (elementsToSkip.indexOf(document.getElementById(elementsToFix[i]).parentElement.getElementsByClassName("setting-name")[j].textContent) == -1) {
                try {
                    document.getElementById(elementsToFix[i]).parentElement.getElementsByClassName("setting-name")[j].setAttribute("width", newLabelSize + "%");
                } catch (e) {
                    console.log(e);
                    console.log(document.getElementById(elementsToFix[i]).parentElement.getElementsByClassName("setting-name")[j]);
                }
            }
        }
    }
}