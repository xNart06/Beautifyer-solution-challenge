package com.example.mytestapp3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.OpenCVLoader
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfRect
import org.opencv.core.MatOfRect2d
import org.opencv.core.Rect
import org.opencv.core.Rect2d
import org.opencv.dnn.Dnn
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File

data class BoxWithText(val box: android.graphics.Rect, val text: String)

class PostProcessingHelper(
    var imageView: ImageView,
    var textView: TextView,
    val context: Context
){
    fun postProcessing(outputFeature0: TensorBuffer,
                       outputFeature1 :TensorBuffer,
                       rH:Float,
                       rW:Float,
                       data: List<List<String>>,
                       bitmap: Bitmap){

        var score = convertToMultiDimArray(outputFeature0)
        var geo = convertToMultiDimArray(outputFeature1)

        var reScore=transposeArray(score)
        var reGeo=transposeArray(geo)


        var numRows = reScore[0][0].size
        var numCols = reScore[0][0][0].size

        val rects = mutableListOf<Rect>()
        val confidences = mutableListOf<Float>()

        score=reScore
        geo=reGeo

        for (y in 0 until numRows) {
            val scoresData = score[0][0][y]
            val xData0 = geo[0][0][y]
            val xData1 = geo[0][1][y]
            val xData2 = geo[0][2][y]
            val xData3 = geo[0][3][y]
            val anglesData = geo[0][4][y]

            for (x in 0 until numCols) {
                if (scoresData[x] < 0.3) {
                    continue
                }

                val offSetX = x * 4.0
                val offSetY = y * 4.0

                val angle = anglesData[x]
                val sin = kotlin.math.sin(angle)
                val cos = kotlin.math.cos(angle)

                val h = xData0[x] + xData2[x]
                val w = xData1[x] + xData3[x]

                val endX = (offSetX + cos * xData1[x] + sin * xData2[x]).toInt()
                val endY = (offSetY - sin * xData1[x] + cos * xData2[x]).toInt()

                val startX = (endX - w).toInt()
                val startY = (endY - h).toInt()

                rects.add(Rect(startX, startY, endX, endY))
                confidences.add(scoresData[x])
            }
        }
        OpenCVLoader.initDebug()

        val boundingBoxesMat = MatOfRect()
        val confidencesMat = MatOfFloat()
        val indicesMat = MatOfInt()

        boundingBoxesMat.fromList(rects)
        confidencesMat.fromList(confidences)

        Dnn.NMSBoxes(
            convertMatOfRectToMatOfRect2d(boundingBoxesMat),
            confidencesMat,
            0.2f,
            0.4f,
            indicesMat
        )

        val boxes:MutableList<Rect> = mutableListOf()

        for (i in indicesMat.toArray()) {
            var boundingBox = boundingBoxesMat.toArray()[i]
            boxes.add(boundingBox)
        }

        var bboxs= mutableListOf<MainActivity.BoxWithText>()

        var res=""

        for (r in boxes) {
            var startX = r.x
            var startY = r.y
            var endX = r.width
            var endY = r.height

            startX = (startX * rW).toInt()
            startY = (startY * rH).toInt()
            endX = (endX * rW).toInt()
            endY = (endY * rH).toInt()

            var croppedBitmap: Bitmap

            if(startY + (endY-startY) > bitmap.height){
                var hi=bitmap.height
                croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    startX,
                    startY,
                    endX-startX,
                    hi-startY
                )
            }else if(startY<0){
                croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    startX,
                    0,
                    endX-startX,
                    endY-startY
                )
            }
            else{
                croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    startX,
                    startY,
                    endX-startX,
                    endY-startY
                )
            }

            try{
                getFileFromAssets(context,"eng.traineddata")
                val folderPath = context.filesDir.path
                val tess = TessBaseAPI()

                textView.text="Predicting"
                tess.init(folderPath,"eng")
                tess.setImage(croppedBitmap)

                var recognizedText = tess.utF8Text
                val matchingRows = data.filter { row -> row.any { it.contains(recognizedText) } }  // Metni veri setinde ara

                // Eşleşen satırları TextView'de göster
                for (matchingRow in matchingRows) {
                    for (i in matchingRow.indices) {            // Eşleşen satırdaki her bir hücreyi göster
                        res += "${data[0][i]}: ${matchingRow[i]} \n "
                    }
                    res += "\n--------------\n"
                }

            }catch (e: Exception){
                textView.text = "Error Occured"
            }

            bboxs.add(
                MainActivity.BoxWithText(
                    android.graphics.Rect(startX, startY, endX, endY),
                    ""
                )
            )
        }

        textView.text = res
        val visualizedResult = drawDetectionResult(bitmap,bboxs)
        imageView.setImageBitmap(visualizedResult)
    }





}

