package com.yujing.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SemverTest {

    @Test
    void acceptsTwoToFourNumericSegments() {
        assertThat(Semver.isValid("1")).isTrue();
        assertThat(Semver.isValid("0.1.0")).isTrue();
        assertThat(Semver.isValid("1.2.3.4")).isTrue();
    }

    @Test
    void rejectsMalformedVersions() {
        assertThat(Semver.isValid(null)).isFalse();
        assertThat(Semver.isValid("")).isFalse();
        assertThat(Semver.isValid("v1.0.0")).isFalse();
        assertThat(Semver.isValid("1.0.0-beta")).isFalse();
        assertThat(Semver.isValid("1..0")).isFalse();
    }

    @Test
    void comparesNumericallyNotLexically() {
        assertThat(Semver.compare("1.10.0", "1.9.9")).isPositive();
        assertThat(Semver.compare("0.10.0", "0.9.0")).isPositive();
        assertThat(Semver.compare("2.0", "2.0.0")).isZero();
        assertThat(Semver.compare("0.1.0", "0.1.1")).isNegative();
    }
}
