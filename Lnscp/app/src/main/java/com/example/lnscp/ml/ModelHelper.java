package com.example.lnscp.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ModelHelper {
    private static final String TAG = "ModelHelper";
    private static final String MODEL_FILE = "monument_model.tflite";
    private static final int IMAGE_SIZE = 224;
    private static final float PROBABILITY_THRESHOLD = 0.2f;

    private final Interpreter interpreter;
    private final ImageProcessor imageProcessor;

    public ModelHelper(Context context) throws IOException {
        try {
            // Load the TFLite model
            ByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(modelBuffer, options);

            // Create image processor to match Python preprocessing
            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(0.0f, 255.0f)) // This will scale to [0,1] like in Python
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing model: " + e.getMessage());
            throw e;
        }
    }

    public String classifyImage(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                Log.e(TAG, "Bitmap is null");
                return "Error: No image provided";
            }

            // Convert bitmap to TensorImage
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // Create output tensor
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(
                    interpreter.getOutputTensor(0).shape(),
                    interpreter.getOutputTensor(0).dataType()
            );

            // Run inference
            interpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer());

            // Get the results
            float[] probabilities = outputBuffer.getFloatArray();
            int maxIndex = getMaxIndex(probabilities);
            float confidence = probabilities[maxIndex];

            Log.d(TAG, "Classification confidence: " + confidence);

            // Return the monument name if probability is above threshold
            if (confidence > PROBABILITY_THRESHOLD) {
                String monumentName = getMonumentName(maxIndex);
                Log.d(TAG, "Recognized monument: " + monumentName);
                return monumentName;
            } else {
                Log.d(TAG, "Confidence too low: " + confidence);
                return "Unknown Monument";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during classification: " + e.getMessage());
            return "Error processing image";
        }
    }

    private int getMaxIndex(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private String getMonumentName(int index) {
        // Map model output indices to monument names based on Python code
        String[] monumentNames = {
            "Agra Fort",
            "Agrasen Ki Baoli",
            "Gurudwara Bangla Sahib",
            "Humayu Tomb",
            "India Gate",
            "Isa Khan Niyazi's Tomb",
            "Itmad-Ud-Daulah's Tomb",
            "Jama Mashjid",
            "Jantar Mantar",
            "Lotus Temple",
            "Mutiny Memorial",
            "Qila-i-Kuhna Mosque",
            "Qutub Minar",
            "Quwwat ul-Islam Mosque",
            "Rashtrapati Bhavan",
            "Red Fort",
            "Taj Mahal"
        };

        if (index >= 0 && index < monumentNames.length) {
            return monumentNames[index];
        }
        return "Unknown Monument";
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
} 