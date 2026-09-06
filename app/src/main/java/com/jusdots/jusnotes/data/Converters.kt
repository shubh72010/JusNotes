package com.jusdots.jusnotes.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    companion object {
        // ponytail: cache Gson + Types — old code allocated TypeToken anonymous class per row × per column
        private val gson = Gson()
        private val stringListType = object : TypeToken<List<String>>() {}.type
        private val checklistType = object : TypeToken<List<ChecklistItem>>() {}.type
        private val intListType = object : TypeToken<List<Int>>() {}.type
        private val mapItemType = object : TypeToken<List<MapItem>>() {}.type
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return gson.fromJson(value, stringListType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringListToString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromChecklistItemList(value: String?): List<ChecklistItem> {
        if (value.isNullOrBlank()) return emptyList()
        return gson.fromJson(value, checklistType) ?: emptyList()
    }

    @TypeConverter
    fun fromChecklistItemListToString(list: List<ChecklistItem>?): String {
        return gson.toJson(list ?: emptyList<ChecklistItem>())
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return gson.fromJson(value, intListType) ?: emptyList()
    }

    @TypeConverter
    fun fromIntListToString(list: List<Int>?): String {
        return gson.toJson(list ?: emptyList<Int>())
    }
    @TypeConverter
    fun fromMapItems(value: String?): List<MapItem> {
        if (value.isNullOrBlank()) return emptyList()
        return gson.fromJson(value, mapItemType) ?: emptyList()
    }

    @TypeConverter
    fun mapItemsToString(list: List<MapItem>?): String {
        return gson.toJson(list ?: emptyList<MapItem>())
    }
}
