package client;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SoundManager {

    private Clip backgroundMusicClip;

    // Phương thức để phát hiệu ứng âm thanh một lần và tắt sau thời gian delay
    public void playSoundEffect(String filePath) {
        try {
            URL soundUrl = getClass().getResource(filePath);
            if (soundUrl == null) {
                System.err.println("Không tìm thấy file âm thanh: " + filePath);
                return;
            }
            
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundUrl);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();

            // *** ĐIỀU CHỈNH ĐỘ TRỄ Ở ĐÂY: 1000 miligiây = 1 giây ***
            int delay = 3000; 
            Timer timer = new Timer(delay, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (clip.isRunning()) {
                        clip.stop(); // Dừng clip
                    }
                    clip.close(); // Đóng clip để giải phóng tài nguyên
                    try {
                        audioInputStream.close(); // Đóng AudioInputStream
                    } catch (IOException ioException) {
                        System.err.println("Lỗi khi đóng luồng âm thanh: " + ioException.getMessage());
                    }
                    ((Timer)e.getSource()).stop(); // Dừng timer này
                }
            });
            timer.setRepeats(false); // Đảm bảo timer chỉ chạy một lần
            timer.start(); // Bắt đầu timer
            
            // Listener để đóng clip khi nó tự động dừng (nếu timer không kịp)
            // hoặc nếu file âm thanh ngắn hơn thời gian delay.
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    // Chỉ đóng nếu clip vẫn đang mở và timer chưa đóng nó
                    if (clip.isOpen()) { 
                        clip.close();
                        try {
                            audioInputStream.close();
                        } catch (IOException ioException) {
                            System.err.println("Lỗi khi đóng luồng âm thanh từ LineListener: " + ioException.getMessage());
                        }
                    }
                }
            });

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Lỗi khi phát hiệu ứng âm thanh " + filePath + ": " + e.getMessage());
        }
    }

    // Các phương thức khác (playBackgroundMusic, stopBackgroundMusic) không thay đổi
    public void playBackgroundMusic(String filePath) {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
        }
        try {
            URL soundUrl = getClass().getResource(filePath);
            if (soundUrl == null) {
                System.err.println("Không tìm thấy file nhạc nền: " + filePath);
                return;
            }
            System.err.println("Thanh Cong" + filePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundUrl);
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(audioInputStream);
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusicClip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Lỗi khi phát nhạc nền " + filePath + ": " + e.getMessage());
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
        }
    }
}