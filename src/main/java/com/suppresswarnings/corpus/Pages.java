package com.suppresswarnings.corpus;

import java.util.List;

public class Pages<T> {
    String current = null;
    List<T> data;
    int size = 10;
    int index = 0;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getDataSize() {
        return data == null ? 0 : data.size();
    }
    public boolean isEnd() {
        return size > getDataSize();
    }

    public Pages update(List<T> data, String lastKey) {
        this.data = data;
        if(data.size() > 0) {
            this.current = lastKey;
            this.index += 1;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Pages{index="+getIndex()+" current=" + getCurrent() + " pageSize=" + getSize() + " dataSize=" + getDataSize() + " isEnd=" + isEnd() + "}";
    }
}
