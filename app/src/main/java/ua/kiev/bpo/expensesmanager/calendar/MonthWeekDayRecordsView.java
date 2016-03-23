package ua.kiev.bpo.expensesmanager.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ua.kiev.bpo.expensesmanager.DayRecord;

public class MonthWeekDayRecordsView  extends SimpleWeekView {
    private static final String TAG = "MonthView";

    public static final String VIEW_PARAMS_ORIENTATION = "orientation";

    protected Time mToday = new Time();
    protected boolean mHasToday = false;
    protected int mTodayIndex = -1;

    protected List<ArrayList<DayRecord>> mDayRecords = null;
    protected ArrayList<DayRecord> mUnsortedDayRecords = null;


    public MonthWeekDayRecordsView(Context context) {
        super(context);
    }

    public void setDayRecords(List<ArrayList<DayRecord>>  sortedDayRecords, ArrayList<DayRecord> unsortedEvents) {

        mDayRecords = sortedDayRecords;
        if (sortedDayRecords == null) {
            return;
        }
        if (sortedDayRecords.size() != mNumDays) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.wtf(TAG, "Events size must be same as days displayed: size="
                        + sortedDayRecords.size() + " days=" + mNumDays);
            }
            mDayRecords = null;
            return;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawWeekNums(canvas);
        drawDaySeparators(canvas);
        drawDayRecords(canvas);

    }

    protected void drawDayRecords(Canvas canvas) {
        if (mDayRecords == null){
            return;
        }

        int day = -1;
        for (ArrayList<DayRecord> dayRecordDay : mDayRecords) {
            day++;

            if (dayRecordDay == null) {
                continue;
            }

            Iterator<DayRecord> iter = dayRecordDay.iterator();
            while (iter.hasNext()) {
                DayRecord dayRecord = iter.next();
                drawEvent(canvas, dayRecord);
            }
        }

    }

    protected void drawEvent(Canvas canvas, DayRecord dayRecord){
        String amount  = Integer.toString(dayRecord.getAmount());
        float width = p.measureText(amount.substring(1));
        CharSequence text = TextUtils.ellipsize(
                amount , null, width, TextUtils.TruncateAt.END);


    }

    public boolean updateToday(String tz) {
        mToday.timezone = tz;
        mToday.setToNow();
        mToday.normalize(true);
        int julianToday = Time.getJulianDay(mToday.toMillis(false), mToday.gmtoff);
        if (julianToday >= mFirstJulianDay && julianToday < mFirstJulianDay + mNumDays) {
            mHasToday = true;
            mTodayIndex = julianToday - mFirstJulianDay;
        } else {
            mHasToday = false;
            mTodayIndex = -1;
        }
        return mHasToday;
    }

}
