package com.example.mytestapp3

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mytestapp3.ml.EastModelFloat16
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer



class MainActivity : AppCompatActivity() {

    data class BoxWithText(val box: android.graphics.Rect, val text: String)

    lateinit var imageUri: Uri
    lateinit var bitmap: Bitmap

    lateinit var postProcessingHelper: PostProcessingHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var imageView: ImageView = findViewById(R.id.imageView)
        var textView: TextView = findViewById(R.id.textView)
        var selectImage: Button = findViewById(R.id.selectButton)
        var predictButton: Button = findViewById(R.id.predictButton)

        textView.setMovementMethod(ScrollingMovementMethod())


        postProcessingHelper = PostProcessingHelper(
            imageView,
            textView,
            this
        )

        var imageProcessor= ImageProcessor.Builder()
            .add(ResizeOp(320,320, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        selectImage.setOnClickListener {
            var intent: Intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent,100)
        }

        predictButton.setOnClickListener {
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            val rW = (bitmap.width.toFloat() / 320.0).toFloat()
            val rH = (bitmap.height.toFloat() / 320.0).toFloat()

            tensorImage=imageProcessor.process(tensorImage)
            val model = EastModelFloat16.newInstance(this)
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 320, 320, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(tensorImage.buffer)

            val outputs:EastModelFloat16.Outputs = model.process(inputFeature0)

            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val outputFeature1 = outputs.outputFeature1AsTensorBuffer

            val data = readExcelFile(this, "Dataset.xlsx")

            postProcessingHelper.postProcessing(outputFeature0, outputFeature1, rH, rW, data, bitmap)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==100){
            var uri=data?.data
            if (uri != null) {
                imageUri=uri
            }
            bitmap= MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
            postProcessingHelper.imageView.setImageBitmap(bitmap)
            postProcessingHelper.textView.text=""
        }
    }
}
