package com.vrumsync.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vrumsync.app.databinding.ActivityNewItemBinding
import com.vrumsync.app.model.Car
import com.vrumsync.app.model.Place
import com.vrumsync.app.service.Result
import com.vrumsync.app.service.RetrofitClient
import com.vrumsync.app.service.safeApiCall
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NewItemActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewItemBinding
    private lateinit var mMap: GoogleMap

    private var selectedMarker: Marker? = null
    private var imageUri: Uri? = null
    private var imageFile: File? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                binding.imageUrl.setText("Imagem capturada")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupMask()     // NOVO AQUI
        setupYearMask() // NOVO AQUI
        setupGoogleMap()
    }

    // UI
    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.saveCta.setOnClickListener { saveItem() }
        binding.takePictureCta.setOnClickListener { takePicture() }
    }

    // MÁSCARA DE PLACA
    private fun setupMask() {
        binding.licence.addTextChangedListener(object : TextWatcher {

            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true

                val input = s.toString()
                    .uppercase()
                    .replace("[^A-Z0-9]".toRegex(), "")

                val formatted = when {
                    input.length <= 3 -> input

                    input.length <= 7 -> {
                        val first = input.substring(0, 3)
                        val rest = input.substring(3)

                        if (rest.isNotEmpty() && rest[0].isDigit()) {
                            "$first-$rest"
                        } else {
                            "$first$rest"
                        }
                    }

                    else -> input.substring(0, 7)
                }

                binding.licence.setText(formatted)
                binding.licence.setSelection(formatted.length)

                isUpdating = false
            }
        })
    }

    // NOVO — MÁSCARA DO ANO
    private fun setupYearMask() {
        binding.year.addTextChangedListener(object : TextWatcher {

            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true

                val input = s.toString()
                    .replace("[^0-9]".toRegex(), "")

                val formatted = when {
                    input.length <= 4 -> input

                    input.length <= 8 -> {
                        val first = input.substring(0, 4)
                        val second = input.substring(4)
                        "$first/$second"
                    }

                    else -> input.substring(0, 8)
                }

                binding.year.setText(formatted)
                binding.year.setSelection(formatted.length)

                // VALIDAÇÃO REAL
                if (formatted.length == 9) {
                    val parts = formatted.split("/")

                    val year1 = parts[0].toIntOrNull()
                    val year2 = parts[1].toIntOrNull()
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                    val isValid =
                        year1 != null && year2 != null &&
                                year1 in 1900..currentYear &&
                                year2 in 1900..currentYear &&
                                year2 >= year1

                    if (!isValid) {
                        binding.year.error = "Ano inválido"
                    } else {
                        binding.year.error = null
                    }
                }

                isUpdating = false
            }
        })
    }

    // CAMERA
    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val file = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )

        imageFile = file

        return FileProvider.getUriForFile(
            this,
            "com.vrumsync.app.fileprovider",
            file
        )
    }

    // SAVE
//    private fun saveItem() {
//        if (!validateForm()) return
//        uploadImageToFirebase()
//    }

    private fun saveItem() {
        if (!validateForm()) return

        // SE já tem URL digitada → salva direto
        if (!binding.imageUrl.text.isNullOrBlank() && imageFile == null) {
            saveData()
        } else {
            // SE tirou foto → faz upload
            uploadImageToFirebase()
        }
    }

    private fun saveData() {
        val name = binding.name.text.toString()
        val year = binding.year.text.toString()
        val licence = binding.licence.text.toString()

        val location = selectedMarker?.position?.let {
            Place(it.latitude, it.longitude)
        }

        val car = Car(
            id = UUID.randomUUID().toString(),
            name = name,
            imageUrl = binding.imageUrl.text.toString(),
            year = year,
            licence = licence,
            place = location
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.addItem(car)
            }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@NewItemActivity, "Carro salvo!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(this@NewItemActivity, "Erro ao salvar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // FIREBASE
    private fun uploadImageToFirebase() {
        val file = imageFile ?: return

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        val bitmap = BitmapFactory.decodeFile(file.path) ?: return
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        onLoadImage(true)

        imageRef.putBytes(baos.toByteArray())
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    binding.imageUrl.setText(uri.toString())
                    onLoadImage(false)
                    saveData()
                }
            }
            .addOnFailureListener {
                onLoadImage(false)
                Toast.makeText(this, "Erro no upload", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateForm(): Boolean {
        var error = false

        if (binding.name.text.isNullOrBlank()) {
            binding.name.error = "Obrigatório"
            error = true
        }

        if (binding.year.text.isNullOrBlank()) {
            binding.year.error = "Obrigatório"
            error = true
        }

        if (binding.licence.text.isNullOrBlank()) {
            binding.licence.error = "Obrigatório"
            error = true
        }

        if (binding.imageUrl.text.isNullOrBlank()) {
            binding.imageUrl.error = "Obrigatório"
            error = true
        }

        // VALIDAÇÃO DO MAPA
        if (selectedMarker == null) {
            Toast.makeText(this, "Selecione a localização no mapa", Toast.LENGTH_SHORT).show()
            error = true
        }

        return !error
    }

    private fun setupGoogleMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        binding.mapContent.visibility = View.VISIBLE

        val defaultLocation = LatLng(-23.55, -46.63)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Local selecionado")
                    .draggable(true)
            )
        }
    }

    private fun onLoadImage(isLoading: Boolean) {
        binding.loadImageProgress.visibility =
            if (isLoading) View.VISIBLE else View.GONE
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002

        fun newIntent(context: Context): Intent {
            return Intent(context, NewItemActivity::class.java)
        }
    }
}