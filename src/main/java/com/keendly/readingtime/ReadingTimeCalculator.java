package com.keendly.readingtime;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class ReadingTimeCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(ReadingTimeCalculator.class);


    private static Map<String, Integer> counts = new HashMap();
    static {
        counts.put("ar", 138);
        counts.put("zh-CN", 158);
        counts.put("nl", 202);
        counts.put("en", 228);
        counts.put("fi", 161);
        counts.put("fr", 195);
        counts.put("de", 179);
        counts.put("he", 187);
        counts.put("it", 188);
        counts.put("ja", 193);
        counts.put("pl", 166);
        counts.put("pt", 181);
        counts.put("ru", 184);
        counts.put("sl", 180);
        counts.put("es", 218);
        counts.put("sv", 199);
        counts.put("tr", 166);
    }

    private LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;

    public ReadingTimeCalculator() {
        //load all languages:
//        List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
        List<LanguageProfile> languageProfiles = null;
        try {
            languageProfiles = new LanguageProfileReader().readBuiltIn(counts.entrySet().stream().map(
                e -> LdLocale.fromString(e.getKey())
            ).collect(Collectors.toList()));
        } catch (IOException e) {
            LOG.error("Error creating readime time calculator", e);
        }

        //build language detector:
        languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(languageProfiles)
            .build();

        //create a text object factory
        textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
    }

    public Integer getReadingTime(String content){
        if (languageDetector == null || textObjectFactory == null){
            // something went wrong during initialization
            return null;
        }
        TextObject textObject = textObjectFactory.forText(content);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        if (lang.isPresent()){
            if (counts.containsKey(lang.get().getLanguage())){
                Document document = Jsoup.parse(content);
                StringTokenizer tokenizer = new StringTokenizer(document.body().text());
                int words = tokenizer.countTokens();
                return words/counts.get(lang.get().getLanguage());
            }
        }
        return null;
    }
}
