package com.example.superheroes.driveguru.Processing;

import io.relayr.model.AccelGyroscope;

/**
 * DriveGuru
 * Created by Cyril Fougeray on 14/03/15.
 * Copyright (c) Whisper 2015
 */

class AccelGyroCircularBuffer {
    private AccelGyroscope data[];
    private int head;
    private int tail;
    private int distanceBetweenHeadAndTail;

    public AccelGyroCircularBuffer(int depth) {
        data = new AccelGyroscope[depth];
        head = 0;
        tail = 0;
        distanceBetweenHeadAndTail = 0;
    }

    public boolean store(AccelGyroscope value) {
        if (!bufferFull()) {
            data[tail++] = value;
            if (tail == data.length) {
                tail = 0;
            }
            distanceBetweenHeadAndTail++;
            return true;
        } else {
            return false;
        }
    }

    public AccelGyroscope read() {
        if (head != tail) {
            AccelGyroscope value = data[head++];
            if (head == data.length) {
                head = 0;
            }
            distanceBetweenHeadAndTail--;
            return value;
        } else {
            return null;
        }
    }

    private boolean bufferFull() {
        if (tail + 1 == head) {
            return true;
        }
        if (tail == (data.length - 1) && head == 0) {
            return true;
        }
        return false;
    }

    public int getDataToProcessNumber(){
        return distanceBetweenHeadAndTail;
    }

    AccelGyroscope[] getData(){
        return data;
    }
}