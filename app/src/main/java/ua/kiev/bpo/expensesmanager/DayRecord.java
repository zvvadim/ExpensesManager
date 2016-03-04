package ua.kiev.bpo.expensesmanager;

import java.util.Date;

public class DayRecord {
    private long mId;
    private Date mDateOfRecord;
    private int mAmount;

    public DayRecord() {
        mId = -1;
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
}
