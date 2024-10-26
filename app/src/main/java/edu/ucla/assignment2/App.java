package edu.ucla.assignment2;

import java.util.ArrayDeque;
import java.util.Deque;

import processing.core.PApplet;
import processing.core.PImage;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;

public class App extends PApplet {
    FFT fft;
    AudioIn in;
    PImage spectroImage;

    int bands = 512, bufferSize = 400;
    float[] spectrum = new float[bands];
    float minIntensityLog = Float.POSITIVE_INFINITY, maxIntensityLog = Float.NEGATIVE_INFINITY;
    Deque<float[]> buffer = new ArrayDeque<>();
    {
        for (int i = 0; i < bufferSize; i++) {
            buffer.add(new float[bands]);
        }
    }

    public static void main(String[] args) {
        PApplet.main("edu.ucla.assignment2.App");
    }

    @Override
    public void settings() {
        size(512, 400);
    }

    @Override
    public void setup() {
        spectroImage = createImage(width, height, RGB);

        background(0);
        textSize(20);
        textAlign(CENTER);

        // List all audio devices
        Sound.list();
        Sound.inputDevice(14);

        // Create an Input stream which is routed into the FFT analyzer
        fft = new FFT(this, bands);
        in = new AudioIn(this, 0);

        // start the Audio Input
        in.start();

        // patch the AudioIn
        fft.input(in);
    }

    @Override
    public void draw() {
        background(0);

        fft.analyze(spectrum);
        buffer.addLast(spectrum.clone());
        buffer.removeFirst();

        // update pixels of the spectrogram image
        spectroImage.loadPixels();
        scrollImage();
        findLogIntensityExtrema();
        waterfallPlot();
        spectroImage.updatePixels();

        // plot the spectrogram
        image(spectroImage, 0, 0);
    }

    void scrollImage() {
        for (int y = height - 1; y > 0; y--) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                int loc = x + offset;
                int locRowAbove = x + offset - width;
                spectroImage.pixels[loc] = spectroImage.pixels[locRowAbove];
            }
        }
    }

    void findLogIntensityExtrema() {
        float refValue = buffer.getLast()[0] + 1;
        float minIntensity = refValue, maxIntensity = refValue;
        for (float[] array : buffer) {
            for (float value : array) {
                value = value + 1;
                if (value < minIntensity) {
                    minIntensity = value;
                } else if (value > maxIntensity) {
                    maxIntensity = value;
                }
            }
        }
        minIntensityLog = log(minIntensity) / log(2);
        maxIntensityLog = log(maxIntensity) / log(2);
    }

    void waterfallPlot() {
        for (int i = 0; i < bands; i++) {
            float xLog = log(i + 1) / log(2);
            float nextXLog = log(i + 2) / log(2);

            int x = Math.round(map(xLog, 0, log(512) / log(2), 0, width - 1));
            int xNext = Math.round(map(nextXLog, 0, log(512) / log(2), 0, width - 1));

            float logMagnitude = log(1 + spectrum[i]) / log(2);
            int colorValue = turboColor(logMagnitude, minIntensityLog, maxIntensityLog);
            for (int j = x; j < xNext; j++) {
                spectroImage.pixels[j] = colorValue;
            }
        }
    }

    int turboColor(float value, float min, float max) {
        // Clamp value between 0 and 1
        value = map(value, min, max, 0, 1);
        int grayScale = (int) (255 * value);

        // Turbo colormap interpolation
        float red = (float) TurboColorMap.turboData[grayScale][0];
        float green = (float) TurboColorMap.turboData[grayScale][1];
        float blue = (float) TurboColorMap.turboData[grayScale][2];

        return color(red * 255, green * 255, blue * 255);
    }
}