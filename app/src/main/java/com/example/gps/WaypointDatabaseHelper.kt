package com.example.gps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WaypointDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "waypoints.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE waypoints (id INTEGER PRIMARY KEY AUTOINCREMENT, latitude REAL, longitude REAL, label TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS waypoints")
        onCreate(db)
    }
}
