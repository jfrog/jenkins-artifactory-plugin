/**
 * @author Roman Gurevitch
 */

function certs(button, bind) {
    button = button._button;
    var spinner = $(button).up("DIV").next();
    spinner.style.display = "block";
    var target = spinner.next();
    target.innerHTML = "";
    var warning = target.next();
    warning.innerHTML = "";
    var port = document.getElementById("buildInfoProxyPort").value;

    bind.generateCerts(spinner, port, function (t) {
        var target = spinner.next();

        var response = t.responseObject();
        if (!response) {
            displayErrorResponse(spinner, target, "Certificates already exists, to generate new certificates remove the old once.");
        }
        else {
            var publicCert = document.getElementById("buildInfoProxyCertPublic");
            var privateCert = document.getElementById("buildInfoProxyCertPrivate");
            if (!publicCert || !privateCert) {
                displayErrorResponse(spinner, target, "Certificates were not generated");
            }
            publicCert.innerText = response.key;
            privateCert.innerText = response.value;

            displaySuccessMessage(spinner, target);
        }
    });
}

function displaySuccessMessage(spinner, target) {
    spinner.style.display = "none";
    target.innerHTML = "Certificates generated successfully";
    target.removeClassName('error');
    target.style.color = "green";
}

function displayErrorResponse(spinner, target, message) {
    spinner.style.display = "none";
    target.innerHTML = message;
    target.addClassName('error');
    target.style.color = "red";
}
