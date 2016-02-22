package ua.kiev.bpo.expensesmanager;

import android.content.Context;

public class DayRecordLoader  extends DataLoader<DayRecord>{
    private long mDayRecordId;

    public DayRecordLoader(Context context, long dayRecordId) {
        super(context);
        mDayRecordId = dayRecordId;
    }

    @Override
    public DayRecord loadInBackground() {
        return DayRecordManager.get(getContext()).getDayRecord(mDayRecordId);
    }
}
