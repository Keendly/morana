package com.keendly.utils;

import com.keendly.model.book.Book;

public class BookUtils {

    public static void setNumbers(Book book){
        for (int i = 0; i < book.getSections().size(); i++){
            book.getSections().get(i).setNumber(i);
            for (int j = 0; j < book.getSections().get(i).articles.size(); j++){
                book.getSections().get(i).articles.get(j).setNumber(j);
            }
        }
    }
}
