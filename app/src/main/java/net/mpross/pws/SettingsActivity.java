package net.mpross.pws;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView errText =(TextView) findViewById(R.id.errorText);

        if(getIntent().getExtras().getBoolean("error")==true){
            errText.setText("Weather Station Error");
        }
        else{
            errText.setText("");
        }

        final EditText editText = (EditText) findViewById(R.id.station);
        byte[] by=new byte[10];
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
                        //fos.write("KWABAINB47".getBytes());
                        fos.close();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        setResult(1);
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
}
