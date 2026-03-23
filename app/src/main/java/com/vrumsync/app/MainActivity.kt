package com.vrumsync.app

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.vrumsync.app.adapter.ItemAdapter
import com.vrumsync.app.database.DatabaseBuilder
import com.vrumsync.app.database.model.UserLocation
import com.vrumsync.app.model.Car
import com.vrumsync.app.service.Result
import com.vrumsync.app.service.RetrofitClient
import com.vrumsync.app.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.vrumsync.app.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        fetchItems()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_loggout -> {
                onLoggout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onLoggout() {
        FirebaseAuth.getInstance().signOut()
        val intent = LoginActivity.newIntent(this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun setupView() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchItems()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.addCta.setOnClickListener {
            startActivity(NewItemActivity.newIntent(this))
        }

        binding.message.setOnClickListener {
            fetchItems()
        }
    }

    // LOCALIZAÇÃO

    private fun requestLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    getLastLocation()
                } else {
                    Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
                }
            }

        checkLocationPermissionAndRequest()
    }

    private fun checkLocationPermissionAndRequest() {
        when {
            ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED -> {
                getLastLocation()
            }
            else -> {
                locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                val location = task.result

                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseBuilder.getInstance()
                        .userLocationDao()
                        .insert(
                            UserLocation(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                }
            }
        }
    }

    // API

    private fun fetchItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getItems() }

            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false

                when (result) {
                    is Result.Success -> handleOnSuccess(result.data)
                    is Result.Error -> handleOnError()
                }
            }
        }
    }

    private fun handleOnSuccess(cars: List<Car>) {
        if (cars.isEmpty()) {
            binding.message.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            return
        }

        binding.message.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE

        binding.recyclerView.adapter = ItemAdapter(cars) { car ->
            val intent = ItemDetailActivity.newIntent(this, car.id)
            startActivity(intent)
        }
    }

    private fun handleOnError() {
        binding.message.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    companion object {
        fun newIntent(context: Context) =
            Intent(context, MainActivity::class.java)
    }
}