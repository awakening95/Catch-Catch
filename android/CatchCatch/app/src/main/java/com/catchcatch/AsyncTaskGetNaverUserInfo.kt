package com.catchcatch

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AsyncTaskGetNaverUserInfo : AsyncTask<String, Void, String>() {
    private var tag: String = "AsyncTaskGetNaverUserInfo"

    override fun doInBackground(vararg info: String): String {
        val accessToken: String = info[0]

        val header = "Bearer $accessToken"
        try {
            val apiURL = "https://openapi.naver.com/v1/nid/me"
            val url = URL(apiURL)

            val httpURLConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = "GET"
            httpURLConnection.setRequestProperty("Authorization", header)
            httpURLConnection.connect()

            val responseCode = httpURLConnection.responseCode

            val br: BufferedReader

            if (responseCode == 200) br = BufferedReader(InputStreamReader(httpURLConnection.inputStream, "UTF-8"))
            else br = BufferedReader(InputStreamReader(httpURLConnection.errorStream, "UTF-8"))

            val sb = java.lang.StringBuilder()
            var inputLine: String? = br.readLine()

            while (inputLine != null) {
                sb.append(inputLine)
                inputLine = br.readLine()
            }
            br.close()

            return sb.toString()
        } catch (e: Exception) {
            Log.d(tag, "Error ", e)

            return "Error: ${e.message}"
        }
    }
}