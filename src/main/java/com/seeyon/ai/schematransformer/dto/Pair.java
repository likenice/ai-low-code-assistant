package com.seeyon.ai.schematransformer.dto;

public class Pair<L, R> {
    private L key;
    private R value;

    public Pair(L left, R right) {
        this.key = left;
        this.value = right;
    }

    public L getKey() {
        return key;
    }

    public void setKey(L key) {
        this.key = key;
    }

    public R getValue() {
        return value;
    }

    public void setValue(R value) {
        this.value = value;
    }
}

class Main {
    public static void main(String[] args) {
        Pair<String, Integer> pair = new Pair<>("LeftValue", 100);

        // 操作 Pair 对象
        System.out.println("Left: " + pair.getKey());
        System.out.println("Right: " + pair.getValue());

        pair.setKey("NewLeftValue");
        pair.setValue(200);

        System.out.println("Updated Left: " + pair.getKey());
        System.out.println("Updated Right: " + pair.getValue());
    }
}
