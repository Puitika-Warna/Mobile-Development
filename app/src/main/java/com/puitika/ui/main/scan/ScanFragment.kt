package com.puitika.ui.main.scan

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.TranslateAnimation
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.puitika.R
import com.puitika.data.remote.response.PrediksiItem
import com.puitika.databinding.FragmentPopupBinding
import com.puitika.databinding.FragmentScanBinding
import com.puitika.factory.ViewModelFactory
import com.puitika.ui.profile.ProfileActivity
import com.puitika.utils.Result
import com.puitika.utils.getImageUri
import com.puitika.utils.uriToFile
import com.puitika.utils.showToast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * A simple [Fragment] subclass.
 * Use the [ScanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScanFragment : Fragment() {
    private lateinit var binding: FragmentScanBinding
    private lateinit var bindingPopup: FragmentPopupBinding
    private lateinit var factory: ViewModelFactory
    private val viewModel: ScanViewModel by viewModels { factory }
    private var currentImageUri: Uri? = null
    private val handler = Handler()
    private var dialog: Dialog? = null
    private var loadingPopup: PopupWindow? = null
    private var popupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnScan.visibility = View.GONE
        binding.ivPhoto.visibility = View.GONE
        binding.layoutResult.visibility = View.GONE
        binding.layoutIvPhoto.visibility = View.GONE
        showScanOptionsPopup()
        setViewModelFactory()

        setAction()
        binding.btnScan.setOnClickListener {
            processImage()
        }
    }

    private fun setViewModelFactory() {
        factory = ViewModelFactory.getInstance(binding.root.context)
    }

    private fun processImage() {
        dialog = Dialog(requireActivity())
        currentImageUri?.let { uri ->
            val file = uriToFile(uri, requireActivity())
            val requestFile = file.asRequestBody("image/jpeg".toMediaType())
            val multiPartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
            viewModel.scanCloth(multiPartBody).observe(requireActivity()) { result ->
                when (result) {
                    is Result.Loading -> {
                        showLoadingDialog(true)
                    }

                    is Result.Error -> {
                        showLoadingDialog(true)
                        showCustomDialog(result.data, false)
                        binding.btnScan.visibility = View.VISIBLE
                    }

                    is Result.Success -> {
                        binding.btnScan.visibility = View.GONE
                        handler.postDelayed({
                            showLoadingDialog(false)
                            showCustomDialog("Classification Success!", true)
                            handler.postDelayed({
                                showResult(result.data.prediksi)
                                binding.layoutResult.visibility = View.VISIBLE
                            }, 2000)
                        }, 7000)
                    }
                }
            }
        }
    }

    private fun showResult(scanModelList: List<PrediksiItem>) {
        val adapter = ScanModelAdapter(requireActivity(), scanModelList)

        // Use GridLayoutManager with 2 columns
        val layoutManager = GridLayoutManager(requireActivity(), 2)

        binding.rvScan.layoutManager = layoutManager
        binding.rvScan.adapter = adapter
        binding.rvScan.visibility = View.VISIBLE
    }


    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        } else {
            showToast(requireActivity(), "No Photo Captured")
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            binding.ivPhoto.setPadding(0, 0, 0, 0)
            binding.ivPhoto.setImageURI(it)
            binding.btnScan.visibility = View.VISIBLE
            binding.ivPhoto.visibility = View.VISIBLE
            binding.layoutIvPhoto.visibility = View.VISIBLE
        }
    }

    private fun showScanOptionsPopup() {
        bindingPopup = FragmentPopupBinding.inflate(layoutInflater)
        popupWindow = createPopupWindow()

        with(bindingPopup) {
            btnCamera.setOnClickListener {
                popupWindow?.dismiss()
                currentImageUri = getImageUri(requireActivity())
                launcherIntentCamera.launch(currentImageUri)
            }

            btnGalery.setOnClickListener {
                popupWindow?.dismiss()
                launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            layoutBtnClose.setOnClickListener {
                popupWindow?.dismiss()
            }

            ivClose.setOnClickListener {
                popupWindow?.dismiss()
            }

            imageViewCloseup.setOnClickListener {
                popupWindow?.dismiss()
            }

            setPopupAnimation(root, popupWindow!!)
        }

        popupWindow?.showAtLocation(bindingPopup.root, Gravity.CENTER, 0, 0)
    }

    private fun createPopupWindow(): PopupWindow {
        return PopupWindow(
            bindingPopup.root,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        ).apply {
            setBackgroundDrawable(resources.getDrawable(R.drawable.popup_background))
        }
    }

    private fun setPopupAnimation(view: View, popupWindow: PopupWindow) {
        val translateAnimation = TranslateAnimation(0f, 0f, popupWindow.height.toFloat(), 0f)
        translateAnimation.duration = 500
        view.startAnimation(translateAnimation)
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            showToast(requireActivity(), "No Media Selected")
        }
    }

    private fun setAction() {
        binding.topNavigation.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_account -> {
                    startActivity(Intent(requireActivity(), ProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    private fun showCustomDialog(message: String, success: Boolean) {
        val popupView = layoutInflater.inflate(R.layout.fragment_popup_loggedin, null)

        // Find views in the custom layout
        val messageTextView: TextView = popupView.findViewById(R.id.tv_loggedin)
        val checkListImageView: ImageFilterView = popupView.findViewById(R.id.iv_checklist)
        val cancelImageView: ImageFilterView = popupView.findViewById(R.id.iv_cancel)

        // Set message and visibility based on success
        messageTextView.text = message
        if (success) {
            checkListImageView.visibility = View.VISIBLE
            cancelImageView.visibility = View.GONE
        } else {
            checkListImageView.visibility = View.GONE
            cancelImageView.visibility = View.VISIBLE
        }

        // Create a PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        // Set background drawable with a transparent color
        popupWindow.setBackgroundDrawable(resources.getDrawable(android.R.color.transparent))

        // Show the PopupWindow at the center of the screen
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        // Dismiss the PopupWindow after a delay
        handler.postDelayed({
            popupWindow.dismiss()
        }, 2000)
    }


    private fun showLoadingDialog(play: Boolean = true) {
        if (play) {
            // Show loading popup
            if (loadingPopup == null) {
                val loadingView = layoutInflater.inflate(R.layout.scan_loading2, null)
                loadingPopup = PopupWindow(
                    loadingView,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                loadingPopup?.setBackgroundDrawable(resources.getDrawable(android.R.color.transparent))
                loadingPopup?.isOutsideTouchable = false
                loadingPopup?.isFocusable = true
            }
            loadingPopup?.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
        } else {
            // Dismiss loading popup
            loadingPopup?.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        popupWindow?.dismiss()
        handler.removeCallbacksAndMessages(null)
    }
}
