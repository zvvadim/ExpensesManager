package ua.kiev.bpo.expensesmanager;

import android.support.v4.app.Fragment;

/**
 * Created by VI on 21.10.2015.
 */
public class ExpensesManagerActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new ExpensesManagerFragment();
    }
}
