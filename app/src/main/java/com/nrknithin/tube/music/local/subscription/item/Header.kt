package com.nrknithin.tube.music.local.subscription.item

import android.view.View
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.databinding.SubscriptionHeaderBinding
import com.xwray.groupie.viewbinding.BindableItem

class Header(private val title: String) : BindableItem<SubscriptionHeaderBinding>() {

    override fun getLayout(): Int = R.layout.subscription_header

    override fun bind(viewBinding: SubscriptionHeaderBinding, position: Int) {
        viewBinding.root.text = title
    }

    override fun initializeViewBinding(view: View) = SubscriptionHeaderBinding.bind(view)
}
