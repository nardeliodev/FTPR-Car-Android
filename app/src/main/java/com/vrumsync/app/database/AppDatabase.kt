package com.vrumsync.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vrumsync.app.database.converters.DateConverters
import com.vrumsync.app.database.dao.UserLocationDao
import com.vrumsync.app.database.model.UserLocation

@Database(entities = [UserLocation::class], version = 1)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userLocationDao(): UserLocationDao
}