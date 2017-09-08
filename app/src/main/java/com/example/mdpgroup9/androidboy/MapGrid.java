package com.example.mdpgroup9.androidboy;

import android.graphics.Color;

public class MapGrid {
    private int posX;
    private int posY;
    private String txt;
    private int bg;

    public MapGrid(int posX, int posY, String txt){
        //Default start configurations
        this.posX = posX;
        this.posY = posY;
        this.txt = txt;
        //Red
        this.bg = Color.parseColor("#ff0000");
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public int getBg() {
        return bg;
    }

    public void setBg(int bg) {
        this.bg = bg;
    }
}
