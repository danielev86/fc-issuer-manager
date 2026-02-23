package com.redcatdev86.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class Injury implements Serializable {

    private static final long serialVersionUID = 1503410810734686182L;

    private String playerName;
    private LocalDate recoveryDate;

    public Injury() {
        // Required for JSON deserialization
    }

    public Injury(String playerName, LocalDate recoveryDate) {
        this.playerName = playerName;
        this.recoveryDate = recoveryDate;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public LocalDate getRecoveryDate() {
        return recoveryDate;
    }

    public void setRecoveryDate(LocalDate recoveryDate) {
        this.recoveryDate = recoveryDate;
    }
}