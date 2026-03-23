package com.vrumsync.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vrumsync.app.databinding.ActivityItemDetailBinding
import com.vrumsync.app.model.Car
import com.vrumsync.app.service.Result
import com.vrumsync.app.service.RetrofitClient
import com.vrumsync.app.service.safeApiCall
import com.vrumsync.app.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.vrumsync.app.R
import kotlinx.coroutines.*

class ItemDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityItemDetailBinding
    private lateinit var car: Car
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        loadItem()
        setupGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (::car.isInitialized) {
            loadCarInMap()
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.deleteCTA.setOnClickListener { deleteItem() }
        binding.editCTA.setOnClickListener { editItem() }
    }

    private fun loadItem() {
        val itemId = intent.getStringExtra(ARG_ID) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getItem(itemId) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        car = result.data
                        handleSuccess()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@ItemDetailActivity,
                            R.string.error_fetch_item,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun handleSuccess() {
        // EditText (usar setText)
        binding.name.setText(car.name)

        // TextViews
        binding.year.text = "Ano: ${car.year}"
        binding.licence.setText(car.licence)

        binding.image.loadUrl(car.imageUrl)

        loadCarInMap()
    }

    private fun setupGoogleMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun loadCarInMap() {
        if (!::mMap.isInitialized) return

        car.place?.let {
            binding.googleMapContent.visibility = View.VISIBLE

            val location = LatLng(it.lat, it.long)

            mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(car.name)
            )

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(location, 15f)
            )
        }
    }

//    private fun deleteItem() {
//        CoroutineScope(Dispatchers.IO).launch {
//            val result = safeApiCall {
//                RetrofitClient.apiService.deleteItem(car.id)
//            }
//
//            withContext(Dispatchers.Main) {
//                when (result) {
//                    is Result.Success -> handleSuccessDelete()
//                    is Result.Error -> {
//                        Toast.makeText(
//                            this@ItemDetailActivity,
//                            R.string.error_delete,
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//        }
//    }

    private fun deleteItem() {
        CoroutineScope(Dispatchers.IO).launch {

            //val response = RetrofitClient.apiService.deleteItem(car.id)
            val response = RetrofitClient.apiService.deleteCar(car.id)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    handleSuccessDelete() // ✔ funciona com 204
                } else {
                    Toast.makeText(
                        this@ItemDetailActivity,
                        R.string.error_delete,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun editItem() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.updateItem(
                    car.id,
                    car.copy(
                        name = car.name, // não é editável na tela
                        year = car.year,
                        licence = binding.licence.text.toString(), //  correto
                        imageUrl = car.imageUrl,
                        place = car.place
                    )
                )
            }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(
                            this@ItemDetailActivity,
                            R.string.success_update,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                    is Result.Error -> {
                        Toast.makeText(
                            this@ItemDetailActivity,
                            R.string.error_update,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun handleSuccessDelete() {
        Toast.makeText(
            this,
            R.string.success_delete,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    companion object {
        const val ARG_ID = "arg_id"

        fun newIntent(context: Context, itemId: String): Intent {
            return Intent(context, ItemDetailActivity::class.java).apply {
                putExtra(ARG_ID, itemId)
            }
        }
    }
}