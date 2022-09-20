package com.example.kidsdrawing

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var ibBrushSizeChooser: ImageButton
    private lateinit var llColorPallet: LinearLayout
    private lateinit var mSelectedColorFromPallet: ImageButton
    private lateinit var ibGallery: ImageButton
    private lateinit var ibUndo: ImageButton
    private lateinit var ibRedo: ImageButton
    private lateinit var ivBackground: ImageView

    private var progressDialog: Dialog? = null


    val storagePermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        //Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show()
                        val pickIntent = // yeh use hoata hy agar image utahni ho gallery men se
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }
                } else {
                    if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission denied for write storage access",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Permission denied for read storage access",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    /**
     * yeh use ho raha hy GAllery se image ko get kar k
     * hamary image view men show karny kleye
     */
    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

                result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ivBackGroung: ImageView = findViewById(R.id.ivBackground)
                ivBackGroung.setImageURI(result.data?.data) // result.data?.data   ka matlab hy k is result k variable data men se data nikalo
            } else {
                Toast.makeText(this, "Error while getting image from gallery", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    //                **/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        ibBrushSizeChooser = findViewById(R.id.ibBrushSizeChooser)
        llColorPallet = findViewById(R.id.llColorPallet)
        ibGallery = findViewById(R.id.ibGalleryImageChooser)
        ibUndo = findViewById(R.id.ibUndo)
        ibRedo = findViewById(R.id.ibRedo)
        ivBackground = findViewById(R.id.ivBackground)
        val ibSave: ImageButton = findViewById(R.id.ibFileSave)


        ibSave.setOnClickListener {
            if (readPermissionGranted()) {
                progressDialogShow()
                lifecycleScope.launch {
                    val frameLayout: FrameLayout = findViewById(R.id.frameLayout)
                    saveBitmapOnStorage(viewToBitmap(frameLayout)) // functions that I created below
                }
            }
        }

        ibUndo.setOnClickListener {
            drawingView.undoDrawing()
        }

        ibRedo.setOnClickListener {
            drawingView.redoDrawing()
        }

        /**
         * Image choosing from gallery
         */

        ibGallery.setOnClickListener {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                //** this will show in rational dialog
                val alertDialog = AlertDialog.Builder(applicationContext)
                alertDialog.setTitle("Kids drawing app required storage permission")
                alertDialog.setMessage("It requires permission to access your external storage for choosing image to draw in app")
                alertDialog.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                alertDialog.setCancelable(false)
                val dialog1: AlertDialog = alertDialog.create()
                dialog1.show()
                //                             **//
            } else {
                storagePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
                // yeh intent use hoga gallery tak jany kelye
            }
        }

        /**
         * by default selected pallet color
         */
        mSelectedColorFromPallet = llColorPallet[3] as ImageButton
        mSelectedColorFromPallet.setImageDrawable(
            ContextCompat
                .getDrawable(this, R.drawable.bg_colorpallet_pressed)
        )
        /**
         *
         */
        ibBrushSizeChooser.setOnClickListener(View.OnClickListener {
            changeBrushThicknessSize()
        })

    }

    private fun changeBrushThicknessSize() {

        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.layout_brushsizedialog)
        brushDialog.setTitle("Brush Size")

        // is men aub hum apny is layout se un tamam buttons ko fvb kar ray hen taaky un k mutaabik koi action perform kiya jay
        val xsmallbtn: ImageButton = brushDialog.findViewById(R.id.xsmallBrush)
        val smallbtn: ImageButton = brushDialog.findViewById(R.id.smallBrush)
        val mediumbtn: ImageButton = brushDialog.findViewById(R.id.mediumBrush)
        val largebtn: ImageButton = brushDialog.findViewById(R.id.largeBrush)
        val xlargebtn: ImageButton = brushDialog.findViewById(R.id.xlargeBrush)

        xsmallbtn.setOnClickListener(View.OnClickListener {

            drawingView.setBurshSize(3.toFloat())
            brushDialog.dismiss()
        })

        smallbtn.setOnClickListener(View.OnClickListener {

            drawingView.setBurshSize(6.toFloat())
            brushDialog.dismiss()
        })

        mediumbtn.setOnClickListener {

            drawingView.setBurshSize(12.toFloat())
            brushDialog.dismiss()
        }

        largebtn.setOnClickListener(View.OnClickListener {

            drawingView.setBurshSize(12.toFloat())
            brushDialog.dismiss()
        })

        xlargebtn.setOnClickListener(View.OnClickListener {

            drawingView.setBurshSize(15.toFloat())
            brushDialog.dismiss()
        })

        brushDialog.show()


    }

    fun changeColor(view: View) {
        if (view != mSelectedColorFromPallet) {

            val selectedBtn = view as ImageButton
            val selectedColor = selectedBtn.tag.toString()

            drawingView.setColor(selectedColor)
            selectedBtn.setImageDrawable(
                ContextCompat
                    .getDrawable(this, R.drawable.bg_colorpallet_pressed)
            )

            mSelectedColorFromPallet.setImageDrawable(
                ContextCompat
                    .getDrawable(this, R.drawable.bg_colorpallet_normal)
            )
            mSelectedColorFromPallet = view

        }
    }

    private fun viewToBitmap(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap) // bind hogya canvas, bitmap k sath
        val bgDrawable = view.background

        if (bgDrawable != null) {
            bgDrawable.draw(canvas) // canvas ko bg par draw kar do
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas) // canvas ko view par bhi draw kar do

        /*
         * maqsad is sab ka yeh tha k hum keh ray hen k
         * view, background, aur canvas ko 1 sath mila kr Bitmap men convert karna hy
         */

        /**
         * aub is returned bitMap k andar view, background aur canvas sab mojood hen
         * jo k bitmap ki form men hen yani bits ki form men
         */
        return returnedBitmap

    }

    //coroutine function
    private suspend fun saveBitmapOnStorage(bitmap: Bitmap?): String {
        var result: String = ""
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                //android version 10 k aagy isi code se save hota hy
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val os: OutputStream
                    try {
                        val resolver: ContentResolver = contentResolver
                        val cv: ContentValues = ContentValues()
                        val fileName: String =
                            "DrawingApp_Awi_ ${System.currentTimeMillis() / 1000} .jpg"
                        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
                        cv.put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            // yahan par wo directory likhni hy jis men ap apna app k name ka
                            // folder bnaa k image save karna chahty hon
                            Environment.DIRECTORY_DOWNLOADS + File.separator + getString(R.string.app_name)
                        )
                        // yahan py aub **Downloads** ki jaga wohi directory ka name likhna hy
                        // jahan store karna ho
                        val imageUri: Uri? =
                            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                        os = Objects.requireNonNull(imageUri)
                            ?.let { resolver.openOutputStream(it) }!!
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        Objects.requireNonNull(os)
                        // for getting location of file
                        result = Environment.DIRECTORY_DOWNLOADS + File.separator +
                                getString(R.string.app_name) + File.separator + fileName
                        /**
                         * is se hamri image share hogi
                         * android 11 + par bhi
                         */
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                        shareIntent.setType("image/*")
                        startActivity(Intent.createChooser(shareIntent, "Share"))
                        /**
                         * End
                         */


                        /**
                         * aur is ka second method yeh hy jo is k neechy likha hy
                         * ismen hum aub directory aur filename
                         * 1 hi line men pathname ki sorat men btaa dety hen
                         * aur yahan hum ByteArrayOutPutStream ka bhi use kar ray hen
                         * bitmat ko compress kar k isi men store kar ray hen
                         * bad men jaaa k yeh bytes stream ko
                         * FileOutPutStream ko pass kar ray hen
                         */

                        /* val bytes = ByteArrayOutputStream()
                         bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                         val filePath = File(externalCacheDir?.absoluteFile.toString() +
                                 File.separator + "DrawingApp_Awi_" +
                                 System.currentTimeMillis() / 1000 + ".jpg")
                         val os = FileOutputStream(filePath)
                         os.write(bytes.toByteArray())
                         os.close()
                         result = filePath.absolutePath*/
                        //shareImage(result)        // function declared below

                        runOnUiThread {

                            progressDialogClose()
                            if (result.isNotEmpty()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "File saved at: " + result,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Unable to save file",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        // }
                    } catch (e: Exception) {
                        result = " "
                        Toast.makeText(this@MainActivity, "Unable to save file", Toast.LENGTH_SHORT)
                            .show()
                        e.printStackTrace()
                    }

                } else {
                    val directory: File = File(
                        Environment.getExternalStorageDirectory(),
                        getString(R.string.app_name)
                    )
                    if (!directory.exists()) {
                        directory.mkdir()
                    }
                    val fileName =
                        "DrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    val filePath: File = File(directory, fileName)
                    try {

                        val os: FileOutputStream = FileOutputStream(filePath)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        result = filePath.absolutePath
                        val mimeType: String = "image/*"
                        MediaScannerConnection.scanFile(this@MainActivity,
                            arrayOf(result), arrayOf(mimeType),
                            MediaScannerConnection.OnScanCompletedListener { s, uri ->
                                val shareIntent = Intent(Intent.ACTION_SEND)
                                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                                shareIntent.setType("image/*")
                                startActivity(Intent.createChooser(shareIntent, "Share"))
                            })
                        runOnUiThread {
                            progressDialogClose()
                            if (result != null) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "file saved at: $result",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                result = " "
                                Toast.makeText(
                                    this@MainActivity,
                                    "Unable to save file",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        result = " "
                        e.printStackTrace()
                    }
                }

            }

        }

        return result
    }

    private fun readPermissionGranted(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED

    }

    /**
     * this function not wokrs in my case
     */
    private fun shareImage(result: String) {

        MediaScannerConnection.scanFile(
            this,
            arrayOf(result),
            null){ s, uri ->
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.setType("image/*")
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }

    }


private fun progressDialogShow() {
    progressDialog = Dialog(this)
    progressDialog?.setContentView(R.layout.layout_progressbar)
    progressDialog?.show()
}

private fun progressDialogClose() {
    if (progressDialog != null) {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
}