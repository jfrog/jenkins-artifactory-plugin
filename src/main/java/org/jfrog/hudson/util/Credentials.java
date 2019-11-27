/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.util.Scrambler;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Credentials model object
 *
 * @author Noam Y. Tenne
 */
public class Credentials implements Serializable {
    public static final Credentials EMPTY_CREDENTIALS = new Credentials(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    private final String username;
    private final String password;
    private final String accessToken;

    /**
     * Main constructor
     *
     * @param username Username
     * @param password Clear-text password. Will be scrambled.
     */
    private Credentials(String username, String password, String accessToken) {
        this.username = username;
        this.password = Scrambler.scramble(password);
        this.accessToken = Scrambler.scramble(accessToken);

    }

    public Credentials(String accessToken) {
        this(StringUtils.EMPTY, StringUtils.EMPTY, accessToken);

    }

    public Credentials(String username, String password) {
        this(username, password, StringUtils.EMPTY);
    }

    public static String extractUsernameFromToken(String accessToken) throws IOException {
        String payload = StringUtils.split(accessToken, '.')[1];
        byte[] decodedPayload = Base64.getDecoder().decode(payload);
        String jsonStr = new String(decodedPayload, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TokenPayload payloadObject = mapper.readValue(jsonStr, TokenPayload.class);
        int usernameStartIndex = payloadObject.sub.lastIndexOf("/") + 1;
        return payloadObject.sub.substring(usernameStartIndex);
    }

    private static class TokenPayload {
        private String sub;

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getSub() {
            return sub;
        }
    }

    public Credentials convertAccessTokenToUsernamePassword() throws java.io.IOException {
        String descrambledToken = Scrambler.descramble(accessToken);
        return new Credentials(extractUsernameFromToken(descrambledToken), descrambledToken);
    }

    /**
     * Constructor bounded to global.jelly
     * It
     */
    @DataBoundConstructor
    public Credentials(String username, String password, String resolverUsername, String resolverPassword) {
        if (StringUtils.isNotBlank(resolverUsername) && StringUtils.isNotBlank(resolverPassword)) {
            username = resolverUsername;
            password = resolverPassword;
        }

        this.username = username;
        this.password = Scrambler.scramble(password);
        this.accessToken = StringUtils.EMPTY;
    }

    /**
     * Returns the username
     *
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the clear-text password (unscrambled)
     *
     * @return Password
     */
    public String getPassword() {
        return Scrambler.descramble(password);
    }

    /**
     * Returns the access token
     *
     * @return Access token
     */
    public String getAccessToken() {
        return Scrambler.descramble(accessToken);
    }
}
