package com.gpetuhov.android.samplearcoreviews

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.pawegio.kandroid.toast

class MainActivity : AppCompatActivity() {

    companion object {
        const val MIN_OPENGL_VERSION = 3.0
    }

    private var arFragment: ArFragment? = null
    private var viewRenderable: ViewRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment

        // TODO: refactor this

        ViewRenderable.builder()
            .setView(this, R.layout.controls_view)
            .build()
            .thenAccept { renderable ->
                viewRenderable = renderable

                val controlsView = renderable.view
                val hello = controlsView.findViewById<TextView>(R.id.hello)
                hello.setOnClickListener { toast("Hello") }
            }
            .exceptionally { throwable ->
                toast("Unable to load renderable")
                null
            }

        arFragment?.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (viewRenderable == null) {
                return@setOnTapArPlaneListener
            }

            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment?.arSceneView?.scene)

            val model = TransformableNode(arFragment?.transformationSystem)
            model.setParent(anchorNode)
            model.renderable = viewRenderable
            model.select()
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * Finishes the activity if Sceneform can not run
     */
    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            toast("Sceneform requires Android N or later")
            activity.finish()
            return false
        }

        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion

        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            toast("Sceneform requires OpenGL ES 3.0 or later")
            activity.finish()
            return false
        }

        return true
    }
}
