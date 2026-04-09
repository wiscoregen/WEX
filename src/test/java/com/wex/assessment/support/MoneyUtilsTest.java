package com.wex.assessment.support;

import com.wex.assessment.error.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyUtilsTest {

    @Test
    void parseUsdToCentsRoundsHalfUp() {
        assertThat(MoneyUtils.parseUsdToCents("12")).isEqualTo(1_200L);
        assertThat(MoneyUtils.parseUsdToCents("12.34")).isEqualTo(1_234L);
        assertThat(MoneyUtils.parseUsdToCents("12.345")).isEqualTo(1_235L);
        assertThat(MoneyUtils.parseUsdToCents("12.999")).isEqualTo(1_300L);
    }

    @Test
    void parseUsdToCentsRejectsRoundedZero() {
        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents("0.004"))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be greater than zero");
    }

    @Test
    void multiplyCentsByRateAndRoundWorksForConvertedCurrencyCents() {
        assertThat(MoneyUtils.multiplyCentsByRateAndRound(1_235L, "0.90")).isEqualTo(1_112L);
    }
}

