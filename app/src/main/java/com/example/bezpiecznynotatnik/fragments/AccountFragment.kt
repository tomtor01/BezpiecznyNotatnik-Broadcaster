package com.example.bezpiecznynotatnik.fragments

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager
import kotlinx.coroutines.launch

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var btnSignIn: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    private lateinit var googleDriveManager: GoogleDriveBackupManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        btnSignIn = view.findViewById(R.id.signUpButton)
        btnExport = view.findViewById(R.id.btnExport)
        btnImport = view.findViewById(R.id.btnImport)

        googleDriveManager = (requireActivity().applicationContext as SecureNotesApp).googleDriveManager
        googleDriveManager.initializeGoogleSignIn(requireActivity())
        setupUI()
    }

    private fun setupUI() {
        // Set up Sign-In button
        btnSignIn.setOnClickListener {
            val signInIntent = googleDriveManager.getSignInIntent()
            signInLauncher.launch(signInIntent)
        }

        // Set up Export button
        btnExport.setOnClickListener {
            lifecycleScope.launch {
                if (!googleDriveManager.isDriveServiceInitialized()) {
                    Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    googleDriveManager.uploadDatabase(requireActivity().applicationContext as SecureNotesApp)
                    Toast.makeText(requireContext(), "Database uploaded successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set up Import button
        btnImport.setOnClickListener {
            lifecycleScope.launch {
                if (!googleDriveManager.isDriveServiceInitialized()) {
                    Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    googleDriveManager.downloadDatabase(requireActivity().applicationContext as SecureNotesApp)
                    Toast.makeText(requireContext(), "Database restored successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handle sign-in result
    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                googleDriveManager.handleSignInResult(requireContext(),
                    data = result.data,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Sign-In successful!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(requireContext(), "Sign-In failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Sign-In canceled or failed.", Toast.LENGTH_SHORT).show()
            }
        }
}