package com.keendly.lang;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class LanguageDetector {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageDetector.class);
    private static final LanguageDetector INSTANCE = new LanguageDetector();


    private com.optimaize.langdetect.LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;

    public static LanguageDetector instance(){
        return INSTANCE;
    }

    public LanguageDetector(){
        init();
    }

    public void init(){
        List<LanguageProfile> languageProfiles = null;
        try {
            languageProfiles = new LanguageProfileReader().readAllBuiltIn();
        } catch (IOException e) {
            LOG.error("Error creating readime time calculator", e);
        }

        languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(languageProfiles)
            .build();

        //create a text object factory
        textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
    }

    public Optional<LdLocale> detect(String content){
        if (languageDetector == null || textObjectFactory == null || content == null){
            // something went wrong during initialization
            return Optional.absent();
        }
        TextObject textObject = textObjectFactory.forText(content);
        return languageDetector.detect(textObject);
    }
}
