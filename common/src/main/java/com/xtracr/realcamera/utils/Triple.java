package com.xtracr.realcamera.utils;

public class Triple<L, M, R> {

    private L left;
    private M middle;
    private R right;

    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public L getLeft() {
        return this.left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public M getMiddle() {
        return this.middle;
    }

    public void setMiddle(M middle) {
        this.middle = middle;
    }

    public R getRight() {
        return this.right;
    }

    public void setRight(R right) {
        this.right = right;
    }
}