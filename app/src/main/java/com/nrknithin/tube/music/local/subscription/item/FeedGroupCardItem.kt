package com.nrknithin.tube.music.local.subscription.item

import android.view.View
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.database.feed.model.FeedGroupEntity
import com.nrknithin.tube.music.databinding.FeedGroupCardItemBinding
import com.nrknithin.tube.music.local.subscription.FeedGroupIcon
import com.xwray.groupie.viewbinding.BindableItem

data class FeedGroupCardItem(
    val groupId: Long = FeedGroupEntity.GROUP_ALL_ID,
    val name: String,
    val icon: FeedGroupIcon
) : BindableItem<FeedGroupCardItemBinding>() {
    constructor (feedGroupEntity: FeedGroupEntity) : this(feedGroupEntity.uid, feedGroupEntity.name, feedGroupEntity.icon)

    override fun getId(): Long {
        return when (groupId) {
            FeedGroupEntity.GROUP_ALL_ID -> super.getId()
            else -> groupId
        }
    }

    override fun getLayout(): Int = R.layout.feed_group_card_item

    override fun bind(viewBinding: FeedGroupCardItemBinding, position: Int) {
        viewBinding.title.text = name
        viewBinding.icon.setImageResource(icon.getDrawableRes())
    }

    override fun initializeViewBinding(view: View) = FeedGroupCardItemBinding.bind(view)
}
