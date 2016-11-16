package com.keendly.template;

import com.keendly.model.Article;
import com.keendly.model.Book;
import com.keendly.model.Section;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Processor {

    private static final String TEMPLATES_DIR = "templates/";

    private static TemplateEngine xmlEngine = new TemplateEngine();
    private static TemplateEngine htmlEngine = new TemplateEngine();
    static {
        xmlEngine.setTemplateResolver(getResolver("XML"));
        htmlEngine.setTemplateResolver(getResolver("XHTML"));
    }

    private static ClassLoaderTemplateResolver getResolver(String mode){
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(mode);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setPrefix(TEMPLATES_DIR);
        return templateResolver;
    }

    public String cover(Book book){
        return xmlEngine.process("cover.html", bookToContext(book));
    }

    public String details(Book book){
        return xmlEngine.process("keendly.opf", bookToContext(book));
    }

    public String contentsHTML(Book book){
        return htmlEngine.process("contents.html", bookToContext(book));
    }

    public String contentsNCX(Book book){
        return xmlEngine.process("nav-contents.ncx", bookToContext(book));
    }

    private Context bookToContext(Book book){
        Context context = new Context();
        context.setVariable("title", book.getTitle());
        context.setVariable("language", book.getLanguage());
        context.setVariable("creator", book.getCreator());
        context.setVariable("publisher", book.getPublisher());
        context.setVariable("subject", book.getSubject());
        context.setVariable("date", book.getDate());
        context.setVariable("description", book.getDescription());
        context.setVariable("sections", book.getSections());
        return context;
    }

    public String section(Section section){
        Context context = new Context();
        context.setVariable("title", section.getTitle());
        return htmlEngine.process("section.html", context);
    }

    public String article(Article article){
        Context context = new Context();
        context.setVariable("title", article.getTitle());
        context.setVariable("author", article.getAuthor());
        context.setVariable("content", article.getContent());
        context.setVariable("date", article.getDate());
        context.setVariable("actions", article.getActions());
        return htmlEngine.process("article.html", context);
    }

}
