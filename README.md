# WEX Purchase Transaction Service

This repository implements the assessment as a Java 21 Spring Boot service that stores purchase transactions in USD and retrieves them in a requested ISO 4217 currency by using the most recent Treasury Reporting Rates of Exchange rate on or before the purchase date within the prior 6 calendar months.

The design goals were straightforward:

- keep money safe by storing values as integer cents
- keep the business rules easy to reason about with controller, service, and repository layers
- avoid any external database or servlet container dependencies
- cache Treasury rates locally so read requests never depend on a live upstream call
- return a consistent JSON error contract with distinct `unsupported_currency` and `no_rate_available` outcomes

## Stack

- Java 21
- Spring Boot 3.2
- Embedded Spring Boot web server
- Jackson-based JSON persistence under `data/`
- JUnit 5 and MockMvc tests

## Endpoints

### `POST /api/v1/purchases`

Stores a purchase transaction.

Request body:

```json
{
  "description": "Team lunch",
  "transactionDate": "2025-04-10",
  "purchaseAmount": "12.345"
}
```

Rules:

- `description` is trimmed and must be 1-50 characters
- `transactionDate` must be `YYYY-MM-DD`
- `purchaseAmount` may be a JSON string or number
- purchase amounts are rounded half-up to the nearest cent before persistence
- the stored amount is represented internally as integer cents

Response:

```json
{
  "id": "3f6d73f8-218e-4666-a22f-e67d5cffd3f0",
  "description": "Team lunch",
  "transactionDate": "2025-04-10",
  "purchaseAmountUsd": "12.35"
}
```

### `GET /api/v1/purchases/{id}?currency=EUR`

Retrieves a stored purchase converted into the requested target currency.

Response:

```json
{
  "id": "3f6d73f8-218e-4666-a22f-e67d5cffd3f0",
  "description": "Team lunch",
  "transactionDate": "2025-04-10",
  "originalAmountUsd": "12.35",
  "targetCurrency": "EUR",
  "exchangeRate": "0.90",
  "exchangeRateEffectiveDate": "2025-03-31",
  "convertedAmount": "11.12"
}
```

Rules:

- ISO 4217 currency validation happens at the API boundary before business logic
- the most recent cached rate on or before the purchase date is used
- the rate must fall within the previous 6 calendar months, inclusive
- `unsupported_currency` is returned when a valid ISO code is not supported by the Treasury mapping
- `no_rate_available` with HTTP `422` is returned when the currency is supported but no eligible historical rate exists

### `GET /health`

Returns a lightweight health response plus rate cache metadata.

## Error contract

All errors share the same response shape:

```json
{
  "error": {
    "code": "unsupported_currency",
    "message": "currency \"AOA\" is not supported for conversion",
    "status": 400
  }
}
```

That same envelope is now used for application-level 400, 404, 405, 406, 415, 422, and 500 responses.

One exception is a malformed raw HTTP request rejected by Tomcat before Spring MVC receives it, such as an invalid request target containing illegal characters. Those failures occur below the application layer.

## Running locally

```bash
mvn spring-boot:run
```

The service listens on port `8080` by default.

Example flow:

```bash
PURCHASE_RESPONSE=$(curl -s localhost:8080/api/v1/purchases \
  -H 'Content-Type: application/json' \
  -d '{"description":"Team lunch","transactionDate":"2025-04-10","purchaseAmount":"12.345"}')
```

```bash
PURCHASE_ID=$(echo "$PURCHASE_RESPONSE" | jq -r '.id')
curl -s "http://localhost:8080/api/v1/purchases/${PURCHASE_ID}?currency=EUR"
```

Do not send the literal string `<id>` in the URL. Replace it with the actual purchase identifier returned by the create request.

## Testing

```bash
mvn test
```

The test suite covers:

- cent-safe parsing and rounding behavior
- latest-rate selection on or before the purchase date
- the 6-calendar-month window rule
- `unsupported_currency` vs `no_rate_available`
- Treasury API paging and deduplication through a mocked transport
- HTTP request and response behavior via MockMvc

## Configuration

Environment variables:

- `APP_PORT` default `8080`
- `APP_DATA_DIR` default `data`
- `TREASURY_BASE_URL` default official Fiscal Data Treasury endpoint
- `TREASURY_REQUEST_TIMEOUT` default `20s`
- `TREASURY_REFRESH_INTERVAL` default `12h`

## Persistence model

The application uses two local JSON stores:

- `data/purchases.json` for purchase transactions
- `data/treasury-rates.json` for the cached Treasury exchange-rate history

Writes go through an atomic temp-file-and-rename flow so the persistence behavior is closer to durable storage than a simple in-memory demo.

## Treasury integration

The runtime flow is intentionally split in two:

- at startup the application attempts to refresh the local Treasury cache
- on a schedule the application refreshes the cache in the background
- during request handling the application reads only from the local cache

If a refresh fails, the service keeps running against the previously persisted cache rather than turning every conversion request into a live upstream dependency.

## Project structure

```text
src/main/java/com/wex/assessment
  config       application properties and cache lifecycle
  domain       core records
  error        typed application errors
  repository   file-backed persistence
  service      business rules
  support      money and currency helpers
  treasury     Treasury client and currency mapping
  web          controllers, DTOs, and JSON error handling
```

## Assumptions

- blank descriptions are treated as invalid even though the brief only explicitly calls out the maximum length
- JSON money values are returned as strings to preserve exact formatting and avoid client-side floating-point ambiguity
- `USD` is treated as a first-class target currency with an exchange rate of `1`
