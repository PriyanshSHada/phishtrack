package com.example.phishtrack.utils

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is UnknownHostException -> "No internet connection. Check your network and try again."
        is SocketTimeoutException -> "Request timed out. Please try again."
        is IOException -> "No internet connection. Check your network and try again."
        is HttpException -> {
            if (code() >= 500) {
                "Something went wrong on our end. Please try again."
            } else {
                "An error occurred: ${message()}"
            }
        }
        else -> message ?: "An unknown error occurred."
    }
}
