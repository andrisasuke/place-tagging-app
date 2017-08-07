package com.andrisasuke.placetagging.adapter

import android.content.Context
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andrisasuke.placetagging.R
import com.andrisasuke.placetagging.model.Places
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import kotlinx.android.synthetic.main.place_item.view.*

class TagPlacesAdapter(val context: Context,
                       val query: Query,
                       val listPlaces: MutableList<Places>,
                       val listKeys: MutableList<String>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TAG = "TagPlacesAdapter"
    }

    private var mBackground: Int = 0

    private val mListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            val key = dataSnapshot.key

            if (!listKeys.contains(key)) {
                Log.d(TAG, "added places $key")
                val item: Places = dataSnapshot.getValue(Places::class.java)
                val insertedPosition: Int
                if (previousChildName == null) {
                    listPlaces.add(0, item)
                    listKeys.add(0, key)
                    insertedPosition = 0
                } else {
                    val previousIndex = listKeys.indexOf(previousChildName)
                    val nextIndex = previousIndex + 1
                    if (nextIndex == listPlaces.size) {
                        listPlaces.add(item)
                        listKeys.add(key)
                    } else {
                        listPlaces.add(nextIndex, item)
                        listKeys.add(nextIndex, key)
                    }
                    insertedPosition = nextIndex
                }
                notifyItemInserted(insertedPosition)
                itemAdded(item, key, insertedPosition)
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String) {
            val key = dataSnapshot.key

            if (listKeys.contains(key)) {
                val index = listKeys.indexOf(key)
                val oldItem = listPlaces[index]
                val newItem = dataSnapshot.getValue(Places::class.java)

                listPlaces[index] = newItem

                notifyItemChanged(index)
                itemChanged(oldItem, newItem, key, index)
            }
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            val key = dataSnapshot.key

            if (listKeys.contains(key)) {
                val index = listKeys.indexOf(key)
                val item = listPlaces[index]

                listKeys.removeAt(index)
                listPlaces.removeAt(index)

                notifyItemRemoved(index)
                itemRemoved(item, key, index)
            }
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            val key = dataSnapshot.key

            val index = listKeys.indexOf(key)
            val item = dataSnapshot.getValue(Places::class.java)
            listPlaces.removeAt(index)
            listKeys.removeAt(index)
            val newPosition: Int
            if (previousChildName == null) {
                listPlaces.add(0, item)
                listKeys.add(0, key)
                newPosition = 0
            } else {
                val previousIndex = listKeys.indexOf(previousChildName)
                val nextIndex = previousIndex + 1
                if (nextIndex == listPlaces.size) {
                    listPlaces.add(item)
                    listKeys.add(key)
                } else {
                    listPlaces.add(nextIndex, item)
                    listKeys.add(nextIndex, key)
                }
                newPosition = nextIndex
            }
            notifyItemMoved(index, newPosition)
            itemMoved(item, key, index, newPosition)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "Listen was cancelled, no more updates will occur, " + error.message)
        }
    }

    init {
        val mTypedValue = TypedValue()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.theme.resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true)
            mBackground = mTypedValue.resourceId
            if (mBackground == 0) mBackground = R.drawable.abc_item_background_holo_light;
        }

        query.addChildEventListener(mListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent?.context).inflate(R.layout.place_item,
                parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val place = listPlaces[position]
        (holder as ViewHolder).bindView(place)
    }

    override fun getItemCount(): Int = listPlaces.size


    public fun itemAdded(item: Places, key: String, position: Int) {
        Log.d(TAG, "adding place key $key")
    }

    public fun itemChanged(oldItem: Places, newItem: Places, key: String, position: Int) {

    }


    public fun itemRemoved(item: Places, key: String, position: Int) {

    }


    public fun itemMoved(item: Places, key: String, oldPosition: Int, newPosition: Int) {

    }

    public fun destroy(){
        query.removeEventListener(mListener)
    }

    inner class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        fun bindView(places: Places){
            with(places){
                itemClickBackground(itemView, mBackground)
                itemView.place_name.text = places.name
                itemView.place_desc.text = places.desc
                itemView.setOnClickListener{
                    Log.d(TAG, "place item ${name} is selected")
                }
            }
        }
    }

    fun itemClickBackground(view: View, @DrawableRes res: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            view.setBackgroundResource(res)
        }
    }

}