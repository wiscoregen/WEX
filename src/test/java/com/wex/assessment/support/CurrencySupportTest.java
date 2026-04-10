package com.wex.assessment.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencySupportTest {

    @Test
    void recognizesValidIsoCodesWithWhitespaceAndCaseDifferences() {
        assertThat(CurrencySupport.isValidIso4217("USD")).isTrue();
        assertThat(CurrencySupport.isValidIso4217(" eur ")).isTrue();
        assertThat(CurrencySupport.isValidIso4217("jPy")).isTrue();
    }

    @Test
    void rejectsBlankOrUnknownIsoCodes() {
        assertThat(CurrencySupport.isValidIso4217(null)).isFalse();
        assertThat(CurrencySupport.isValidIso4217("   ")).isFalse();
        assertThat(CurrencySupport.isValidIso4217("EURO")).isFalse();
    }
}
