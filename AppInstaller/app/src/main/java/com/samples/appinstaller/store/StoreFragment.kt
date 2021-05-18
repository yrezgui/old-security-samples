package com.samples.appinstaller.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.databinding.FragmentStoreBinding

class StoreFragment : Fragment() {
    private val appViewModel: AppViewModel by activityViewModels()

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)

        val adapter = StoreRecyclerViewAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        appViewModel.store.observe(viewLifecycleOwner) {
            adapter.updateData(it.values.toList())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}