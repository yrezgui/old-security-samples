package com.samples.appinstaller.app

import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("mipmap")
fun setPaddingLeft(image: ImageView, resourceId: Int) {
    image.setImageResource(resourceId)
}