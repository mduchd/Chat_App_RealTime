package com.teamchat.swing.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;

/**
 * Service phát âm thanh thông báo cho ứng dụng ChatSpace.
 * Sử dụng âm tổng hợp (synthesized chime) 2 nốt nhẹ nhàng, trong trẻo,
 * không cần file âm thanh ngoài nên không lo lỗi thiếu file hay sai đường dẫn.
 */
public class SoundService {

    private static volatile boolean soundEnabled = true;

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    /**
     * Phát âm thanh thông báo tin nhắn mới đến
     */
    public static void playNotificationSound() {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                byte[] chime = createChimeAudio();
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format);
                    line.start();
                    line.write(chime, 0, chime.length);
                    line.drain();
                }
            } catch (Exception e) {
                try {
                    Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {}
            }
        }, "SoundNotificationThread").start();
    }

    /**
     * Tạo âm thanh chuông 2 nốt nhẹ nhàng (D5 -> A5)
     */
    private static byte[] createChimeAudio() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int sampleRate = 44100;
        // Nốt 1: 587.33 Hz (D5) 90ms
        appendTone(baos, sampleRate, 587.33, 0.09, 0.30);
        // Nốt 2: 880.00 Hz (A5) 140ms
        appendTone(baos, sampleRate, 880.0, 0.14, 0.40);
        return baos.toByteArray();
    }

    private static void appendTone(ByteArrayOutputStream out, int sampleRate, double freq, double duration, double volume) {
        int numSamples = (int) (duration * sampleRate);
        for (int i = 0; i < numSamples; i++) {
            double time = i / (double) sampleRate;
            // Áp dụng đường bao sine để fade-in & fade-out êm tai, tránh tiếng click/tách
            double envelope = Math.sin(Math.PI * i / numSamples);
            double sample = Math.sin(2 * Math.PI * freq * time) * envelope * volume;
            short pcm = (short) (sample * 32767);
            out.write((byte) (pcm & 0xFF));
            out.write((byte) ((pcm >> 8) & 0xFF));
        }
    }
}
