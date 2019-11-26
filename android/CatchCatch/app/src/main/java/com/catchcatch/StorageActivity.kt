package com.catchcatch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_storage.*
import java.io.File

class StorageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage)

        activity_storage_recycler_view.layoutManager = LinearLayoutManager(this)
        activity_storage_recycler_view.setHasFixedSize(true)

        val fileList = arrayListOf<String>()
        val fullPathFileList = arrayListOf<String>()
        val thumbnailList = arrayListOf<Bitmap?>()

        val folder = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" + File.separator + "CatchCatch" + File.separator
        val directory = File(folder)
        if (!directory.exists()) directory.mkdirs()

        if (directory.list() != null) {
            directory.list()!!.forEach {
                val extension = it.substring(it.length - 3, it.length)
                val fileName = it.substring(0, it.length - 3) // .포함
                val pngFileName = fileName + "png"

                if (extension == "flv") {
                    if(!File(pngFileName).exists()) {
                        FFmpeg.execute("-i ${folder + it} -ss 00:00:01.000 -vframes 1 ${folder + pngFileName}")
                    }

                    fileList.add(it)
                    fullPathFileList.add(folder + it)
                    thumbnailList.add(BitmapFactory.decodeFile(folder + pngFileName))

                } else if (extension == "mp4") {
                    fileList.add(it)
                    fullPathFileList.add(folder + it)
                    thumbnailList.add(ThumbnailUtils.createVideoThumbnail(folder + it, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND))
                }
            }
        }

        activity_storage_recycler_view.adapter = StorageRecyclerViewAdapter(this, fileList, thumbnailList, fullPathFileList)
    }
}