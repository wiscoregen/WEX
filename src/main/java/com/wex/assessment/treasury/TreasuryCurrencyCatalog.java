package com.wex.assessment.treasury;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class TreasuryCurrencyCatalog {

    private static final Map<String, String> DESCRIPTOR_ALIAS_TO_CODE = new HashMap<>();
    private static final Map<String, String> CURRENCY_ALIAS_TO_CODE = new HashMap<>();
    private static final Set<String> TREASURY_SUPPORTED_CODES = new TreeSet<>();

    static {
        register("AED", List.of("united arab emirates dirham", "uae dirham"), List.of());
        register("ARS", List.of("argentina peso"), List.of());
        register("AUD", List.of("australia dollar"), List.of());
        register("BBD", List.of("barbados dollar"), List.of());
        register("BDT", List.of("bangladesh taka"), List.of());
        register("BHD", List.of("bahrain dinar"), List.of());
        register("BMD", List.of("bermuda dollar"), List.of());
        register("BOB", List.of("bolivia boliviano"), List.of());
        register("BRL", List.of("brazil real"), List.of());
        register("BSD", List.of("bahamas dollar"), List.of());
        register("BWP", List.of("botswana pula"), List.of());
        register("CAD", List.of("canada dollar"), List.of());
        register("CHF", List.of("switzerland franc", "swiss franc"), List.of());
        register("CLP", List.of("chile peso"), List.of());
        register("CNY", List.of("china yuan", "china renminbi"), List.of("yuan", "renminbi"));
        register("COP", List.of("colombia peso"), List.of());
        register("CRC", List.of("costa rica colon", "costa rican colon"), List.of());
        register("CZK", List.of("czech republic koruna", "czechia koruna"), List.of());
        register("DKK", List.of("denmark krone"), List.of());
        register("DOP", List.of("dominican republic peso"), List.of());
        register("DZD", List.of("algeria dinar"), List.of());
        register("EGP", List.of("egypt pound"), List.of());
        register("EUR", List.of(), List.of("euro"));
        register("FJD", List.of("fiji dollar"), List.of());
        register("GBP", List.of("united kingdom pound sterling", "uk pound sterling", "britain pound sterling"), List.of("pound sterling"));
        register("GTQ", List.of("guatemala quetzal"), List.of());
        register("HKD", List.of("hong kong dollar"), List.of());
        register("HNL", List.of("honduras lempira"), List.of());
        register("HUF", List.of("hungary forint"), List.of("forint"));
        register("IDR", List.of("indonesia rupiah"), List.of());
        register("ILS", List.of("israel shekel", "israeli shekel"), List.of("shekel"));
        register("INR", List.of("india rupee"), List.of());
        register("ISK", List.of("iceland krona"), List.of());
        register("JMD", List.of("jamaica dollar"), List.of());
        register("JPY", List.of("japan yen"), List.of("yen"));
        register("KES", List.of("kenya shilling"), List.of());
        register("KRW", List.of("south korea won", "korea south won", "korea republic won"), List.of("won"));
        register("KWD", List.of("kuwait dinar"), List.of());
        register("KYD", List.of("cayman islands dollar"), List.of());
        register("KZT", List.of("kazakhstan tenge"), List.of());
        register("LKR", List.of("sri lanka rupee", "sri lankan rupee"), List.of());
        register("MAD", List.of("morocco dirham"), List.of());
        register("MUR", List.of("mauritius rupee"), List.of());
        register("MXN", List.of("mexico peso"), List.of());
        register("MYR", List.of("malaysia ringgit"), List.of("ringgit"));
        register("NAD", List.of("namibia dollar"), List.of());
        register("NGN", List.of("nigeria naira"), List.of());
        register("NOK", List.of("norway krone"), List.of());
        register("NPR", List.of("nepal rupee"), List.of());
        register("NZD", List.of("new zealand dollar"), List.of());
        register("OMR", List.of("oman rial"), List.of());
        register("PAB", List.of("panama balboa"), List.of());
        register("PEN", List.of("peru sol", "peru nuevo sol"), List.of());
        register("PHP", List.of("philippines peso", "philippine peso"), List.of());
        register("PKR", List.of("pakistan rupee"), List.of());
        register("PLN", List.of("poland zloty"), List.of());
        register("QAR", List.of("qatar riyal"), List.of());
        register("RON", List.of("romania leu"), List.of());
        register("RSD", List.of("serbia dinar"), List.of());
        register("RUB", List.of("russia ruble", "russian ruble"), List.of());
        register("SAR", List.of("saudi arabia riyal"), List.of());
        register("SEK", List.of("sweden krona"), List.of());
        register("SGD", List.of("singapore dollar"), List.of());
        register("THB", List.of("thailand baht"), List.of("baht"));
        register("TRY", List.of("turkey lira", "turkish lira"), List.of("lira"));
        register("TTD", List.of("trinidad tobago dollar", "trinidad and tobago dollar"), List.of());
        register("TWD", List.of("taiwan new dollar", "new taiwan dollar"), List.of());
        register("UAH", List.of("ukraine hryvnia"), List.of());
        register("UGX", List.of("uganda shilling"), List.of());
        register("UYU", List.of("uruguay peso"), List.of());
        register("VND", List.of("vietnam dong"), List.of("dong"));
        register("XAF", List.of(
                "cameroon cfa franc",
                "central african republic cfa franc",
                "chad cfa franc",
                "congo republic cfa franc",
                "equatorial guinea cfa franc",
                "gabon cfa franc"
        ), List.of());
        register("XCD", List.of(
                "antigua barbuda e caribbean dollar",
                "dominica e caribbean dollar",
                "grenada e caribbean dollar",
                "saint kitts nevis e caribbean dollar",
                "st kitts nevis e caribbean dollar",
                "saint lucia e caribbean dollar",
                "st lucia e caribbean dollar",
                "saint vincent grenadines e caribbean dollar",
                "st vincent grenadines e caribbean dollar"
        ), List.of());
        register("XOF", List.of(
                "benin cfa franc bceao",
                "burkina faso cfa franc bceao",
                "cote d ivoire cfa franc bceao",
                "ivory coast cfa franc bceao",
                "guinea bissau cfa franc bceao",
                "mali cfa franc bceao",
                "niger cfa franc bceao",
                "senegal cfa franc bceao",
                "togo cfa franc bceao"
        ), List.of());
        register("XPF", List.of(
                "french polynesia cfp franc",
                "new caledonia cfp franc",
                "wallis futuna cfp franc"
        ), List.of());
        register("ZAR", List.of("south africa rand"), List.of());
        register("ZMW", List.of("zambia kwacha"), List.of());
    }

    private TreasuryCurrencyCatalog() {
    }

    public static boolean isTreasurySupportedCurrency(String currencyCode) {
        if (currencyCode == null) {
            return false;
        }
        return "USD".equalsIgnoreCase(currencyCode) || TREASURY_SUPPORTED_CODES.contains(currencyCode.trim().toUpperCase(Locale.ROOT));
    }

    public static Optional<String> resolveCurrencyCode(String country, String currency, String descriptor) {
        String normalizedDescriptor = normalizeKey(descriptor);
        String normalizedCountryCurrency = normalizeKey("%s %s".formatted(nullToEmpty(country), nullToEmpty(currency)));
        String normalizedCurrency = normalizeKey(currency);

        if (DESCRIPTOR_ALIAS_TO_CODE.containsKey(normalizedDescriptor)) {
            return Optional.of(DESCRIPTOR_ALIAS_TO_CODE.get(normalizedDescriptor));
        }
        if (DESCRIPTOR_ALIAS_TO_CODE.containsKey(normalizedCountryCurrency)) {
            return Optional.of(DESCRIPTOR_ALIAS_TO_CODE.get(normalizedCountryCurrency));
        }
        if (CURRENCY_ALIAS_TO_CODE.containsKey(normalizedCurrency)) {
            return Optional.of(CURRENCY_ALIAS_TO_CODE.get(normalizedCurrency));
        }

        return Optional.empty();
    }

    private static void register(String currencyCode, List<String> descriptorAliases, List<String> currencyAliases) {
        TREASURY_SUPPORTED_CODES.add(currencyCode);

        for (String alias : descriptorAliases) {
            DESCRIPTOR_ALIAS_TO_CODE.put(normalizeKey(alias), currencyCode);
        }
        for (String alias : currencyAliases) {
            CURRENCY_ALIAS_TO_CODE.put(normalizeKey(alias), currencyCode);
        }
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        boolean lastWasSpace = true;

        for (char character : value.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                builder.append(character);
                lastWasSpace = false;
            } else if (!lastWasSpace) {
                builder.append(' ');
                lastWasSpace = true;
            }
        }

        return builder.toString().trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

