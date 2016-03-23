package ua.kiev.bpo.expensesmanager;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import ua.kiev.bpo.expensesmanager.ExpensesManagerDatabaseHelper.DayRecordCursor;

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
        DayRecord dayRecord = null;
        if (id == -1){
            Calendar calendar = new GregorianCalendar();
            Date trialTime = new Date();
            calendar.setTime(trialTime);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            DayRecordCursor cursor = mHelper.queryDayRecordByDay(calendar.getTimeInMillis());
            cursor.moveToFirst();
            if (cursor.isAfterLast()){
                dayRecord = new DayRecord();
                dayRecord.setDateOfRecord(new Date(calendar.getTimeInMillis()));
                dayRecord.setId(mHelper.insertDayRecord(dayRecord));
                dayRecord.setStartDate((int) calendar.getTimeInMillis());
            } else {
                dayRecord = cursor.getDayRecord();
            }
        } else {
            DayRecordCursor cursor = mHelper.queryDayRecord(id);
            cursor.moveToFirst();
            if (!cursor.isAfterLast()){
                dayRecord = cursor.getDayRecord();
            }
        }

        return dayRecord;
    }

    public void updateDayRecordDatabase(DayRecord dayRecord){
        mHelper.updateDayRecord(dayRecord);
    }

    public Loader<Cursor> queryDayRecors(Uri uri){
        return new CursorLoader(mAppContext,uri, DayRecord
                .DAYRECORD_PROJECTION, null,
                null, null);
    }

}
