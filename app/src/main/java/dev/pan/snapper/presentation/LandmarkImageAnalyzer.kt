package dev.pan.snapper.presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import dev.pan.snapper.domain.Classification
import dev.pan.snapper.domain.LandmarkClassifier

class LandmarkImageAnalyzer(
    private val classifier: LandmarkClassifier,
    private val onResults: (List<Classification>) -> Unit // just for updates when value changes
) : ImageAnalysis.Analyzer{

    // by default it analyze each frame, but that's too fast for this app and we will be fine with analyzing each second
    private var frameSkipCounter = 0

    /*usually model have expected values that you need to feed to it (in this case it's listed on their website below model description)
    * so we need to get those values to it, such as image must be some expected size (square here)*/
    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 60 == 0){
            val rotationDegrees = image.imageInfo.rotationDegrees
            val bitmap = image
                .toBitmap()
                .centerCrop(321, 321)

            val results = classifier.classify(bitmap, rotationDegrees)
            onResults(results)
        }
        frameSkipCounter++


        image.close()
    }
}