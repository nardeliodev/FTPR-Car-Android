package com.vrumsync.app.model

data class Car(
    val id: String,
    val name: String,
    val year: String,
    val imageUrl: String,
    val licence: String,
    val place: Place?
)

data class Place(
    val lat: Double,
    val long: Double
)