fun convertMatOfRectToMatOfRect2d(matOfRect: MatOfRect): MatOfRect2d {
    val rects2d = mutableListOf<Rect2d>()
    for (rect in matOfRect.toArray()) {
        rects2d.add(Rect2d(rect.x.toDouble(), rect.y.toDouble(), rect.width.toDouble(), rect.height.toDouble()))
    }
    return MatOfRect2d(*rects2d.toTypedArray())
}

fun getFileFromAssets(context: Context, fileName: String): File {

    val cacheDir = context.filesDir.path + File.separator + "tessdata"
    val folderFile = File(cacheDir)

    if (!folderFile.exists()){
        folderFile.mkdirs()
    }

    return File(cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.createNewFile()
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }
}

fun drawDetectionResult(
    bitmap: Bitmap,
    detectionResults: List<MainActivity.BoxWithText>
): Bitmap {
    val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(outputBitmap)
    val pen = Paint()
    pen.textAlign = Paint.Align.LEFT

    detectionResults.forEach {
        pen.color = Color.RED
        pen.strokeWidth = 2f
        pen.style = Paint.Style.STROKE
        val box = it.box
        canvas.drawRect(box, pen)

        val tagSize = android.graphics.Rect(0, 0, 0, 0)

        pen.style = Paint.Style.FILL_AND_STROKE
        pen.color = Color.YELLOW
        pen.strokeWidth = 2F

        pen.getTextBounds(it.text, 0, it.text.length, tagSize)
        val fontSize: Float = pen.textSize * box.width() / tagSize.width()

        if (fontSize < pen.textSize) pen.textSize = fontSize

        var margin = (box.width() - tagSize.width()) / 2.0F
        if (margin < 0F) margin = 0F
        canvas.drawText(
            it.text, box.left + margin,
            box.top + tagSize.height().times(1F), pen
        )
    }
    return outputBitmap
}

fun convertToMultiDimArray(tensorBuffer: TensorBuffer): Array<Array<Array<FloatArray>>> {
    val outputData0 = tensorBuffer.floatArray
    val batchSizeS = tensorBuffer.shape[0]
    val heightS = tensorBuffer.shape[1]
    val widthS = tensorBuffer.shape[2]
    val channelsS = tensorBuffer.shape[3]

    var idx=0

    val multidimensionalArrayScore = Array(batchSizeS){Array(heightS){Array(widthS){FloatArray(channelsS)} } }

    for(i in 0 until batchSizeS){
        for(j in 0 until heightS){
            for(k in 0 until widthS){
                for(l in 0 until channelsS){
                    multidimensionalArrayScore[i][j][k][l]=outputData0[idx]
                    idx++
                }
            }
        }
    }


    return multidimensionalArrayScore
}

fun transposeArray(arr:Array<Array<Array<FloatArray>>>): Array<Array<Array<FloatArray>>>{
    val batchSizeS = arr.size
    val heightS = arr[0].size
    val widthS = arr[0][0].size
    val channelsS = arr[0][0][0].size

    val transposedArray=Array(batchSizeS) { Array(channelsS) { Array(heightS) { FloatArray(widthS) } } }

    for (i in 0 until transposedArray[0][0].size) {
        for (j in 0 until transposedArray[0][0][0].size) {
            for (k in 0 until transposedArray[0].size) {
                transposedArray[0][k][i][j] = arr[0][i][j][k]
            }
        }
    }

    return transposedArray
}
