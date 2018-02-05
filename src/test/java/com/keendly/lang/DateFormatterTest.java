package com.keendly.lang;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.google.common.base.Optional;
import com.optimaize.langdetect.i18n.LdLocale;
import org.junit.Test;

public class DateFormatterTest {

    private LanguageDetector languageDetector = mock(LanguageDetector.class);
    private DateFormatter dateFormatter = new DateFormatter(languageDetector);

    private long TIMESTAMP = 1494369402358L;

    @Test
    public void given_cantDetectContentLanguage_when_formatDate_then_useDefault(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.absent());

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "GMT");

        // then
        assertEquals("09/05/17 22:36", formatted);
    }

    @Test
    public void given_noLocalesForLanguage_when_formatDate_then_useDefault(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.of(LdLocale.fromString("ur")));

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "GMT");

        // then
        assertEquals("09/05/17 22:36", formatted);
    }

    @Test
    public void given_oneLocaleForLanguage_when_formatDate_then_useIt(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.of(LdLocale.fromString("pl-PL")));

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "GMT");

        // then
        assertEquals("09.05.17 22:36", formatted);
    }

    @Test
    public void given_multipleLocalesWithSameFormat_when_formatDate_then_useAny(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.of(LdLocale.fromString("de")));

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "GMT");

        // then
        assertEquals("09.05.17 22:36", formatted);
    }

    @Test
    public void given_timezoneCountryMatchesOneLocale_when_formatDate_then_useThat(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.of(LdLocale.fromString("nl")));

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "Europe/Amsterdam");

        // then
        assertEquals("10-5-17 0:36", formatted);
    }

    @Test
    public void given_timezoneCountryDoesNotMatchAnyLocale_when_formatDate_then_useNoCountryLocale(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.of(LdLocale.fromString("zh")));

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "Europe/Warsaw");

        // then
        assertEquals("2017/5/10 上午 12:36", formatted);
    }

    @Test
    public void given_cantFindTimezoneCountry_when_formatDate_then_useNoCountryLocale(){
        // given
        when(languageDetector.detect(any())).thenReturn(Optional.of(LdLocale.fromString("ja")));

        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "blabla", "CET");

        // then
        assertEquals("西暦2017.05.10 0:36", formatted);
    }

    @Test
    public void given_timezoneCountryNotFound_when_formatDate_then_useDefault() {
        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "CET");

        // then
        assertEquals("10-May-2017", formatted);
    }

    @Test
    public void given_timezoneCountryFound_when_formatDate_then_useLocalisedFormatter() {
        // when
        String formatted = dateFormatter.formatDate(TIMESTAMP, "Europe/Warsaw");

        // then
        assertEquals("2017-05-10", formatted);
    }

//    @Test
//    public void test(){
//        String text = "A skąd się wziął taki Carvajal w Realu?\n"
//            + "Tak jak piszesz - jak wracał do mierdy to przez sezon BO z niego szydziło i wytykało, że najsłabsze ogniwo. Ale postawili na młodego chłopaka, bo nie było za bardzo w czym wybierać i zbierają plony.\n"
//            + "\n"
//            + "Ja w razie przyjścia Bellerina płakać nie będę na pewno. W jego aktualnym wieku do mierdy wracał właśnie nieopierzony Carvajal, a Dani Alves to zaczynał karierę w Sevilli i nikomu do głowy nie przychodziło wówczas, jakim może być przechujem. Ciężko znaleźć na RB gościa z potencjałem Hectora, w przybliżonym wieku i już z umiejętnościami, które pozwalałyby na grę w Barcelonie.\n"
//            + "\n"
//            + "Wiele osób tutaj wyśmiewa i jedzie po Bellerinie i równocześnie pisze, że trzeba kupić 'jakiegoś RB z topu', tylko jakoś z tymi propozycjami jest ciężko. Wystarczy popatrzeć na prawych obrońców najlepszych drużyn na świecie, żeby się zorientować, że jest kłopot. W tych okolicznościach więc zupełnie nie miałbym problemu z zaakceptowaniem HB. ";
//
//        String date = DateFormatter.formatDate(DateTime.now().getMillis(), text, "Europe/Warsaw");
//
//        String a = "a";
//    }
//
//    @Test
//    public void test_fr(){
//        String text = "Le français est une langue indo-européenne de la famille des langues romanes. Le français s'est formé en France (variété de la « langue d’oïl », qui est la langue de la partie septentrionale du pays) et est aujourd'hui parlé sur tous les continents par environ 274 millions de personnes1,5 dont 212 millions l'utilisent quotidiennement, et 76 millions2 à 77 millions3 en sont des locuteurs natifs. En 2014, 77 millions d'élèves et étudiants s'instruisent en français dans le monde6";
//
//        String date = DateFormatter.formatDate(DateTime.now().getMillis(), text, "Europe/Paris");
//
//        String a = "a";
//    }
}
