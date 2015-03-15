package com.example.superheroes.driveguru.Game;

/**
 * DriveGuru
 * Created by Cyril Fougeray on 14/03/15.
 * Copyright (c) Whisper 2015
 */
public class GameController {

    private int score;

    public GameController(){
        score = 0;
    }


    public void addPoint(){
        score++;
    }

    public void removePoints(int number){
        score = score - number;
    }

    public int getScore()
    {
        return score;
    }
}
