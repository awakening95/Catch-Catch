package com.catchcatch

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import android.text.Editable
import android.util.Log
import android.widget.Toast
import com.facebook.FacebookSdk.getApplicationContext
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class StorageRecyclerViewAdapter(private val context: StorageActivity, private val fileList: ArrayList<String>, private val thumbnailList: ArrayList<Bitmap?>, private val fullPathFileList: ArrayList<String>) : RecyclerView.Adapter<StorageRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val linearLayout: LinearLayout = view.findViewById(R.id.recycler_view_item_storage_linear_layout)
        val constraintLayoutInfo: ConstraintLayout = view.findViewById(R.id.recycler_view_item_storage_info)
        val textViewTitle: TextView = view.findViewById(R.id.recycler_view_item_storage_title)
        val textViewSize: TextView = view.findViewById(R.id.recycler_view_item_storage_size)
        val textViewdate: TextView = view.findViewById(R.id.recycler_view_item_storage_date)
        val imageViewThumbnail: ImageView = view.findViewById(R.id.recycler_view_item_storage_thumbnail)
        val imageViewMenu: ImageView = view.findViewById(R.id.recycler_view_item_storage_menu)
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item_storage, parent, false)

        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileName = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CatchCatch/" + fileList[position])
        val extension = fileList[position].substring(fileList[position].length - 4, fileList[position].length) // . 포함

        val thumbnailFileName = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "CatchCatch/" + fileList[position].substring(0, fileList[position].length - 4) + ".png")

        val lastModified = Date(fileName.lastModified())
        val formatter = SimpleDateFormat("yy.MM.dd HH:mm:ss", Locale.KOREAN)
        val formattedDateString = formatter.format(lastModified)
        holder.textViewdate.text = formattedDateString

        var fileSize = fileName.length().toFloat()
        var result = fileSize / 1024
        var unit = "B"

        if (result > 1.00) {
            fileSize /= 1024
            result = fileSize / 1024
            unit = "KB"

            if (result > 1.00) {
                fileSize /= 1024
                result = fileSize / 1024
                unit = "MB"

                if (result > 1.00) {
                    fileSize /= 1024
                    unit = "GB"
                }
            }
        }
        val size = String.format("%.2f", fileSize) + unit
        holder.textViewSize.text = size

        holder.textViewTitle.text = fileList[position]
        holder.imageViewThumbnail.setImageBitmap(thumbnailList[position])

        holder.imageViewThumbnail.setOnClickListener {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra("file", fullPathFileList[position])
            context.startActivity(intent)
        }

        holder.constraintLayoutInfo.setOnClickListener {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra("file", fullPathFileList[position])
            context.startActivity(intent)
        }

        holder.imageViewMenu.setOnClickListener {
            val popup = PopupMenu(context, holder.imageViewMenu)
            popup.inflate(R.menu.file_option_menu)

            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.file_option_menu_name_change -> { // 이름 변경
                        val editText = EditText(context)
                        editText.text = Editable.Factory.getInstance().newEditable(fileList[position].substring(0, fileList[position].length - 4))

                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("이름 변경")
                        builder.setView(editText)
                        builder.setPositiveButton("입력") { dialog, which ->
                            val changedFileName = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CatchCatch/" + editText.text.toString() + extension)

                            if (changedFileName.exists()) {
                                Toast.makeText(getApplicationContext(), "이미 존재하는 파일 이름입니다.", Toast.LENGTH_LONG).show()
                            } else {
                                val result = fileName.renameTo(changedFileName)

                                when (result) {
                                    true -> {
                                        val changedThumbnailFileName = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CatchCatch/" + editText.text.toString() + ".png")
                                        if (thumbnailFileName.exists()) {
                                            thumbnailFileName.renameTo(changedThumbnailFileName)
                                        }

                                        Toast.makeText(getApplicationContext(), "변경되었습니다.", Toast.LENGTH_LONG).show()
                                        val intent = Intent(context, StorageActivity::class.java)
                                        context.finish()
                                        context.startActivity(intent)
                                    }
                                    false -> Toast.makeText(getApplicationContext(), "이름 변경에 실패하였습니다.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        builder.setNegativeButton("취소") { dialog, which -> }
                        builder.show()
                    }

                    R.id.file_option_menu_name_delete -> { // 삭제
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("확인")
                        builder.setMessage("정말 삭제하시겠습니까?")
                        builder.setPositiveButton("예") { dialog, which ->
                            val result = fileName.delete()
                            when (result) {
                                true -> {
                                    if (thumbnailFileName.exists()) {
                                        thumbnailFileName.delete()
                                    }

                                    Toast.makeText(getApplicationContext(), "삭제되었습니다.", Toast.LENGTH_LONG).show()
                                    val intent = Intent(context, StorageActivity::class.java)
                                    context.finish()
                                    context.startActivity(intent)
                                }
                                false -> Toast.makeText(getApplicationContext(), "삭제에 실패하였습니다.", Toast.LENGTH_LONG).show()
                            }
                        }
                        builder.setNegativeButton("아니오") { dialog, which -> }
                        builder.show()
                    }
                    R.id.file_option_menu_name_share -> {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.setType("video/*")
                        val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", fileName)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        context.startActivity(Intent.createChooser(intent, "공유하기"))
                    }
                }
                true
            })
            popup.show()
        }
    }
}

