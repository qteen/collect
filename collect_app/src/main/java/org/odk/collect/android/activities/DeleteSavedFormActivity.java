package org.odk.collect.android.activities;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.odk.collect.android.R;
import org.odk.collect.android.adapters.DataManagementTabsAdapter;
import org.odk.collect.android.adapters.DeleteFormsTabsAdapter;
import org.odk.collect.android.formmanagement.BlankFormsListViewModel;
import org.odk.collect.android.injection.DaggerUtils;

import javax.inject.Inject;

public class DeleteSavedFormActivity extends CollectAbstractActivity {
    @Inject
    BlankFormsListViewModel.Factory viewModelFactory;
    BlankFormsListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerUtils.getComponent(this).inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(BlankFormsListViewModel.class);

        setContentView(R.layout.tabs_layout);
        initToolbar(getString(R.string.delete_files));
        setUpViewPager();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.notes);
    }

    private void setUpViewPager() {
        String[] tabNames = {getString(R.string.data), getString(R.string.forms)};
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager.setAdapter(new DeleteFormsTabsAdapter(this, viewModel.isMatchExactlyEnabled()));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabNames[position])).attach();
    }
}
