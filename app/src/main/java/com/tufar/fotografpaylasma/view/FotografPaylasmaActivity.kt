package com.tufar.fotografpaylasma.view

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tufar.fotografpaylasma.databinding.ActivityFotografPaylasmaBinding
import kotlinx.android.synthetic.main.activity_fotograf_paylasma.*
import java.io.IOException
import java.util.*

class FotografPaylasmaActivity : AppCompatActivity() {
    var secilenGorsel : Uri? = null
    var secilenBitmap : Bitmap? = null
    private lateinit var binding: ActivityFotografPaylasmaBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var storage: FirebaseStorage
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_fotograf_paylasma)

        binding = ActivityFotografPaylasmaBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
    }

    fun paylas(view: View){
        // depo işlemleri
        // UUID -> universal unique id (evrensel benzersiz id)

        val uuid = UUID.randomUUID()
        val gorselIsmi = "${uuid}.jpg"

        val reference = storage.reference

        val gorselReference = reference.child("images").child(gorselIsmi)

        if (secilenGorsel != null){
            gorselReference.putFile(secilenGorsel!!).addOnSuccessListener { taskSnapshot ->
                // Depoya yüklenen görselin linki alındı ve o link ile veritabanına kayıt edilecek.
                val yuklenenGorselReference = FirebaseStorage.getInstance().reference.child("images").child(gorselIsmi)
                yuklenenGorselReference.downloadUrl.addOnSuccessListener { uri ->
                    // Veritabanı işlemleri
                    val downloadUrl = uri.toString()
                    val guncelKullaniciEmail = auth.currentUser!!.email.toString()
                    val kullaniciYorumu = yorumText.text.toString()
                    val tarih = com.google.firebase.Timestamp.now()

                    val postHashMap = hashMapOf<String,Any>()

                    if (kullaniciYorumu == null){
                        Toast.makeText(this@FotografPaylasmaActivity,"Boş yorum kabul edilemez!",Toast.LENGTH_LONG).show()
                    }
                    else{

                        postHashMap.put("gorselurl",downloadUrl)
                        postHashMap.put("kullaniciemail",guncelKullaniciEmail)
                        postHashMap.put("tarih",tarih)
                        postHashMap.put("kullaniciyorum",kullaniciYorumu)

                        database.collection("Post").add(postHashMap).addOnCompleteListener{ task ->
                            if (task.isSuccessful){
                                finish()
                            }
                        }.addOnFailureListener { exception ->
                            Toast.makeText(applicationContext,exception.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(applicationContext,exception.localizedMessage,Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    fun gorselSec(view: View){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                    View.OnClickListener {
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            val galeriIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(galeriIntent)

        }
/*
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            // izin alınmadıysa = izin istenir
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1)
        }
        else{
            // izin zaten varsa yapılacaklar
            val galeriIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(galeriIntent)

        }

 */
    }

/*
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1){
            if(grantResults.size>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // izin verilince yapılacaklar
                val galeriIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(galeriIntent)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
 */
    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    secilenGorsel = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(
                                this@FotografPaylasmaActivity.contentResolver,
                                secilenGorsel!!
                            )
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        } else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(
                                this@FotografPaylasmaActivity.contentResolver,
                                secilenGorsel
                            )
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { result ->
            if (result) {
                //permission granted
                val galeriIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(galeriIntent)
            } else {
                //permission denied
                Toast.makeText(this@FotografPaylasmaActivity, "Permisson needed!", Toast.LENGTH_LONG).show()
            }
        }
    }

        /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null){
            secilenGorsel = data.data

            if(secilenGorsel != null){
                if (Build.VERSION.SDK_INT >= 28){
                    val source = ImageDecoder.createSource(this .contentResolver,secilenGorsel!!)
                    secilenBitmap = ImageDecoder.decodeBitmap(source)
                    imageView.setImageBitmap(secilenBitmap)
                }
                else{
                    secilenBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,secilenGorsel)
                    imageView.setImageBitmap(secilenBitmap)
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }
         */
}