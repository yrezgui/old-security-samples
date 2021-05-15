package com.samples.appinstaller.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.samples.appinstaller.databinding.FragmentLibraryBinding

class LibraryFragment : Fragment() {
    private val viewModel: LibraryViewModel by viewModels()

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = LibraryRecyclerViewAdapter(emptyList())

        viewModel.loadInstalledApps()
        viewModel.installedApps.observe(viewLifecycleOwner) { installedApps ->
            binding.recyclerView.adapter = LibraryRecyclerViewAdapter(installedApps)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}