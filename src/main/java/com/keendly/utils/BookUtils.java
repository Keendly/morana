package com.keendly.utils;

import com.keendly.model.Book;

public class BookUtils {

    public static void setNumbers(Book book){
        for (int i = 0; i < book.sections.size(); i++){
            book.sections.get(i).setNumber(i);
            for (int j = 0; j < book.sections.get(i).articles.size(); j++){
                book.sections.get(i).articles.get(j).setNumber(j);
            }
        }
    }
}
