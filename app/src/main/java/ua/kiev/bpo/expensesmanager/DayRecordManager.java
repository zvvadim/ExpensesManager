package ua.kiev.bpo.expensesmanager;

import android.content.Context;
import android.content.SharedPreferences;

public class DayRecordManager {

    private static final String PREFS_FILE = "expenses";
    private static final String PREF_CURRENT_DAY_ID = "DayRecordManager.currentDayRecordID";
    private static DayRecordManager sDayRecordManager;
    private long mCurrentDayRecordId;
    private Context mAppContext;
    private ExpensesManagerDatabaseHelper mHelper;
    private SharedPreferences mPreferences;

    private DayRecordManager(Context appContext) {
        mAppContext = appContext;
        mPreferences = mAppContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        mHelper = new ExpensesManagerDatabaseHelper(appContext);
        mCurrentDayRecordId = mPreferences.getLong(PREF_CURRENT_DAY_ID, -1);
    }

    public static DayRecordManager get(Context c) {
        if (sDayRecordManager == null) {
            sDayRecordManager = new DayRecordManager(c);
        }
        return sDayRecordManager;
    }

    public DayRecord getDayRecord(Long id){
        return null;
    }
}
