package ua.kiev.bpo.expensesmanager.calendar;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;

import java.util.List;

import ua.kiev.bpo.expensesmanager.R;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;


public class AllInOneActivity extends AbstractCalendarActivity {
    private static final String TAG = "AllInOneActivity";
    private static final boolean DEBUG = false;

    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";

    private CalendarController mController;

    private View mMiniMonth;
    private View mCalendarsList;
    private View mMiniMonthContainer;
    private String mTimeZone;

    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;

    private boolean mOnSaveInstanceStateCalled = false;

    private final Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            AllInOneActivity.this.invalidateOptionsMenu();
        }
    };

    // runs every midnight/time changes and refreshes the today icon
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
        }
    };


    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (DEBUG)
            Log.d(TAG, "New intent received " + intent.toString());
        // Don't change the date if we're just returning to the app's home
        if (Intent.ACTION_VIEW.equals(action)
                && !intent.getBooleanExtra(Utils.INTENT_KEY_HOME, false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                millis = Utils.timeFromIntentInMillis(intent);
            }
            if (millis != -1 &&  mController != null) {
                Time time = new Time(mTimeZone);
                time.set(millis);
                time.normalize(true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mController = CalendarController.getInstance(this);

        mCalendarsList = findViewById(R.id.calendar_list);
        mMiniMonthContainer = findViewById(R.id.mini_month_container);

        long timeMillis = -1;
        final Intent intent = getIntent();
        if (savedInstanceState != null) {
            timeMillis = savedInstanceState.getLong(BUNDLE_KEY_RESTORE_TIME);
        } else {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                // Open EventInfo later
                timeMillis = parseViewAction(intent);
            }
            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }

        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        Time t = new Time(mTimeZone);
        t.set(timeMillis);

        setContentView(R.layout.all_in_one);

        initFragments(timeMillis, savedInstanceState);
    }

    private void initFragments(Long timeMillis, Bundle savedInstanceState){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment miniMonthFrag = new MonthByWeekFragment();
        ft.replace(R.id.mini_month, miniMonthFrag);

        setMainPane(ft, R.id.main_pane, timeMillis, true);
        ft.commit(); // this needs to be after setMainPane()
    }

    private void setMainPane(
            FragmentTransaction ft, int viewId, long timeMillis, boolean force) {
        if (mOnSaveInstanceStateCalled) {
            return;
        }
        FragmentManager fragmentManager = getFragmentManager();
        Fragment frag = new MonthByWeekFragment();

        boolean doCommit = false;
        if (ft == null) {
            doCommit = true;
            ft = fragmentManager.beginTransaction();
        }
        ft.replace(viewId, frag);
        if (doCommit) {
            if (DEBUG) {
                Log.d(TAG, "setMainPane AllInOne=" + this + " finishing:" + this.isFinishing());
            }
            ft.commit();
        }

    }

    private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.valueOf(data.getLastPathSegment());
                    if (mViewEventId != -1) {
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        timeMillis = mIntentEventStartMillis;
                    }
                } catch (NumberFormatException e) {
                    // Ignore if mViewEventId can't be parsed
                }
            }
        }
        return timeMillis;
    }



    @Override
    protected void onResume() {
        super.onResume();

        mOnSaveInstanceStateCalled = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);
    }
}
