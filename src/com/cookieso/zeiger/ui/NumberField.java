package com.cookieso.zeiger.ui;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public class NumberField extends TextField {
    public NumberField(String text) {
        super();
        this.setText(text);
        this.addEventFilter(KeyEvent.KEY_TYPED, t -> {
            char[] ar = t.getCharacter().toCharArray();
            char ch = ar[t.getCharacter().toCharArray().length - 1];
            if (!(ch >= '0' && ch <= '9' || ch == '-' || ch == '.')) {
                t.consume();
            }
        });
    }
}
