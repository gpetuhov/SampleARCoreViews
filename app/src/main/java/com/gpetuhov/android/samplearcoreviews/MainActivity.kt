package com.gpetuhov.android.samplearcoreviews

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.pawegio.kandroid.toast

class MainActivity : AppCompatActivity() {

    companion object {
        const val MIN_OPENGL_VERSION = 3.0
    }

    private var arFragment: ArFragment? = null
    private var viewRenderable: ViewRenderable? = null
    private var redSphereRenderable: ModelRenderable? = null
    private var anchorNode: AnchorNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_main)

        loadView()
        generateSphere()
        initArFragment()
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

    private fun loadView() {
        // Create renderable from the view layout.
        // This will be rendered as flat card in the scene.
        ViewRenderable.builder()
            .setView(this, R.layout.controls_view)
            .build()
            .thenAccept { renderable ->
                viewRenderable = renderable
                initControls(renderable.view)
            }
            .exceptionally { throwable ->
                toast("Unable to load renderable")
                null
            }
    }

    private fun initControls(view: View) {
        // Find views and set click listeners as usual
        val generateButton = view.findViewById<Button>(R.id.generateButton)
        generateButton?.setOnClickListener { addSphereToScene() }
    }

    private fun initArFragment() {
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment?.setOnTapArPlaneListener(::onPlaneTap)
    }

    // Show ViewRenderable in the place of tap
    private fun onPlaneTap(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (viewRenderable == null) {
            return
        }

        val anchor = hitResult.createAnchor()

        // We keep the anchor at hit point,
        // so that we are able to add our red sphere to it later.
        anchorNode = AnchorNode(anchor)
        anchorNode?.setParent(arFragment?.arSceneView?.scene)

        val model = TransformableNode(arFragment?.transformationSystem)
        model.setParent(anchorNode)
        model.renderable = viewRenderable

        // This places our view a little above the plane
        model.localPosition = Vector3(0.0f, 0.1f, 0.0f)

        model.select()
    }

    // Programmatically models are created in advance like views and models from assets
    private fun generateSphere() {
        MaterialFactory
            .makeOpaqueWithColor(this, Color(android.graphics.Color.RED))
            .thenAccept { material ->
                // Center of this sphere is positioned at R above the plane, where R is radius
                // (this is needed, so that sphere will lay upon the plane).
                redSphereRenderable = ShapeFactory.makeSphere(0.05f, Vector3(0.0f, 0.05f, 0.0f), material)
            }
    }

    private fun addSphereToScene() {
        if (anchorNode == null || redSphereRenderable == null) {
            return
        }

        // Add programmatically created sphere to previously saved anchor
        val sphere = Node()
        sphere.setParent(anchorNode)
        sphere.renderable = redSphereRenderable
    }
}
