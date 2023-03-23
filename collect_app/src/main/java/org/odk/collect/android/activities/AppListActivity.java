/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.odk.collect.android.BuildConfig;
import org.odk.collect.android.R;
import org.odk.collect.android.adapters.SortDialogAdapter;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.configure.qr.QRCodeTabsActivity;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.injection.config.AppDependencyComponent;
import org.odk.collect.android.listeners.RecyclerViewClickListener;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.DialogUtils;
import org.odk.collect.android.utilities.MultiClickGuard;
import org.odk.collect.android.utilities.SnackbarUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static org.odk.collect.android.preferences.GeneralKeys.KEY_USERNAME;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.android.utilities.DialogUtils.showIfNotShowing;

abstract class AppListActivity extends CollectAbstractActivity {

    protected static final int LOADER_ID = 0x01;
    private static final String SELECTED_INSTANCES = "selectedInstances";
    private static final String IS_SEARCH_BOX_SHOWN = "isSearchBoxShown";
    private static final String IS_BOTTOM_DIALOG_SHOWN = "isBottomDialogShown";
    private static final String SEARCH_TEXT = "searchText";

    protected static int MENU_NAV_NEW_INDEX = 0;
    protected static int MENU_NAV_EDIT_INDEX = 1;
    protected static int MENU_NAV_SEND_INDEX = 2;
    protected static int MENU_NAV_SENT_INDEX = 3;
    protected static int MENU_NAV_GET_INDEX = 4;

    protected CursorAdapter listAdapter;
    protected LinkedHashSet<Long> selectedInstances = new LinkedHashSet<>();
    protected int[] sortingOptions;
    protected Integer selectedSortingOrder;
    protected ListView listView;
    protected LinearLayout llParent;
    protected ProgressBar progressBar;
    private BottomSheetDialog bottomSheetDialog;
    private boolean isBottomDialogShown;

    private String filterText;
    private String savedFilterText;
    private boolean isSearchBoxShown;

    private SearchView searchView;

    private boolean canHideProgressBar;
    private boolean progressBarVisible;

    protected BottomNavigationView bottomNav;
    private int completedCount;
    private int savedCount;
    private int viewSentCount;

