package com.jindle.template;

import com.jindle.model.Article;
import com.jindle.model.Book;
import com.jindle.model.Section;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Processor {

    private static final String TEMPLATES_DIR = "templates/";

    private TemplateEngine xmlEngine;
    private TemplateEngine htmlEngine;

    public Processor(){
        xmlEngine = new TemplateEngine();
        xmlEngine.setTemplateResolver(getResolver("XML"));
        htmlEngine = new TemplateEngine();
        htmlEngine.setTemplateResolver(getResolver("XHTML"));
    }

    private ClassLoaderTemplateResolver getResolver(String mode){
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(mode);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setPrefix(TEMPLATES_DIR);
        return templateResolver;
    }

    public String details(Book book){
        return xmlEngine.process("jindle.opf", bookToContext(book));
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
        return htmlEngine.process("article.html", context);
    }

}
