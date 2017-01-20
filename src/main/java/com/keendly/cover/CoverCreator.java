package com.keendly.cover;

import com.keendly.model.Book;
import com.keendly.model.Section;

import javax.imageio.ImageIO;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;



public class CoverCreator {

    public static final int SECTION_TITLE_MAX_LENGTH = 23;
    public static final int MAX_SECTIONS = 13;

    public void create(Book book, String coverFilePath) throws IOException {
        BufferedImage image = ImageIO.read(getClass().getClassLoader().getResource("templates/cover.jpg"));
        Graphics g = image.getGraphics();
        g.setFont(g.getFont().deriveFont(40f));
        g.setColor(Color.black);
        printSectionTitles(book, g);
        g.dispose();
        ImageIO.write(image, "jpg", new File(coverFilePath));
    }

    private void printSectionTitles(Book book, Graphics graphics){
        List<Section> sections = book.sections;
        int sectionsToShow = sections.size() <= MAX_SECTIONS ? sections.size() : MAX_SECTIONS - 1;
        for (int i = 0; i < sectionsToShow; i++){
            Section section = sections.get(i);
            graphics.drawString(sectionText(section), 100, 260 + (i * 60));
        }
        if (sections.size() > MAX_SECTIONS){
            printVerticalEllipsis(graphics);
        }
    }

    private void printVerticalEllipsis(Graphics graphics){
        graphics.drawString("\u22ee", 100, 260 + ((MAX_SECTIONS - 1) * 60));
    }

    private String sectionText(Section section){
        int size = section.articles != null ? section.articles.size() : 0;
        return getNormalizedTitle(section) + " (" + size + ")";
    }

    private String getNormalizedTitle(Section section){
        if (section.title.length() <= SECTION_TITLE_MAX_LENGTH){
            return section.title;
        } else {
            return section.title.substring(0, SECTION_TITLE_MAX_LENGTH) + "...";
        }
    }
}
