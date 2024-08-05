package dev.pan.snapper.data

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import dev.pan.snapper.domain.Classification
import dev.pan.snapper.domain.LandmarkClassifier
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class TfLiteLandmarkClassifier(
    private val context: Context,
    private val threshold: Float = 0.5f, // minimum value at which AI consider result as correct
    private val maxResults: Int = 1 // results of AI guesses
) : LandmarkClassifier {

    // class from lib for image recognition
    private var classifier: ImageClassifier? = null

    // setup for using TensorFlow model that we imported and adding needed options for it
    private fun setupClassifier(){
        val baseOptions = BaseOptions.builder()
            .setNumThreads(2)
            .build()
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .setScoreThreshold(threshold)
            .build()

        try {
            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                "europe.tflite",
                options
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // using created above classifier creating setup for image recognition and how to return result of it
    override fun classify(bitmap: Bitmap, rotation: Int): List<Classification> {
        if(classifier == null){
            setupClassifier()
        }

        val imageProcessor = ImageProcessor.Builder().build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = classifier?.classify(tensorImage, imageProcessingOptions)

        /* flatmap used because we can have multiple categories for one landmark, and to get rid of
        * list of lists for different category we flatmap it to single List as result*/
        return results?.flatMap { classifications ->
            classifications.categories.map { category ->
                Classification(
                    name = category.displayName,
                    score = category.score
                )
            }
        }?.distinctBy { it.name } ?: emptyList() // get rid of duplicates except one last entry
    }

    // dunno about this, needed for options setup
    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_0 -> ImageProcessingOptions.Orientation.RIGHT_TOP
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.LEFT_BOTTOM
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }
}