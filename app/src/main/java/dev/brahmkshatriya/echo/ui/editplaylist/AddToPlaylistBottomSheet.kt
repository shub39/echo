package dev.brahmkshatriya.echo.ui.editplaylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.databinding.DialogAddToPlaylistBinding
import dev.brahmkshatriya.echo.databinding.ItemCreatePlaylistBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.adapter.MediaItemSelectableAdapter
import dev.brahmkshatriya.echo.ui.adapter.MediaItemSelectableAdapter.Companion.mediaItemSpanCount
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.library.CreatePlaylistFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.putSerialized
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddToPlaylistBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(clientId: String, item: EchoMediaItem) = AddToPlaylistBottomSheet().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putSerialized("item", item)
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val item: EchoMediaItem by lazy { args.getSerialized("item")!! }

    var binding by autoCleared<DialogAddToPlaylistBinding>()
    val viewModel by viewModels<AddToPlaylistViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.save.setOnClickListener {
            viewModel.saving = true
            binding.loading.root.isVisible = true
            binding.loading.textView.text = getString(R.string.saving)
            binding.recyclerView.isVisible = false
            binding.bottomBar.isVisible = false
            viewModel.addToPlaylists()
        }

        val adapter = MediaItemSelectableAdapter { _, item ->
            item as EchoMediaItem.Lists.PlaylistItem
            viewModel.togglePlaylist(item.playlist)
            binding.save.setEnabled(viewModel.selectedPlaylists.isNotEmpty())
        }

        val createPlaylistAdapter = CreatePlaylistAdapter {
            requireActivity().openFragment(CreatePlaylistFragment())
            dismiss()
        }

        observe(viewModel.playlists) { list ->
            list ?: return@observe
            val visible = viewModel.playlists.value != null || viewModel.saving
            binding.loading.root.isVisible = !visible
            binding.recyclerView.isVisible = visible
            binding.bottomBar.isVisible = visible

            adapter.setItems(
                list.map { it.toMediaItem() },
                viewModel.selectedPlaylists.map { it.toMediaItem() }
            )
        }

        observe(viewModel.dismiss) { dismiss() }
        lifecycleScope.launch {
            val hasCreate =
                viewModel.extensionListFlow.getExtension(clientId)?.isClient<PlaylistEditClient>()
                    ?: false
            val concatAdapter =
                if (hasCreate) ConcatAdapter(createPlaylistAdapter, adapter) else adapter

            binding.recyclerView.adapter = concatAdapter
            binding.recyclerView.mediaItemSpanCount {
                (binding.recyclerView.layoutManager as GridLayoutManager).spanCount = it
            }
            viewModel.clientId = clientId
            viewModel.item = item
            viewModel.onInitialize()
        }
    }

    class CreatePlaylistAdapter(
        private val onClick: () -> Unit
    ) : RecyclerView.Adapter<CreatePlaylistAdapter.ViewHolder>() {

        override fun getItemCount() = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            ItemCreatePlaylistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.root.setOnClickListener { onClick() }
        }

        class ViewHolder(val binding: ItemCreatePlaylistBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}