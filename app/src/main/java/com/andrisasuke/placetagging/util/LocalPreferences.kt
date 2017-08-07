package com.andrisasuke.placetagging.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.support.v4.util.Pair
import com.andrisasuke.placetagging.BuildConfig
import com.andrisasuke.placetagging.model.UserModel
import com.google.gson.Gson


class LocalPreferences(val context: Context) {

    private val preferences: SharedPreferences by lazy { context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE) }
    private val gson: Gson by lazy { Gson() }

    companion object {
        val LAST_LATITUDE = "last_latitude"
        val LAST_LONGITUDE = "last_longitude"
        val USER_MODEL = "m_user"
    }

    fun storeUser(userModel: UserModel) {
        val json = gson.toJson(userModel)
        putPreference(USER_MODEL, json)
    }

    fun getUser(): UserModel? {
        val json: String = findPreference(USER_MODEL, "")
        if(json != "") return gson.fromJson(json, UserModel::class.java)
        else return null
    }

    fun destroy(){
        preferences.edit().clear().apply()
    }

    fun storeLastLocation(latitude: String, longitude: String) {
        preferences.edit().putString(LAST_LATITUDE, latitude)
                .putString(LAST_LONGITUDE, longitude).apply()
    }

    fun getLastLocation(): Pair<String, String>? {
        val lat = findPreference(LAST_LATITUDE, "")
        val lon = findPreference(LAST_LONGITUDE, "")
        if (lat != "" && lon != "") return Pair.create<String, String>(lat, lon)
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findPreference(name: String, default: T?): T = with(preferences) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw IllegalArgumentException("Type is unknown")
        }
        res as T
    }

    @SuppressLint("CommitPrefEdits")
    private fun <T> putPreference(name: String, value: T) = with(preferences.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type can't be saved into Preferences")
        }.apply()
    }
}