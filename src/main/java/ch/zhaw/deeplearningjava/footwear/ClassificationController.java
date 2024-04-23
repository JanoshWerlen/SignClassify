package ch.zhaw.deeplearningjava.footwear;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;



@RestController
public class ClassificationController {

    static {
        try {
            String opencvPath = "E:/VS_Code/ZHAW/Model_Deployment/JavaSpringboot/djl-footwear_classification/opencv/build/java/x64/opencv_java490.dll";
            System.load(opencvPath);
            System.out.println("Loaded OpenCV library from: " + opencvPath);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV library from specified path.");
            e.printStackTrace();
            System.exit(1);
        }
    }
    

    private static final String TEMP_DIR = "E:\\VS_Code\\ZHAW\\Model_Deployment\\JavaSpringboot\\djl-footwear_classification\\tempVideos";
    private static final String HIGH_PROB_DIR = "E:\\VS_Code\\ZHAW\\Model_Deployment\\JavaSpringboot\\djl-footwear_classification\\HighProb_Frames";
    private static final double YES_PROBABILITY_THRESHOLD = 0.85; // 80% probability
    private Inference inference = new Inference();
    private FrameExtractor extractor = new FrameExtractor();
    
    @GetMapping("/ping")
    public String ping() {
        return "Classification app is up and running!";
    }

    @PostMapping(path = "/analyze")
    public String predict(@RequestParam("image") MultipartFile image) throws Exception {
        System.out.println(image);
        return inference.predict(image.getBytes()).toJson();
    }

    @PostMapping("/upload_video")
    public String handleFileUpload(@RequestParam("video") MultipartFile file) {
        if (!file.isEmpty()) {
            Path tempFile = null;
            try {
                System.out.println("Video not empty " + file.getSize());
                Path tempDirPath = Paths.get(TEMP_DIR);
                Files.createDirectories(tempDirPath);
    
                tempFile = Files.createTempFile(tempDirPath, null, ".mp4");
                file.transferTo(tempFile);
                System.out.println("Tempfile saved at: " + tempFile);
    
                List<Path> frames = extractor.extractFrames(tempFile.toString());
                JSONArray resultsForHighProbYes = new JSONArray();
    
                Path highProbYesDir = Paths.get(HIGH_PROB_DIR);
                Files.createDirectories(highProbYesDir);
                System.out.println("FrameCount: "+ frames.size());
                int count = 0;    
                for (Path framePath : frames) {
                    
                    byte[] imageData = Files.readAllBytes(framePath);
                    String jsonResult = inference.predict(imageData).toJson();
                    JSONArray results = new JSONArray(jsonResult);
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        if ("Yes".equals(result.getString("className")) && result.getDouble("probability") > YES_PROBABILITY_THRESHOLD) {
                            Path targetPath = highProbYesDir.resolve(framePath.getFileName());
                            Files.copy(framePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            resultsForHighProbYes.put(new JSONObject()
                                .put("frame", framePath.getFileName().toString())
                                .put("probability", result.getDouble("probability")));
                        count++;
                        System.out.println(count);
                        }
                    }
                }
    
                // After processing is complete, delete the temporary video file
                if (tempFile != null && Files.exists(tempFile)) {
                    Files.delete(tempFile);
                    System.out.println("Temporary video file deleted successfully.");
                }
                System.out.println(resultsForHighProbYes.length());
                return resultsForHighProbYes.toString();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    // Attempt to delete the temporary file in case of an error during processing
                    if (tempFile != null && Files.exists(tempFile)) {
                        Files.delete(tempFile);
                        System.out.println("Temporary video file deleted after an error.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        } else {
            return "{\"status\": \"file is empty\"}";
        }
    }
}