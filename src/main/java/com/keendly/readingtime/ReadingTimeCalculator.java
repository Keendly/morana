package com.keendly.readingtime;

import com.google.common.base.Optional;
import com.keendly.lang.LanguageDetector;
import com.optimaize.langdetect.i18n.LdLocale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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

    public Integer getReadingTime(String content){
        Optional<LdLocale> lang = LanguageDetector.instance().detect(content);
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
