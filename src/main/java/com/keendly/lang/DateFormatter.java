package com.keendly.lang;

import com.amazonaws.util.StringUtils;
import com.google.common.base.Optional;
import com.optimaize.langdetect.i18n.LdLocale;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateFormatter {

    private static DateTimeFormatter DEFAULT_FORMAT =
        DateTimeFormat.shortDateTime().withLocale(Locale.forLanguageTag("en-GB"));

    private static Map<String, String> countriesTimezones = mapTimezonesToCountries();

    private static Map<String, List<Locale>> languagesLocales = mapLanguageToLocales();

    private LanguageDetector languageDetector;

    public DateFormatter(){
        this(LanguageDetector.instance());
    }

    public DateFormatter(LanguageDetector languageDetector) {
        this.languageDetector = languageDetector;
    }

    public String formatDate(Long timestamp, String content, String timezone){
        if (timestamp == null){
            return null;
        }

        DateTimeFormatter formatter = defaultFormatter();

        Optional<LdLocale> lang = languageDetector.detect(content);
        if (lang.isPresent()){
            List<Locale> locales = languagesLocales.get(lang.get().getLanguage());
            if (locales == null || locales.isEmpty()){
               return  formatter.print(new DateTime(timestamp, DateTimeZone.forID(timezone)));
            }
            if (locales.size() == 1){
                // awsome use it
                formatter = formatter.withLocale(locales.get(0));
            } else {
                List<Locale> withCountries = getLocalesWithCountries(locales);
                List<Locale> withoutCountries = getLocalesWithoutCountries(locales);
                if (withCountries.isEmpty()) {
                    formatter = formatter.withLocale(withCountries.get(0));
                } else if (locales.size() == 1) {
                    formatter = formatter.withLocale(locales.get(0));
                } else if (locales.size() > 1) {
                    // format with all locales and see if the result is different
                    if (formatEqually(timestamp, locales)){
                        formatter = formatter.withLocale(locales.get(0));
                    } else {
                        // found same language in different locales, will try timezone
                        String timeZoneCountry = countriesTimezones.get(timezone);
                        if (timeZoneCountry != null){
                            Optional<Locale> l = getLocale(lang.get().getLanguage(), timeZoneCountry);
                            if (l.isPresent()){
                                formatter = formatter.withLocale(l.get());
                            } else {
                                if (!withoutCountries.isEmpty()){
                                    formatter = formatter.withLocale(withCountries.get(0));
                                }
                            }
                        } else {
                            if (!withoutCountries.isEmpty()){
                                formatter = formatter.withLocale(withCountries.get(0));
                            }
                        }
                    }
                }
            }
        }

        return formatter.print(new DateTime(timestamp, DateTimeZone.forID(timezone)));
    }

    private Optional<Locale> getLocale(String language, String timeZoneCountry) {

        Locale l = Locale.forLanguageTag(language + "-" + timeZoneCountry);
        for (Locale locale : Locale.getAvailableLocales()){
            if (locale.getDisplayName().equals(l.getDisplayName())){
                return Optional.of(l);
            }
        }
        return Optional.absent();
    }

    public static DateTimeFormatter defaultFormatter(){
        return DateTimeFormat.shortDateTime().withLocale(Locale.forLanguageTag("en-GB"));
    }

    public static Map<String, List<Locale>> mapLanguageToLocales() {
        Map<String, List<Locale>> languageToLocales = new HashMap<>();

        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales){
            String language = locale.getLanguage();
            if (languageToLocales.containsKey(language)){
                languageToLocales.get(language).add(locale);
            } else {
                List<Locale> l = new ArrayList<>();
                l.add(locale);
                languageToLocales.put(language, l);
            }
        }
        return languageToLocales;
    }

    public static Map<String, String> mapTimezonesToCountries() {
        Map<String, String> timezoneToCountry = new HashMap<>();

        String[] locales = Locale.getISOCountries();

        for (String countryCode : locales) {
            for (String id : com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode))
            {
                // Add timezone to result map

                timezoneToCountry.put(id, countryCode);
            }

        }
        return timezoneToCountry;
    }

    private List<Locale> getLocalesWithCountries(List<Locale> locales){
        List<Locale> withCountries = new ArrayList<>();
        for (Locale locale : locales){
            if (!StringUtils.isNullOrEmpty(locale.getCountry())){
                withCountries.add(locale);
            }
        }
        return withCountries;
    }

    private List<Locale> getLocalesWithoutCountries(List<Locale> locales){
        List<Locale> withoutCountries = new ArrayList<>();
        for (Locale locale : locales){
            if (StringUtils.isNullOrEmpty(locale.getCountry())){
                withoutCountries.add(locale);
            }
        }
        return withoutCountries;
    }

    private boolean formatEqually(Long timestamp, List<Locale> locales){
        String result = DEFAULT_FORMAT.withLocale(locales.get(0)).print(timestamp);
        for (Locale locale : locales){
            if (!result.equals(DEFAULT_FORMAT.withLocale(locale).print(timestamp))){
                return false;
            }
        }
        return true;
    }
}
