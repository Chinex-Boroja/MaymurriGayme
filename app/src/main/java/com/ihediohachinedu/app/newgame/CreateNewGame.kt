package com.ihediohachinedu.app.newgame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ihediohachinedu.app.adapters.ImageSelectorAdapter
import com.ihediohachinedu.app.R
import com.ihediohachinedu.app.models.BoardSize
import com.ihediohachinedu.app.utils.*
import java.io.ByteArrayOutputStream

class CreateNewGame : AppCompatActivity() {

     companion object {
         private const val TAG = "CreateNewGame"
         private const val PICK_PHOTO_CODE = 208
         private const val READ_EXTERNAL_PHOTOS_CODE = 343
         private const val READ_PHOTO_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
         private const val MIN_GAME_LENGTH = 4
         private const val MAX_GAME_LENGTH = 15
     }

    //Member variable to be referenced across different files
    private lateinit var boardSize: BoardSize
    private var numberOfImagesRequired = -1

    private lateinit var rvImageSelector: RecyclerView
    private lateinit var eTGameName: EditText
    private lateinit var buttonSave: Button
    private lateinit var pBarUpload: ProgressBar

    private lateinit var adapter: ImageSelectorAdapter

    private val selectedImageUris = mutableListOf<Uri>()
    //getting a reference from Firebase storage and fire store
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_game)

        rvImageSelector = findViewById(R.id.imageSelector)
        eTGameName = findViewById(R.id.eTGameName)
        buttonSave = findViewById(R.id.buttonSave)
        pBarUpload = findViewById(R.id.pBarUpload)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numberOfImagesRequired = boardSize.getNumberOfPairs()
        supportActionBar?.title = "Select images (0 / $numberOfImagesRequired)"

        //save game to firebase
        buttonSave.setOnClickListener {
            saveDataToFirebase()
        }

        eTGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))
        eTGameName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                buttonSave.isEnabled = shouldEnableSaveButton()
            }
        })

        adapter = ImageSelectorAdapter(this, selectedImageUris, boardSize, object: ImageSelectorAdapter.ImageClickListener {
            override fun onPlaceholderClicked() {
                if (isPermissionGranted(this@CreateNewGame, READ_PHOTO_PERMISSION)) {
                    launchIntentForPhotos()
                } else {
                    requestPermission(this@CreateNewGame, READ_PHOTO_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })
        rvImageSelector.adapter = adapter
        rvImageSelector.setHasFixedSize(true)
        rvImageSelector.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            }else {
                Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //check if the request code matches and return early if any of the conditions are met
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
            return
        }
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            Log.i(TAG, "clipData numberOfImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (selectedImageUris.size < numberOfImagesRequired) {
                    selectedImageUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            selectedImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Select images (${selectedImageUris.size} / $numberOfImagesRequired)"
        buttonSave.isEnabled = shouldEnableSaveButton()
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        buttonSave.isEnabled = false
        val customGameName = eTGameName.text.toString()
        //Check that we are not overwriting someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game already exist with the name '$customGameName'. Please type another name")
                    .setPositiveButton("OK", null)
                    .show()
                buttonSave.isEnabled = true
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Encountered error while saving game", exception)
            Toast.makeText(this, "Encountered error while saving game", Toast.LENGTH_SHORT).show()
            buttonSave.isEnabled = true
        }
    }

    private fun handleImageUploading(customGameName: String) {
        pBarUpload.visibility = View.VISIBLE
        //Logic for downscaling the images for the games
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        //iterate over the selected image Uris with index and elements are imageUri
        for ((index, imageUri) in selectedImageUris.withIndex())  {
            //imageByteArray is going to be uploaded to Firebase
            val imageByteArray = getImageByteArray(imageUri)
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            val imageReference = storage.reference.child(filePath)
            imageReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    imageReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG, "Exception with Firebase storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        pBarUpload.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pBarUpload.progress = uploadedImageUrls.size * 100 / selectedImageUris.size
                    Log.i(TAG, "Finished uploading $imageUri, num uploaded ${uploadedImageUrls.size}")
                    if (uploadedImageUrls.size == selectedImageUris.size) {
                        handleAllImagesUploaded(customGameName, uploadedImageUrls)
                    }
                }
        }

    }

    private fun handleAllImagesUploaded(
        customGameName: String,
        uploadedImageUrls: MutableList<String>
    ) {
        // TODO: upload this info to Firestore
        db.collection("games").document(customGameName)
            .set(mapOf("images" to uploadedImageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pBarUpload.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $customGameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play your game '$customGameName'")
                    .setPositiveButton("Ok") {_,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, customGameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    //This method is going to take care of all the downgrading
    private fun getImageByteArray(imageUri: Uri): ByteArray {
        //get the original bitmap, based on the photo Uri depending on the Api version of the phone
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            //If we are running the lower version of the phone
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaleBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaleBitmap.width} and height ${scaleBitmap.height}")

        val byteOutputStream = ByteArrayOutputStream()
        scaleBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun shouldEnableSaveButton(): Boolean {
        // This is to check if we can enable the save button or not
        if (selectedImageUris.size != numberOfImagesRequired) {
            return false
        }
        if (eTGameName.text.isBlank() || eTGameName.text.length < MIN_GAME_LENGTH) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // needs access to files on the phone
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
    }
}