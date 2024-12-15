package com.example.gestureapp

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private lateinit var rotateGestureDetector: RotateGestureDetector // Added rotation gesture detector

    private lateinit var database: DatabaseReference // Firebase Database reference

    private var scaleFactor = 1.0f
    private var rotationValue = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        imageView = findViewById(R.id.imageView)
        imageView.isClickable = true

        // Load the saved image state from Firebase
        loadImageState()

        // Setup pinch-to-zoom
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
                imageView.scaleX = scaleFactor
                imageView.scaleY = scaleFactor
                saveStateToFirebase() // Save state to Firebase
                return true
            }
        })

        // Setup gesture detection
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Toast.makeText(this@MainActivity, "Single Tap", Toast.LENGTH_SHORT).show()
                imageView.performClick()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                scaleFactor = 1.0f
                imageView.scaleX = scaleFactor
                imageView.scaleY = scaleFactor
                saveStateToFirebase() // Save state to Firebase
                Toast.makeText(this@MainActivity, "Double Tap: Reset Zoom", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        // Setup rotation gesture detector
        rotateGestureDetector = RotateGestureDetector(object : RotateGestureDetector.OnRotationGestureListener {
            override fun onRotation(rotationAngle: Float) {
                rotationValue += rotationAngle
                imageView.rotation = rotationValue
                saveStateToFirebase() // Save state to Firebase
            }
        })

        // Combine all gestures
        imageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            rotateGestureDetector.onTouchEvent(event) // Handle rotation gestures
            true
        }
    }

    // Load the saved image state from Firebase
    private fun loadImageState() {
        database.child("imageState").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    scaleFactor = snapshot.child("scaleFactor").getValue(Float::class.java) ?: 1.0f
                    rotationValue = snapshot.child("rotationValue").getValue(Float::class.java) ?: 0.0f

                    imageView.scaleX = scaleFactor
                    imageView.scaleY = scaleFactor
                    imageView.rotation = rotationValue

                    Toast.makeText(this@MainActivity, "Image state loaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "No saved image state found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load image state: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Save the current scale and rotation state to Firebase
    private fun saveStateToFirebase() {
        val state = mapOf(
            "scaleFactor" to scaleFactor,
            "rotationValue" to rotationValue
        )
        database.child("imageState").setValue(state)
            .addOnSuccessListener {
                Toast.makeText(this, "State saved to Firebase", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save state", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

// RotateGestureDetector class
class RotateGestureDetector(private val listener: OnRotationGestureListener) {

    interface OnRotationGestureListener {
        fun onRotation(rotationAngle: Float)
    }

    private var previousAngle: Float = 0f

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            val deltaX = (event.getX(1) - event.getX(0)).toDouble()
            val deltaY = (event.getY(1) - event.getY(0)).toDouble()
            val angle = Math.toDegrees(Math.atan2(deltaY, deltaX)).toFloat()

            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                val deltaAngle = angle - previousAngle
                listener.onRotation(deltaAngle)
            }
            previousAngle = angle
        }
        return true
    }
}
