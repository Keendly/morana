package com.keendly.model.book;

import lombok.Setter;

public abstract class Numbered {

    @Setter
    private Integer number;

    public String getHref(){
        if (number == null){
            return null;
        }
        return String.format("%3s", Integer.toString(number)).replace(' ', '0');
    }
}
