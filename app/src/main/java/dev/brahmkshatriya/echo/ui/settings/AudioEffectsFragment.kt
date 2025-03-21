package dev.brahmkshatriya.echo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.PagerSnapHelper
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentAudioFxBinding
import dev.brahmkshatriya.echo.databinding.FragmentSettingsContainerBinding
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.deleteGlobalFx
import dev.brahmkshatriya.echo.ui.player.AudioEffectsBottomSheet.Companion.bind
import dev.brahmkshatriya.echo.ui.player.AudioEffectsBottomSheet.Companion.onEqualizerClicked
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class AudioEffectsFragment : Fragment() {

    private var binding: FragmentSettingsContainerBinding by autoCleared()
    private val fragment = AudioFxFragment()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.title.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.title.title = getString(R.string.audio_fx)
        childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment)
            .commit()

        binding.title.inflateMenu(R.menu.audio_fx)
        binding.title.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.reset -> {
                    val context = requireContext()
                    context.deleteGlobalFx()
                    fragment.bind()
                    true
                }

                else -> false
            }
        }
    }

    class AudioFxFragment : Fragment() {
        var binding by autoCleared<FragmentAudioFxBinding>()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentAudioFxBinding.inflate(inflater, container, false)
            return binding.root
        }

        fun bind() = binding.bind(requireContext()) { onEqualizerClicked() }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            PagerSnapHelper().attachToRecyclerView(binding.speedRecycler)
            bind()
            binding.root.apply {
                clipToPadding = false
                applyInsets { applyContentInsets(it) }
                isVerticalScrollBarEnabled = false
                FastScrollerHelper.applyTo(this)
            }
        }
    }
}