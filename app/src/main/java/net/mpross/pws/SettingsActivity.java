/*
Personal Weather Station Data Viewer by M.P.Ross
Copyright (C) 2018  M.P.Ross

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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    int units=0;
    int nordic=0;
    boolean nothingSelected=false;
    boolean nothingSelectedNordic=false;
    String listIn=",";
    Bundle b=new Bundle();
    Intent i=new Intent();
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        b=getIntent().getExtras();
        //Units selection
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(getIntent().getExtras().getInt("unit"));

        Spinner nordicSpinner = (Spinner) findViewById(R.id.nordicSpinner);
        nordicSpinner.setOnItemSelectedListener(this);

        final EditText editText = (EditText) findViewById(R.id.station);
        byte[] by=new byte[50];
        byte[] byU=new byte[1];
        try {
            FileInputStream fis = openFileInput("station_file");
            int n= fis.read(by);
            fis.close();
            String str = new String(by, "UTF-8");
            editText.setText(str.trim());
        }
        catch (IOException e) {
            System.out.println(e);
        }
        try{
            FileInputStream fis = openFileInput("unit_file");
            fis.read(byU);
            fis.close();
            units=(int)byU[0];
        }
        catch (IOException e) {
            System.out.println(e);
        }
        try{
            FileInputStream fis = openFileInput("nordic_file");
            fis.read(byU);
            fis.close();
            nordic=(int)byU[0];
        }
        catch (IOException e) {
            System.out.println(e);
        }
        try {
            FileInputStream fis = openFileInput("stationList_file");
            int n= fis.read(by);
            fis.close();
            listIn = new String(by, "UTF-8");

        }
        catch (IOException e) {
            System.out.println(e);
        }
        //"Apply" button action
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
                            build.append("");
                        }
                        string=build.toString();
                    }
                    fos.write(string.toUpperCase().getBytes());
                    fos.close();
                    if(!string.trim().equals("")) {
                        fos = openFileOutput("stationList_file", Context.MODE_APPEND);
                        StringBuilder listBuild = new StringBuilder();
                        listBuild.append(string.trim().toUpperCase());
                        listBuild.append(',');
                        fos.write(listBuild.toString().getBytes());
                        fos.close();
                    }

                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    i.putExtra("nordic",nordic);
                    setResult(units,i);

                    finish();
                }
                catch (IOException e){
                }
            }
        });
        // Recent station list
        final ListView listview = (ListView) findViewById(R.id.stationList);
        final String[] values = listIn.replaceAll("\\P{Print}", "").split(",");
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
        list.removeAll(Arrays.asList("", null));
        Set<String> hs = new HashSet<>();
        hs.addAll(list);
        list.clear();
        list.addAll(hs);
        Collections.reverse(list);
        final ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);

        ListView recentStations= (ListView) findViewById(R.id.stationList);
        recentStations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                editText.setText(list.get(position).toString());
            }
        });
        //"Clear" button action
        final Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    FileOutputStream fos = openFileOutput("stationList_file", Context.MODE_PRIVATE);
                    fos.write("".getBytes());
                    fos.close();
                }
                catch (IOException e){

                }
                adapter.clear();
                adapter.notifyDataSetChanged();
            }
        });
        //Return key action
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    handled = true;
                }
                return handled;
            }
        });
    }
    public void onItemSelected(AdapterView<?> parent, View view,int pos, long id) {
        Spinner spin= (Spinner) parent;
        if(spin.getId()==R.id.spinner) {
            Spinner nordicSpinner = (Spinner) findViewById(R.id.nordicSpinner);
            nordicSpinner.setVisibility(View.VISIBLE);

            Resources res = getResources();
            String[] istring = res.getStringArray(R.array.ispeeds);
            String[] mstring = res.getStringArray(R.array.mspeeds);

            ArrayAdapter<String> iadapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, istring);
            iadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ArrayAdapter<String> madapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, mstring);
            madapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            if(nothingSelected==false){
                if (units == 1) {
                    nordicSpinner.setAdapter(madapter);
                    iadapter.notifyDataSetChanged();

                } else {
                    nordicSpinner.setAdapter(iadapter);
                    madapter.notifyDataSetChanged();
                }
                spin.setSelection(units);
                nothingSelected=true;
            }

            units = pos;
            if (pos == 1) {
                nordicSpinner.setAdapter(madapter);
                iadapter.notifyDataSetChanged();

            } else {
                nordicSpinner.setAdapter(iadapter);
                madapter.notifyDataSetChanged();
            }
        }
        if(spin.getId()==R.id.nordicSpinner) {
            Spinner nordicSpinner = (Spinner) findViewById(R.id.nordicSpinner);
            if(nothingSelectedNordic==false){
                nordicSpinner.setSelection(nordic);
                nothingSelectedNordic=true;
            }
            nordic=pos;
        }

    }

    public void onNothingSelected(AdapterView<?> parent) {
    }
}
