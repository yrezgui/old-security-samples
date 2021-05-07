package com.samples.appinstaller.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.samples.appinstaller.databinding.FragmentAppDetailsBinding

class AppDetailsFragment : Fragment() {
    private val viewModel: AppDetailsViewModel by viewModels()

    private var _binding: FragmentAppDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: AppDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDetailsBinding.inflate(inflater, container, false)

        viewModel.loadApp(args.packageId)
        initFragment()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initFragment() {
        binding.appName.text = viewModel.selectedApp.name
        binding.company.text = viewModel.selectedApp.company
        binding.icon.setImageResource(viewModel.selectedApp.icon)
        binding.cancelButton.visibility = View.GONE
        binding.openButton.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }
}