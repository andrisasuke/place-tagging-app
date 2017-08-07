package com.andrisasuke.placetagging.tag

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import com.andrisasuke.placetagging.BaseActivity
import com.andrisasuke.placetagging.R
import com.google.firebase.database.FirebaseDatabase

import com.andrisasuke.placetagging.model.Places
import kotlinx.android.synthetic.main.tag_location_activity.*
import java.util.*

class TagLocationActivity: BaseActivity() {

    companion object {
        val TAG = "TagLocationActivity"
        val LATITUDE_PARAM = "p_lat"
        val LONGITUDE_PARAM = "p_lon"
    }

    private val database by lazy { FirebaseDatabase.getInstance().reference }
    private val toolbar by lazy { findViewById(R.id.toolbar) as Toolbar }
    private var latitude: String? = null
    private var longitude: String? = null
    private val userModel by lazy { localPreferences.getUser() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tag_location_activity)

        latitude = intent.extras.getString(LATITUDE_PARAM)
        longitude = intent.extras.getString(LONGITUDE_PARAM)

        renderView()
    }

    fun renderView(){
        lat_lon_tv.text = getString(R.string.lat_lon_f, latitude, longitude)
        tag_button.setOnClickListener{
            tagPlace()
        }

        toolbar.findViewById<ImageView>(R.id.back_arrow).setOnClickListener{
            finish()
        }
    }

    fun validate(): Boolean {
        if(TextUtils.isEmpty(place_name.text.toString())) {
            place_name.error = "Name can't be empty"
            return false
        }

        if(TextUtils.isEmpty(place_desc.text.toString())) {
            place_desc.error = "Description can't be empty"
            return false
        }

        return true
    }

    fun tagPlace(){
        if(!validate()) return
        val key = database.child("events_tag_places").key
        Log.d(TAG, "event_tag_places new id $key")

        val place = Places( id = key,
                        name = place_name.text.toString(),
                        desc = place_desc.text.toString(),
                        latitude = latitude ?: "",
                        longitude = longitude ?: "",
                        created_at = Date().time,
                        created_by = userModel!!.id )

        tag_button.isEnabled = false
        val task = database.child("events_tag_places")
                .child(place.id).setValue(place)
        task.addOnCompleteListener{
            finish()
        }

        task.addOnFailureListener{
            e ->
            Log.e(TAG, "failed to save tag places, ${e.message}")
            tag_button.isEnabled = true
            showSnackbar(main_layout, "Failed to tag this place, please try again later.")
        }

    }
}