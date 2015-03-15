package com.example.superheroes.driveguru.Processing;

import com.example.superheroes.driveguru.Game.GameController;

import io.relayr.model.AccelGyroscope;

/**
 * DriveGuru
 * Created by Cyril Fougeray on 14/03/15.
 * Copyright (c) Whisper 2015
 */
public class ProcessingManager extends Thread {

    private static final int FILTER_DEPTH = 5;
    private static final double MAX_STEERING_G = 0.3;
    private static final double MAX_ACCELERATION_G = 0.3;
    private static final double DANGEROUS_BRAKE_G = 0.3 ;
    private static final double VERY_DANGEROUS_BRAKE_G = 0.5;

    public static GameController gameController;

    private AccelCircularBuffer rawDataAccelGyroCircularBuffer;
 /*   private AccelCircularBuffer filteredDataAccelGyroCircularBuffer; */


    public ProcessingManager(){
        rawDataAccelGyroCircularBuffer = new AccelCircularBuffer(50);
//        filteredDataAccelGyroCircularBuffer = new AccelCircularBuffer(50);
        gameController = new GameController();
    }

    public void addData(AccelGyroscope.Acceleration acc){
        rawDataAccelGyroCircularBuffer.store(acc);

/*
        if(rawDataAccelGyroCircularBuffer.getDataToProcessNumber() > FILTER_DEPTH){
            int filteredAccelX = 0;
            int filteredAccelY = 0;
            int filteredAccelZ = 0;
            long timestamp = 0;
            for(int i=0; i < FILTER_DEPTH; i++){
                AccelGyroscope sample = rawDataAccelGyroCircularBuffer.read();
                filteredAccelX += sample.acc.x;
                filteredAccelY += sample.acc.y;
                filteredAccelZ += sample.acc.z;
                if(i == (FILTER_DEPTH-1))
                    timestamp = sample.ts;

            }
            filteredAccelX = filteredAccelX/FILTER_DEPTH;
            filteredAccelY = filteredAccelY/FILTER_DEPTH;
            filteredAccelZ = filteredAccelZ/FILTER_DEPTH;

            AccelGyroscope filteredValue = new AccelGyroscope();
            filteredValue.acc.x = filteredAccelX;
            filteredValue.acc.y = filteredAccelY;
            filteredValue.acc.z = filteredAccelZ;
            filteredValue.ts = timestamp;

            filteredDataAccelGyroCircularBuffer.store(filteredValue);
        }
*/

    }

/*    public AccelGyroscope getCurrent(){

    }*/


    public void run() {
        AccelGyroscope.Acceleration processBuffer[] = new AccelGyroscope.Acceleration[2];
        int i=0;

        /* Waiting until we have at least 2 samples to process */
        while (rawDataAccelGyroCircularBuffer.getDataToProcessNumber() < 2){};

        while((processBuffer[i%2] = rawDataAccelGyroCircularBuffer.read()) == null);
        // long previousTimestamp = processBuffer[i%2].ts;
        i++;

        long previousTimestamp = System.currentTimeMillis();

        while(true){
            do {
                processBuffer[i % 2] = rawDataAccelGyroCircularBuffer.read();
            } while(processBuffer[i%2] == null);


            try {
                float x1 = processBuffer[i % 2].x;
                float x2 = processBuffer[(i + 1) % 2].x;
            } catch (Exception e){
                e.printStackTrace();
            }


            float differenceLongitudinalAccel = processBuffer[i % 2].x - processBuffer[(i+1)%2].x;
            float differenceLateralAccel = processBuffer[i % 2].y - processBuffer[(i+1)%2].y;

            /* detect bad behaviour */

            if(differenceLateralAccel > MAX_STEERING_G || (-differenceLateralAccel) > MAX_STEERING_G){
                gameController.removePoints(3);
            }

            if(-differenceLongitudinalAccel > MAX_ACCELERATION_G){

            }

            if(differenceLongitudinalAccel > DANGEROUS_BRAKE_G){
                gameController.removePoints(3);

                if(differenceLongitudinalAccel > VERY_DANGEROUS_BRAKE_G){
                    gameController.removePoints(5);
                }
            }

            if((System.currentTimeMillis() - previousTimestamp) > 60000){
                gameController.addPoint();
                previousTimestamp = System.currentTimeMillis();
            }


            i++;
        }

    }



}
