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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    int units=0;
    Bundle b=new Bundle();
    Intent i=new Intent();
    String calDate="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        b=getIntent().getExtras();

        calDate=b.getString("calDate");
        System.out.println(calDate);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(getIntent().getExtras().getInt("unit"));

        TextView errText =(TextView) findViewById(R.id.errText);

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



        if(getIntent().getExtras().getBoolean("error")==true){
            errText.setText("Input a valid weather station ID");
        }
        else{
            errText.setText("");
        }

        final EditText editText = (EditText) findViewById(R.id.station);
        byte[] by=new byte[11];
        try {
            FileInputStream fis = openFileInput("station_file");
            int n= fis.read(by);
            fis.close();
            String str = new String(by, "UTF-8");
            editText.setText(str);
        }
        catch (IOException e) {
            System.out.println(e);
        }
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String string = editText.getText().toString();
                StringBuilder build =new StringBuilder();

                try {
                    FileOutputStream fos = openFileOutput("station_file", Context.MODE_PRIVATE);
                    if(string.length()<10){
                        build.append(string);
                        for(int i=0; i<(10-string.length());i++) {
                            build.append("0");
                        }
                        string=build.toString();
                    }
                    fos.write(string.toUpperCase().getBytes());
                    fos.close();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                    i.putExtra("calDate",calDate);
                    setResult(units,i);

                    finish();
                }
                catch (IOException e){
                    System.out.println(e);
                }
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String string = editText.getText().toString();
                    StringBuilder build =new StringBuilder();

                    try {
                        FileOutputStream fos = openFileOutput("station_file", Context.MODE_PRIVATE);
                        if(string.length()<10){
                            build.append(string);
                            for(int i=0; i<(10-string.length());i++) {
                                build.append("0");
                            }
                            string=build.toString();
                        }
                        fos.write(string.toUpperCase().getBytes());
                        fos.close();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                        i.putExtra("calDate",calDate);
                        setResult(units,i);

                        finish();
                    }
                    catch (IOException e){
                        System.out.println(e);
                    }

                    handled = true;
                }
                return handled;
            }
        });
    }
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        units=pos;
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }
}
