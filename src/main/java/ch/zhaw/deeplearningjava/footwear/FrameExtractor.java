package ch.zhaw.deeplearningjava.footwear;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FrameExtractor {

    public List<Path> extractFrames(String videoFile) {
        List<Path> framePaths = new ArrayList<>();
        System.out.println("Attempting to open video file at: " + videoFile);
        VideoCapture cap = new VideoCapture(videoFile);
    
        if (!cap.isOpened()) {
            System.err.println("Error opening video stream or file at: " + videoFile);
            return framePaths;
        }
    
        Mat frame = new Mat();
        int frameCount = 0;
        double frameRate = cap.get(Videoio.CAP_PROP_FPS);
        int frameInterval = (int) (frameRate * 2); // Calculating the interval for a frame every 2 seconds
        String videoName = new File(videoFile).getName();
        videoName = videoName.substring(0, videoName.lastIndexOf('.'));
        String outputDirectory = videoName + "_frames";
        File dir = new File(outputDirectory);
        boolean isDirCreated = dir.mkdirs();
        if (!isDirCreated && !dir.exists()) {
            System.err.println("Could not create directory for frames at: " + outputDirectory);
            return framePaths;
        }
    
        while (true) {
            boolean ret = cap.read(frame);
            if (!ret) {
                System.out.println("No more frames to read or failed to read a frame.");
                break;
            }
            if (frameCount % frameInterval == 0) { // Save the frame only at the specified interval
                String outputFilePath = outputDirectory + File.separator + "frame_" + frameCount + ".jpg";
                boolean isSaved = Imgcodecs.imwrite(outputFilePath, frame);
                if (isSaved) {
                    System.out.println("Frame " + frameCount + " has been extracted and saved as " + outputFilePath);
                    framePaths.add(Paths.get(outputFilePath));
                } else {
                    System.err.println("Failed to save frame at: " + outputFilePath);
                }
            }
            frameCount++;
        }
        cap.release();
        System.err.println("Frame Extraction finished with frames extracted every 2 seconds. " + framePaths.size());
        
        return framePaths;
    }
}
