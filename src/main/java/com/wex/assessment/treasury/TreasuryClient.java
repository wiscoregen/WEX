package com.wex.assessment.treasury;

import com.wex.assessment.domain.ExchangeRate;

import java.util.List;

public interface TreasuryClient {

    List<ExchangeRate> fetchRates();
}

