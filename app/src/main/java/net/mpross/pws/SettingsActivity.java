package net.mpross.pws;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import java.io.*;
import android.content.*;
import android.view.inputmethod.InputMethodManager;
import android.app.Activity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final EditText editText = (EditText) findViewById(R.id.station);
        byte[] by=new byte[10];
        try {
            FileInputStream fis = openFileInput("station_file");
            int n= fis.read(by);
            String str = new String(by, "UTF-8");
            editText.setText(str);
        }
        catch (IOException e) {
            System.out.println(e);
        }
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String string = editText.getText().toString();

                    try {
                        FileOutputStream fos = openFileOutput("station_file", Context.MODE_PRIVATE);
                        fos.write(string.getBytes());
                        fos.close();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        setResult(1);
                        finish();
                    }
                    catch (IOException e){

                    }

                    handled = true;
                }
                return handled;
            }
        });
    }
}
