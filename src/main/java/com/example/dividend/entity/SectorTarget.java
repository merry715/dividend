package com.example.dividend.entity;

import jakarta.persistence.*;

@Entity
public class SectorTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String sectorName;

    private int targetRate;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public int getTargetRate() {
        return targetRate;
    }

    public void setTargetRate(int targetRate) {
        this.targetRate = targetRate;
    }
}
