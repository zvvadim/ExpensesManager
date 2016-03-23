package ua.kiev.bpo.expensesmanager;

import android.content.Context;
import android.database.Cursor;
import android.text.format.Time;
import android.util.Log;

import java.util.Date;
import java.util.List;

public class DayRecord {
    private static final String TAG = "DayRecord";

    protected int mNumWeeks = 6;
    protected boolean mShowWeekNumber = false;
    protected int mDaysPerWeek = 7;

    private long mId;
    private Date mDateOfRecord;
    public int mStartDate; // start Julian day
    private int mAmount;
    private Time mTempTime;

    public static final String[] DAYRECORD_PROJECTION = {
            ExpensesManagerDatabaseHelper.COLUMN_EXPENSES_ID,
            ExpensesManagerDatabaseHelper.COLUMN_EXPENSES_AMOUNT,
            ExpensesManagerDatabaseHelper.COLUMN_EXPENSES_DATE
    };

    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_AMOUNT_INDEX = 1;
    private static final int PROJECTION_DATE_INDEX = 2;

    public DayRecord() {
        mId = -1;
        mStartDate = -1;
        mDateOfRecord = new Date();
        mAmount = 0;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public Date getDateOfRecord() {
        return mDateOfRecord;
    }

    public void setDateOfRecord(Date dateOfRecord) {
        mDateOfRecord = dateOfRecord;
    }

    public int getAmount() {
        return mAmount;
    }

    public void setAmount(int amount) {
        mAmount = amount;
    }

    public long getStartDate() {
        return mStartDate;
    }

    public void setStartDate(int startDate) {
        mStartDate = startDate;
    }

    public void appendResult(String result){
        mAmount = mAmount + Integer.parseInt(result);
    }

    public static void buildDayRecordsFromCursor(List<DayRecord> dayRecords, Cursor cDayReconds,
                                                 Context context, int startDay, int endDay){

        if (cDayReconds == null || dayRecords == null) {
            Log.e(TAG, "buildEventsFromCursor: null cursor or null events list!");
            return;
        }

        int count = cDayReconds.getCount();

        if (count == 0) {
            return;
        }

        // Sort events in two passes so we ensure the allday and standard events
        // get sorted in the correct order
        cDayReconds.moveToPosition(-1);
        while (cDayReconds.moveToNext()) {
            DayRecord d = generateDayRecordFromCursor(cDayReconds);
            dayRecords.add(d);
        }
    }

    private static DayRecord generateDayRecordFromCursor(Cursor cDayrecord){
        DayRecord d = new DayRecord();
        d.mId = cDayrecord.getLong(PROJECTION_ID_INDEX);
        d.mAmount = cDayrecord.getInt(PROJECTION_AMOUNT_INDEX);
        d.mDateOfRecord = new Date(cDayrecord.getLong(PROJECTION_DATE_INDEX));

        Time tempTime = new Time();
        tempTime.set(cDayrecord.getLong(PROJECTION_DATE_INDEX));
        d.mStartDate = Time.getJulianDay(tempTime.toMillis(true), tempTime.gmtoff);

        return d;
    }
}
