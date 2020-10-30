package org.odk.collect.android.activities;

import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.odk.collect.android.R;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.utilities.ApplicationConstants;

import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_DESC;

public abstract class FormListActivity extends AppListActivity {

    protected static final String SORT_BY_NAME_ASC
            = FormsColumns.DISPLAY_NAME + " COLLATE NOCASE ASC";
    protected static final String SORT_BY_NAME_DESC
            = FormsColumns.DISPLAY_NAME + " COLLATE NOCASE DESC";
    protected static final String SORT_BY_DATE_ASC = FormsColumns.DATE + " ASC";
    protected static final String SORT_BY_DATE_DESC = FormsColumns.DATE + " DESC";

    protected String getSortingOrder() {
        String sortingOrder = SORT_BY_NAME_ASC;
        switch (getSelectedSortingOrder()) {
            case BY_NAME_ASC:
                sortingOrder = SORT_BY_NAME_ASC;
                break;
            case BY_NAME_DESC:
                sortingOrder = SORT_BY_NAME_DESC;
                break;
            case BY_DATE_ASC:
                sortingOrder = SORT_BY_DATE_ASC;
                break;
            case BY_DATE_DESC:
                sortingOrder = SORT_BY_DATE_DESC;
                break;
        }
        return sortingOrder;
    }
}
