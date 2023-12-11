/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Optional;

import com.google.common.cache.Cache;
import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.beans.CaptchaBundle;
import edu.cornell.mannlib.vitro.webapp.beans.CaptchaServiceBean;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import org.junit.Before;
import org.junit.Test;
import stubs.edu.cornell.mannlib.vitro.webapp.config.ConfigurationPropertiesStub;
import stubs.javax.servlet.ServletContextStub;
import stubs.javax.servlet.http.HttpServletRequestStub;
import stubs.javax.servlet.http.HttpSessionStub;

public class CaptchaServiceBeanTest extends AbstractTestClass {

    HttpServletRequestStub httpServletRequest;


    @Before
    public void createConfigurationProperties() throws Exception {
        ConfigurationPropertiesStub props = new ConfigurationPropertiesStub();
        props.setProperty("recaptcha.secretKey", "secretKey");

        ServletContextStub ctx = new ServletContextStub();
        props.setBean(ctx);

        HttpSessionStub session = new HttpSessionStub();
        session.setServletContext(ctx);

        httpServletRequest = new HttpServletRequestStub();
        httpServletRequest.setSession(session);
    }

    @Test
    public void validateReCaptcha_InvalidResponse_ReturnsFalse() throws IOException {
        // Given
        String secretKey = "WRONG_SECRET_KEY";

        // When
        boolean result = CaptchaServiceBean.validateReCaptcha("invalidResponse", secretKey);

        // Then
        assertFalse(result);
    }

    @Test
    public void generateChallenge_ValidChallengeGenerated() throws IOException {
        // When
        CaptchaBundle captchaBundle = CaptchaServiceBean.generateChallenge();

        // Then
        assertNotNull(captchaBundle);
        assertNotNull(captchaBundle.getB64Image());
        assertNotNull(captchaBundle.getCode());
        assertNotNull(captchaBundle.getCaptchaId());
    }

    @Test
    public void getChallenge_MatchingCaptchaIdAndRemoteAddress_ReturnsCaptchaBundle() {
        // Given
        String captchaId = "sampleCaptchaId";
        CaptchaBundle sampleChallenge = new CaptchaBundle("sampleB64Image", "sampleCode", captchaId);
        CaptchaServiceBean.getCaptchaChallenges().put(captchaId, sampleChallenge);

        // When
        Cache<String, CaptchaBundle> captchaChallenges = CaptchaServiceBean.getCaptchaChallenges();
        Optional<CaptchaBundle> result = CaptchaServiceBean.getChallenge(captchaId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(sampleChallenge, result.get());
        assertEquals(0, captchaChallenges.size());
    }

    @Test
    public void getChallenge_NonMatchingCaptchaIdAndRemoteAddress_ReturnsEmptyOptional() {
        // When
        Cache<String, CaptchaBundle> captchaChallenges = CaptchaServiceBean.getCaptchaChallenges();
        Optional<CaptchaBundle> result = CaptchaServiceBean.getChallenge("nonMatchingId");

        // Then
        assertFalse(result.isPresent());
        assertEquals(0, captchaChallenges.size());
    }
}