    protected void initBottomNav() {
        bottomNav = findViewById(R.id.bottom_nav);
        initBadges();
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            item.setChecked(false);
            Intent i = null;
            switch (item.getItemId()) {
                case R.id.navigation_new:
                    i = new Intent(getApplicationContext(),
                            FillBlankFormActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    break;
                case R.id.navigation_edit:
                    i = new Intent(getApplicationContext(), InstanceChooserList.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                            ApplicationConstants.FormModes.EDIT_SAVED);
                    startActivity(i);
                    break;
                case R.id.navigation_send:
                    i = new Intent(getApplicationContext(),
                            InstanceUploaderListActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    break;
                case R.id.navigation_sent:
                    i = new Intent(getApplicationContext(), InstanceChooserList.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                            ApplicationConstants.FormModes.VIEW_SENT);
                    startActivity(i);
                    break;
                case R.id.navigation_get:
                    i = new Intent(getApplicationContext(),
                            FormDownloadListActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    break;
            }
            return true;
        });
    }

    private void countSavedForms() {
        InstancesDao instancesDao = new InstancesDao();

        // count for finalized instances
        Cursor finalizedCursor = null;
        try {
            finalizedCursor = instancesDao.getFinalizedInstancesCursor();
        } catch (Exception e) {
            DialogUtils.createErrorDialog(this, e.getMessage(), false);
        } finally {
            completedCount = finalizedCursor != null ? finalizedCursor.getCount() : 0;
            if(finalizedCursor!=null) {
                finalizedCursor.close();
            }
        }

        // count for saved instances
        Cursor savedCursor = null;
        try {
            savedCursor = instancesDao.getUnsentInstancesCursor();
        } catch (Exception e) {
            DialogUtils.createErrorDialog(this, e.getMessage(), false);
        } finally {
            savedCount = savedCursor != null ? savedCursor.getCount() : 0;
            if(savedCursor!=null) {
                savedCursor.close();
            }
        }


        //count for view sent form
        Cursor viewSentCursor = null;
        try {
            viewSentCursor = instancesDao.getSentInstancesCursor();
        } catch (Exception e) {
            DialogUtils.createErrorDialog(this, e.getMessage(), false);
        } finally {
            viewSentCount = viewSentCursor != null ? viewSentCursor.getCount() : 0;
            if(viewSentCursor!=null) {
                viewSentCursor.close();
            }
        }
    }

    private void initBadges() {
        countSavedForms();

        BadgeDrawable editBadge = bottomNav.getOrCreateBadge(R.id.navigation_edit);
        if(savedCount>0) {
            editBadge.setNumber(savedCount);
            editBadge.setVisible(true);
        } else {
            editBadge.clearNumber();
            editBadge.setVisible(false);
        }

        BadgeDrawable sendBadge = bottomNav.getOrCreateBadge(R.id.navigation_send);
        if(completedCount>0) {
            sendBadge.setNumber(completedCount);
            sendBadge.setVisible(true);
        } else {
            sendBadge.clearNumber();
            sendBadge.setVisible(false);
        }

        BadgeDrawable sentBadge = bottomNav.getOrCreateBadge(R.id.navigation_sent);
        if(viewSentCount>0) {
            sentBadge.setNumber(viewSentCount);
            sentBadge.setVisible(true);
        } else {
            sentBadge.clearNumber();
            sentBadge.setVisible(false);
        }
    }

    // toggles to all checked or all unchecked
    // returns:
    // true if result is all checked
    // false if result is all unchecked
    //
    // Toggle behavior is as follows:
    // if ANY items are unchecked, check them all
    // if ALL items are checked, uncheck them all
    public static boolean toggleChecked(ListView lv) {
        // shortcut null case
        if (lv == null) {
            return false;
        }

        boolean newCheckState = lv.getCount() > lv.getCheckedItemCount();
        setAllToCheckedState(lv, newCheckState);
        return newCheckState;
    }

    public static void setAllToCheckedState(ListView lv, boolean check) {
        // no-op if ListView null
        if (lv == null) {
            return;
        }

        for (int x = 0; x < lv.getCount(); x++) {
            lv.setItemChecked(x, check);
        }
    }

    // Function to toggle button label
    public static void toggleButtonLabel(Button toggleButton, ListView lv) {
        if (lv.getCheckedItemCount() != lv.getCount()) {
            toggleButton.setText(R.string.select_all);
        } else {
            toggleButton.setText(R.string.clear_all);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        listView = findViewById(android.R.id.list);
        listView.setOnItemClickListener((AdapterView.OnItemClickListener) this);
        listView.setEmptyView(findViewById(android.R.id.empty));
        progressBar = findViewById(R.id.progressBar);
        llParent = findViewById(R.id.llParent);

        // Use the nicer-looking drawable with Material Design insets.
        listView.setDivider(ContextCompat.getDrawable(this, R.drawable.list_item_divider));
        listView.setDividerHeight(1);

        setSupportActionBar(findViewById(R.id.toolbar));
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreSelectedSortingOrder();
        setupBottomSheet();
        initBadges();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SELECTED_INSTANCES, selectedInstances);
        outState.putBoolean(IS_BOTTOM_DIALOG_SHOWN, bottomSheetDialog.isShowing());

        if (searchView != null) {
            outState.putBoolean(IS_SEARCH_BOX_SHOWN, !searchView.isIconified());
            outState.putString(SEARCH_TEXT, String.valueOf(searchView.getQuery()));
        } else {
            Timber.e("Unexpected null search view (issue #1412)");
        }

        if (bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        selectedInstances = (LinkedHashSet<Long>) state.getSerializable(SELECTED_INSTANCES);
        isSearchBoxShown = state.getBoolean(IS_SEARCH_BOX_SHOWN);
        isBottomDialogShown = state.getBoolean(IS_BOTTOM_DIALOG_SHOWN);
        savedFilterText = state.getString(SEARCH_TEXT);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);

        int gray_color = Color.parseColor("#5a5a5a");
        String username = (String) GeneralSharedPreferences.getInstance().get(KEY_USERNAME);
        MenuItem usernameItem = menu.findItem(R.id.menu_username);
        SpannableString userSpan = new SpannableString(getString(R.string.welcome_username, username));
        userSpan.setSpan(new ForegroundColorSpan(gray_color), 0, userSpan.length(), 0);
        usernameItem.setTitle(userSpan);
        MenuItem versionItem = menu.findItem(R.id.menu_version);
        SpannableString versionSpan = new SpannableString("v" + BuildConfig.VERSION_NAME);
        versionSpan.setSpan(new ForegroundColorSpan(gray_color), 0, versionSpan.length(), 0);
        versionItem.setTitle(versionSpan);

        final MenuItem sortItem = menu.findItem(R.id.menu_sort);
        final MenuItem searchItem = menu.findItem(R.id.menu_filter);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(themeUtils.getColorOnPrimary());
        searchView.setQueryHint(getResources().getString(R.string.search));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterText = query;
                updateAdapter();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterText = newText;
                updateAdapter();
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                sortItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                sortItem.setVisible(true);
                return true;
            }
        });

        if (isSearchBoxShown) {
            searchItem.expandActionView();
            searchView.setQuery(savedFilterText, false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!MultiClickGuard.allowClick(getClass().getName())) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_sort:
                bottomSheetDialog.show();
                isBottomDialogShown = true;
                return true;
            case R.id.menu_configure_qr_code:
                startActivity(new Intent(this, QRCodeTabsActivity.class));
                return true;
            case R.id.menu_delete_files:
                startActivity(new Intent(this, DeleteSavedFormActivity.class));
                return true;
            case R.id.menu_data_management:
                startActivityForResult(new Intent(this, DataManagementActivity.class), ApplicationConstants.RequestCodes.DATA_REQUEST);
                return true;
            case R.id.menu_general_preferences:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            case R.id.menu_logout:
                AppDependencyComponent component = Collect.getInstance().getComponent();
                component.generalSharedPreferences().save(GeneralKeys.KEY_USERNAME, null);
                component.generalSharedPreferences().save(GeneralKeys.KEY_PASSWORD, null);
                component.generalSharedPreferences().save(GeneralKeys.KEY_AUTH_TOKEN, null);
//                component.propertyManager().reload();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void performSelectedSearch(int position) {
        saveSelectedSortingOrder(position);
        updateAdapter();
    }

    protected void checkPreviouslyCheckedItems() {
        listView.clearChoices();
        List<Integer> selectedPositions = new ArrayList<>();
        int listViewPosition = 0;
        Cursor cursor = listAdapter.getCursor();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long instanceId = cursor.getLong(cursor.getColumnIndex(InstanceColumns._ID));
                if (selectedInstances.contains(instanceId)) {
                    selectedPositions.add(listViewPosition);
                }
                listViewPosition++;
            } while (cursor.moveToNext());
        }

        for (int position : selectedPositions) {
            listView.setItemChecked(position, true);
        }
    }

    protected abstract void updateAdapter();

    protected abstract String getSortingOrderKey();

    protected boolean areCheckedItems() {
        return getCheckedCount() > 0;
    }

    protected int getCheckedCount() {
        return listView.getCheckedItemCount();
    }

    private void saveSelectedSortingOrder(int selectedStringOrder) {
        selectedSortingOrder = selectedStringOrder;
        PreferenceManager.getDefaultSharedPreferences(Collect.getInstance())
                .edit()
                .putInt(getSortingOrderKey(), selectedStringOrder)
                .apply();
    }

    protected void restoreSelectedSortingOrder() {
        selectedSortingOrder = PreferenceManager
                .getDefaultSharedPreferences(Collect.getInstance())
                .getInt(getSortingOrderKey(), BY_NAME_ASC);
    }

    protected int getSelectedSortingOrder() {
        if (selectedSortingOrder == null) {
            restoreSelectedSortingOrder();
        }
        return selectedSortingOrder;
    }

    protected CharSequence getFilterText() {
        return filterText != null ? filterText : "";
    }

    protected void clearSearchView() {
        searchView.setQuery("", false);
    }

    private void setupBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this, themeUtils.getBottomDialogTheme());
        final View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        final RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerView);

        final SortDialogAdapter adapter = new SortDialogAdapter(this, recyclerView, sortingOptions, getSelectedSortingOrder(), new RecyclerViewClickListener() {
            @Override
            public void onItemClicked(SortDialogAdapter.ViewHolder holder, int position) {
                holder.updateItemColor(selectedSortingOrder);
                performSelectedSearch(position);
                bottomSheetDialog.dismiss();
                isBottomDialogShown = false;
            }
        });
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        bottomSheetDialog.setContentView(sheetView);

        if (isBottomDialogShown) {
            bottomSheetDialog.show();
        }
    }

    protected void showSnackbar(@NonNull String result) {
        SnackbarUtils.showShortSnackbar(llParent, result);
    }

    protected void hideProgressBarIfAllowed() {
        if (canHideProgressBar && progressBarVisible) {
            hideProgressBar();
        }
    }

    protected void hideProgressBarAndAllow() {
        this.canHideProgressBar = true;
        hideProgressBar();
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        progressBarVisible = false;
    }

    protected void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        progressBarVisible = true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, FillBlankFormActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == ApplicationConstants.RequestCodes.DATA_REQUEST && resultCode == RESULT_OK) {
            initBottomNav();
        }
    }
}
