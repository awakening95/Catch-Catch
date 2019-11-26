package com.catchcatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_video_player.*
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.extractor.flv.FlvExtractor
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File

class VideoPlayerActivity: AppCompatActivity() {

    lateinit var file: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val intent = intent
        file = intent.getStringExtra("file")!!

        val player = ExoPlayerFactory.newSimpleInstance(this)
        activity_video_player_exoplayer_view.player = player
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "CatchCatch"))
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(File(file)))
        player.prepare(videoSource)
        player.playWhenReady = true
    }
}