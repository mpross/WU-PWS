/*
Personal Weather Station Data Viewer by M.P.Ross
Copyright (C) 2017  M.P.Ross

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.mpross.pws;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Bundle b=new Bundle();
    Intent i=new Intent();
    String calDate="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date);

        TextView errText =(TextView) findViewById(R.id.errDText);

        b=getIntent().getExtras();

        calDate=b.getString("calDate");
        //Tells user if they selected wrong date
        if(getIntent().getExtras().getBoolean("error")==true){
            errText.setText("No data available for that date.");
        }
        else{
            errText.setText("");
        }
        //Calendar
        CalendarView calendar = (CalendarView) findViewById(R.id.calendarView);
        try{
            calendar.setDate(new SimpleDateFormat("dd,MM,yyyy").parse(calDate).getTime(), true, true);
        }
        catch (ParseException p){}
        catch (RuntimeException r){}
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {

            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month,
                                            int dayOfMonth) {
                calDate =String.valueOf(dayOfMonth)+","+String.valueOf(month+1)+","+String.valueOf(year);
            }
        });

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    i.putExtra("calDate",calDate);
                    setResult(0,i);
                    finish();
            }
        });


    }
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }
}
