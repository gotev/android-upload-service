package net.gotev.uploadservicedemo.views;

import android.view.View;

import net.gotev.recycleradapter.RecyclerAdapterViewHolder;

import butterknife.ButterKnife;

/**
 * @author Aleksandar Gotev
 */

public abstract class ButterKnifeViewHolder extends RecyclerAdapterViewHolder {
    public ButterKnifeViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
