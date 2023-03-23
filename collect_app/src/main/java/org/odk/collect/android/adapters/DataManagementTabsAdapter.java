package org.odk.collect.android.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.odk.collect.android.fragments.BlankFormListFragment;
import org.odk.collect.android.fragments.DataDownloadListFragment;
import org.odk.collect.android.fragments.SavedFormListFragment;

public class DataManagementTabsAdapter extends FragmentStateAdapter {

    public DataManagementTabsAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DataDownloadListFragment();
            default:
                // should never reach here
                throw new IllegalArgumentException("Fragment position out of bounds");
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}