package com.catchcatch

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class AsyncTaskLogin : AsyncTask<String, Void, String>() {
    var tag: String = "Login"

    override fun doInBackground(vararg info: String): String {
        val id: String = info[1]
        val password: String = info[2]
        val serverURL: String = info[0]
        val postParameters = "id=$id&password=$password"

        try {
            val url = URL(serverURL)
            val httpURLConnection: HttpURLConnection = url.openConnection() as HttpURLConnection

            httpURLConnection.readTimeout = 5000
            httpURLConnection.connectTimeout = 5000
            httpURLConnection.requestMethod = "POST"
            httpURLConnection.connect()

            val outputStream: OutputStream = httpURLConnection.outputStream
            outputStream.write(postParameters.toByteArray())
            outputStream.flush()
            outputStream.close()

            val responseStatusCode: Int = httpURLConnection.responseCode
            Log.d(tag, "response code - $responseStatusCode")

            lateinit var inputStream: InputStream

            if (responseStatusCode == HttpURLConnection.HTTP_OK) inputStream = httpURLConnection.inputStream
            else inputStream = httpURLConnection.errorStream

            val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
            val bufferedReader = BufferedReader(inputStreamReader)

            val sb: StringBuilder = StringBuilder()
            var line: String? = bufferedReader.readLine()

            while (line != null) {
                sb.append(line)
                line = bufferedReader.readLine()
            }

            bufferedReader.close()

            return sb.toString()
        }
        catch (e: Exception ) {
            Log.d(tag, "Error ", e)

            return "Error: " + e.message
        }
    }
}