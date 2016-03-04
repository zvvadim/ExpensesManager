package ua.kiev.bpo.expensesmanager;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CalendarView;
import android.widget.Toast;

public class ExpensesManagerCalendarActivity extends AppCompatActivity {
    private ExpensesManagerCalendarView mCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_manager_calendar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


    }

    public void initializeCalendar(){
        mCalendar = (ExpensesManagerCalendarView) findViewById(R.id.calendar);
        mCalendar.setShowWeekNumber(false);
        mCalendar.setFirstDayOfWeek(2);
        mCalendar.setSelectedWeekBackgroundColor(getResources().getColor(R.color.green));
        mCalendar.setUnfocusedMonthDateColor(getResources().getColor(R.color.transparent));
        mCalendar.setWeekSeparatorLineColor(getResources().getColor(R.color.transparent));
        mCalendar.setSelectedDateVerticalBar(R.color.darkgreen);
        mCalendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                Toast.makeText(getApplicationContext(), day + "/" + month + "/" + year,
                        Toast.LENGTH_LONG).show();
            }
        });

    }
}
