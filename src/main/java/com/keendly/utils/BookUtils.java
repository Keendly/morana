package com.keendly.utils;

import com.keendly.model.Book;

public class BookUtils {

    public static void setNumbers(Book book){
        for (int i = 0; i < book.getSections().size(); i++){
            book.getSections().get(i).setNumber(i);
            for (int j = 0; j < book.getSections().get(i).getArticles().size(); j++){
                book.getSections().get(i).getArticles().get(j).setNumber(j);
            }
        }
    }
}
