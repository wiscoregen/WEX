package com.wex.assessment.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
    void parseUsdToCentsAcceptsJsonTextAndNumericNodes() {
        assertThat(MoneyUtils.parseUsdToCents(JsonNodeFactory.instance.textNode("12.345"))).isEqualTo(1_235L);
        assertThat(MoneyUtils.parseUsdToCents(JsonNodeFactory.instance.numberNode(12.345))).isEqualTo(1_235L);
    }

    @Test
    void parseUsdToCentsRejectsNullAndNonScalarJsonNodes() {
        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents((com.fasterxml.jackson.databind.JsonNode) null))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount is required");

        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents(JsonNodeFactory.instance.objectNode()))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be a JSON string or number");
    }

    @Test
    void parseUsdToCentsRejectsRoundedZeroAndInvalidValues() {
        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents("0.004"))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be greater than zero");

        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents("0"))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be greater than zero");

        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents("-12.34"))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be greater than zero");

        assertThatThrownBy(() -> MoneyUtils.parseUsdToCents("abc"))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be a positive decimal amount");
    }

    @Test
    void multiplyCentsByRateAndRoundWorksForConvertedCurrencyCents() {
        assertThat(MoneyUtils.multiplyCentsByRateAndRound(1_235L, "0.90")).isEqualTo(1_112L);
    }

    @Test
    void formatCentsFormatsWholeAndNegativeAmounts() {
        assertThat(MoneyUtils.formatCents(0)).isEqualTo("0.00");
        assertThat(MoneyUtils.formatCents(1_235L)).isEqualTo("12.35");
        assertThat(MoneyUtils.formatCents(-1_235L)).isEqualTo("-12.35");
    }
}
