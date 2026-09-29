package com.nrknithin.tube.music.local.subscription.item

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.database.subscription.SubscriptionEntity
import com.nrknithin.tube.music.databinding.PickerSubscriptionItemBinding
import com.nrknithin.tube.music.ktx.AnimationType
import com.nrknithin.tube.music.ktx.animate
import com.nrknithin.tube.music.util.image.CoilHelper
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder

data class PickerSubscriptionItem(
    val subscriptionEntity: SubscriptionEntity,
    var isSelected: Boolean = false
) : BindableItem<PickerSubscriptionItemBinding>() {
    override fun getId(): Long = subscriptionEntity.uid
    override fun getLayout(): Int = R.layout.picker_subscription_item
    override fun getSpanSize(spanCount: Int, position: Int): Int = 1

    override fun bind(viewBinding: PickerSubscriptionItemBinding, position: Int) {
        CoilHelper.loadAvatar(viewBinding.thumbnailView, subscriptionEntity.avatarUrl)
        viewBinding.titleView.text = subscriptionEntity.name
        viewBinding.selectedHighlight.isVisible = isSelected
    }

    override fun unbind(viewHolder: GroupieViewHolder<PickerSubscriptionItemBinding>) {
        super.unbind(viewHolder)

        viewHolder.binding.selectedHighlight.apply {
            animate().setListener(null).cancel()
            isGone = true
            alpha = 1F
        }
    }

    override fun initializeViewBinding(view: View) = PickerSubscriptionItemBinding.bind(view)

    fun updateSelected(containerView: View, isSelected: Boolean) {
        this.isSelected = isSelected
        PickerSubscriptionItemBinding.bind(containerView).selectedHighlight
            .animate(isSelected, 150, AnimationType.LIGHT_SCALE_AND_ALPHA)
    }
}
