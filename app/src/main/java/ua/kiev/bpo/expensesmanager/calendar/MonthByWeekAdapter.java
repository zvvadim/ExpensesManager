package ua.kiev.bpo.expensesmanager.calendar;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;

import java.util.ArrayList;
import java.util.HashMap;

import ua.kiev.bpo.expensesmanager.DayRecord;

public class MonthByWeekAdapter extends SimpleWeeksAdapter {

    private static final String TAG = "MonthByWeekAdapter";

    public static final String WEEK_PARAMS_IS_MINI = "mini_month";
    protected static int DEFAULT_QUERY_DAYS = 7 * 8; // 8 weeks
    private static final long ANIMATE_TODAY_TIMEOUT = 1000;

    private boolean mAnimateToday = false;
    private long mAnimateTime = 0;


    protected String mHomeTimeZone;
    protected int mOrientation = Configuration.ORIENTATION_PORTRAIT;

    protected int mFirstJulianDay;
    protected int mQueryDays;
    protected boolean mIsMiniMonth = true;

    protected ArrayList<ArrayList<DayRecord>> mDayRecordList = new ArrayList<ArrayList<DayRecord>>();
    protected ArrayList<DayRecord> mDayRecords = null;

    public MonthByWeekAdapter(Context context,
                              HashMap<String, Integer> params) {
        super(context, params);
    }

    public void setDayRecords(int firstJulianDay, int numDays, ArrayList<DayRecord> dayRecords){

        mDayRecords = dayRecords;
        mFirstJulianDay = firstJulianDay;
        mQueryDays = numDays;

        ArrayList<ArrayList<DayRecord>> dayRecordDayList = new ArrayList<ArrayList<DayRecord>>();
        for (int i = 0; i < numDays; i++) {
            dayRecordDayList.add(new ArrayList<DayRecord>());
        }

        if (dayRecords == null || dayRecords.size() == 0) {
            if(Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No records. Returning early--go schedule something fun.");
            }
            mDayRecordList = dayRecordDayList;
            refresh();
            return;
        }

        for (DayRecord dayRecord : dayRecords){
            int startDay = dayRecord.mStartDate - mFirstJulianDay;
            if (startDay <= numDays){
                 if (startDay < 0) {
                     startDay = 0;
                 }
                 if (startDay > numDays) {
                     startDay = numDays;
                 }
                 for (int j = startDay; j <= startDay; j++) {
                     dayRecordDayList.get(j).add(dayRecord);
                 }
            }
        }

        mDayRecordList = dayRecordDayList;

        if(Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Processed " + dayRecords.size() + " events.");
        }
        mDayRecordList = dayRecordDayList;
        refresh();

    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //if (mIsMiniMonth) {
        //    return super.getView(position, convertView, parent);
        //}
        MonthWeekDayRecordsView v;
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        HashMap<String, Integer> drawingParams = null;
        boolean isAnimatingToday = false;
        if (convertView != null) {
            v = (MonthWeekDayRecordsView) convertView;
            // Checking updateToday uses the current params instead of the new
            // params, so this is assuming the view is relatively stable
            if (mAnimateToday && v.updateToday(mSelectedDay.timezone)) {
                long currentTime = System.currentTimeMillis();
                // If it's been too long since we tried to start the animation
                // don't show it. This can happen if the user stops a scroll
                // before reaching today.
                if (currentTime - mAnimateTime > ANIMATE_TODAY_TIMEOUT) {
                    mAnimateToday = false;
                    mAnimateTime = 0;
                } else {
                    isAnimatingToday = true;
                    // There is a bug that causes invalidates to not work some
                    // of the time unless we recreate the view.
                    v = new MonthWeekDayRecordsView(mContext);
                }
            } else {
                drawingParams = (HashMap<String, Integer>) v.getTag();
            }
        } else {
            v = new MonthWeekDayRecordsView(mContext);
        }
        if (drawingParams == null) {
            drawingParams = new HashMap<String, Integer>();
        }
        drawingParams.clear();

        v.setLayoutParams(params);
        v.setClickable(true);
        v.setOnTouchListener(this);

        int selectedDay = -1;
        if (mSelectedWeek == position) {
            selectedDay = mSelectedDay.weekDay;
        }

        drawingParams.put(SimpleWeekView.VIEW_PARAMS_HEIGHT,
                (parent.getHeight() + parent.getTop()) / mNumWeeks);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_SHOW_WK_NUM, mShowWeekNumber ? 1 : 0);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK_START, mFirstDayOfWeek);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_NUM_DAYS, mDaysPerWeek);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK, position);
        drawingParams.put(SimpleWeekView.VIEW_PARAMS_FOCUS_MONTH, mFocusMonth);
        drawingParams.put(MonthWeekDayRecordsView.VIEW_PARAMS_ORIENTATION, mOrientation);

        if (isAnimatingToday) {
            mAnimateToday = false;
        }

        v.setWeekParams(drawingParams, mSelectedDay.timezone);
        sendDayRecordsToView(v);
        return v;
    }

    private void sendDayRecordsToView(MonthWeekDayRecordsView v) {
        if (mDayRecordList.size() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No events loaded, did not pass any events to view.");
            }
            v.setDayRecords(null, null);
            return;
        }
        int viewJulianDay = v.getFirstJulianDay();
        int start = viewJulianDay - mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mDayRecordList.size()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Week is outside range of loaded events. viewStart: " + viewJulianDay
                        + " eventsStart: " + mFirstJulianDay);
            }
            v.setDayRecords(null, null);
            return;
        }
        v.setDayRecords(mDayRecordList.subList(start, end), mDayRecords);
    }
    @Override
    protected void refresh() {
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mShowWeekNumber = Utils.getShowWeekNumber(mContext);
        mHomeTimeZone = Utils.getTimeZone(mContext, null);
        mOrientation = mContext.getResources().getConfiguration().orientation;
        notifyDataSetChanged();
    }

}
