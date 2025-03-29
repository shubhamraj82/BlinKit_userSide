package com.example.userblinkit.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CartProducts::class], version = 2, exportSchema = false)
abstract class CartProductsDatabase : RoomDatabase() {
    abstract fun cartProductsDao() : CartProductDao

    companion object {

        // Volatile ensures that any update to the variable is immediately visible to all threads
        @Volatile
        var INSTANCE : CartProductsDatabase ?= null

        fun getDatabaseInstance(context: Context) : CartProductsDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val roomDb = Room.databaseBuilder(context, CartProductsDatabase::class.java, "CartProducts").allowMainThreadQueries().build()
                INSTANCE = roomDb
                return roomDb
            }
        }
    }
}