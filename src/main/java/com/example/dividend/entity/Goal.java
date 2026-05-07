package com.example.dividend.entity;

import jakarta.persistence.*;

@Entity
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int targetDividend;

    public Long getId() {
        return id;
    }

    public int getTargetDividend() {
        return targetDividend;
    }

    public void setTargetDividend(int targetDividend) {
        this.targetDividend = targetDividend;
    }
}