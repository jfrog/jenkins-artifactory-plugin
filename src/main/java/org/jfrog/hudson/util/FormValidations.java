package org.jfrog.hudson.util;

import com.google.common.base.Strings;
import hudson.Util;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for form validations.
 *
 * @author Yossi Shaul
 */
public abstract class FormValidations {

    private static final Pattern VALID_EMAIL_PATTERN = Pattern.compile(
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z]{2,}){1}$)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Validates a space separated list of emails.
     *
     * @param emails Space separated list of emails
     * @return {@link hudson.util.FormValidation.ok()} if valid or empty, error otherwise
     */
    public static FormValidation validateEmails(String emails) {
        if (!Strings.isNullOrEmpty(emails)) {
            String[] recipients = StringUtils.split(emails, " ");
            for (String email : recipients) {
                FormValidation validation = validateInternetAddress(email);
                if (validation != FormValidation.ok()) {
                    return validation;
                }
            }
        }
        return FormValidation.ok();
    }

    /**
     * Validates an internet address (url, email address, etc.).
     *
     * @param address The address to validate
     * @return {@link hudson.util.FormValidation.ok()} if valid or empty, error otherwise
     */
    public static FormValidation validateInternetAddress(String address) {
        if (Strings.isNullOrEmpty(address)) {
            return FormValidation.ok();
        }
        Matcher matcher = VALID_EMAIL_PATTERN.matcher(address);
        if (matcher.matches()) {
            return FormValidation.ok();
        } else {
            return FormValidation.error("Email address is invalid");
        }
    }

    /**
     * Validate the Combination filter field in Multi configuration jobs
     */
    public static FormValidation validateArtifactoryCombinationFilter(String value)
            throws IOException, InterruptedException {
        String url = Util.fixEmptyAndTrim(value);
        if (url == null)
            return FormValidation.error("Mandatory field - You don`t have any deploy matches");

        return FormValidation.ok();
    }
}
