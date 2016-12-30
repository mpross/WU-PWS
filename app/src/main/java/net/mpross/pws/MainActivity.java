package net.mpross.pws;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.os.*;
import android.net.*;
import java.net.*;
import java.io.*;
import android.widget.*;
import java.util.*;
import java.text.*;
import android.content.Intent;
import java.io.*;
import java.lang.Byte;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    String currentString=new String();
    String dailyString=new String();
    String station="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new datagrab().execute("");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView text =(TextView) findViewById(R.id.text1);
                text.setText("Loading...");
                new datagrab().execute("");
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }
    public class datagrab extends AsyncTask<String, Void,String>
    {

        @Override
        protected String doInBackground(String[] p1)
        {
            //String station="KWABAINB47";
            byte[] by=new byte[10];
            try {
                FileInputStream fis = openFileInput("station_file");
                int n= fis.read(by);
                fis.close();
                station = new String(by, "UTF-8");
                System.out.println(by);
                System.out.println(station);

            }
            catch (IOException e){
                System.out.println(e);
            }

            String day = new SimpleDateFormat("dd").format(Calendar.getInstance().getTime());
            String month = new SimpleDateFormat("MM").format(Calendar.getInstance().getTime());
            String year = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());
            StringBuilder url =new StringBuilder();
            url.append("https://www.wunderground.com/weatherstation/WXDailyHistory.asp?");
            url.append("ID="+station);
            url.append("&day="+day);
            url.append("&month="+month);
            url.append("&year="+year);
            url.append("&graphspan=day&format=1");

            try {
                URL site = new URL(url.toString());

                BufferedReader data = new BufferedReader(
                        new InputStreamReader(site.openStream()));

                String in;
                StringBuilder build = new StringBuilder();
                StringBuilder outBuild = new StringBuilder();
                while ((in = data.readLine()) != null)
                    build.append(in + "\r");

                String[] lines = build.toString().split("\r");

                float[] temp = new float[lines.length];
                float[] dew = new float[lines.length];
                float[] press = new float[lines.length];
                float[] windDeg = new float[lines.length];
                float[] windSpeed = new float[lines.length];
                float[] windGust = new float[lines.length];
                float[] hum = new float[lines.length];
                float[] precip = new float[lines.length];

                float tempAvg = 0;
                float dewAvg = 0;
                float pressAvg = 0;
                float windDAvg = 0;
                float windSAvg = 0;
                float windG = 0;
                float humAvg = 0;
                float precipMax = 0;
                int j = 0;
                for (String line : lines) {
                    String[] col = line.split(",");
                    if (col.length > 1 && j > 1) {
                        if (Float.parseFloat(col[1]) > 0) {
                            temp[j] = Float.parseFloat(col[1]);
                        }
                        if (Float.parseFloat(col[2]) > 0) {
                            dew[j] = Float.parseFloat(col[2]);
                        }
                        if (Float.parseFloat(col[3]) > 0) {
                            press[j] = Float.parseFloat(col[3]);
                        }
                        if (Float.parseFloat(col[5]) > 0) {
                            windDeg[j] = Float.parseFloat(col[5]);
                        }
                        if (Float.parseFloat(col[6]) > 0) {
                            windSpeed[j] = Float.parseFloat(col[6]);
                        }
                        if (Float.parseFloat(col[7]) > 0) {
                            windGust[j] = Float.parseFloat(col[7]);
                        }
                        if (Float.parseFloat(col[8]) > 0) {
                            hum[j] = Float.parseFloat(col[8]);
                        }
                        if (Float.parseFloat(col[9]) > 0) {
                            precip[j] = Float.parseFloat(col[9]);
                        }

                        tempAvg += temp[j];
                        dewAvg += dew[j];
                        pressAvg += press[j];
                        windDAvg += windDeg[j];
                        windSAvg += windSpeed[j];
                        humAvg += hum[j];

                        if (windGust[j] > windG) {
                            windG = windGust[j];
                        }

                        if (Float.parseFloat(col[12]) > precipMax) {
                            precipMax = Float.parseFloat(col[12]);
                        }


                    }
                    j++;
                }
                tempAvg /= j / 2;
                dewAvg /= j / 2;
                pressAvg /= j / 2;
                windDAvg /= j / 2;
                windSAvg /= j / 2;
                humAvg /= j / 2;

                outBuild.append(lines[lines.length - 2]);
                outBuild.append(";");
                //outBuild.append(String.valueOf(tempAvg));
                outBuild.append(String.valueOf(Math.round(tempAvg * 100.0) / 100.0));
                outBuild.append(",");
                outBuild.append(String.valueOf(Math.round(dewAvg * 100.0) / 100.0));
                outBuild.append(",");
                outBuild.append(String.valueOf(Math.round(pressAvg * 100.0) / 100.0));
                outBuild.append(",");
                outBuild.append(String.valueOf(Math.round(windDAvg * 100.0) / 100.0));
                outBuild.append(",");
                outBuild.append(String.valueOf(Math.round(windSAvg * 100.0) / 100.0));
                outBuild.append(",");
                outBuild.append(String.valueOf(windG));
                outBuild.append(",");
                outBuild.append(String.valueOf(Math.round(humAvg * 100.0) / 100.0));
                outBuild.append(",");
                outBuild.append(String.valueOf(precipMax));

                return outBuild.toString();


            }

            catch(IOException e){
                return e.toString();
            }
            catch(NetworkOnMainThreadException b){
                return b.toString();
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            String fieldString="Date & Time,Temperature,Dewpoint,Pressure,Wind Direction,Wind Direction,Wind Speed," +
                    "Wind Gust,Humidity,Hourly Precip,Conditions,Clouds,Daily Rain,SoftwareType,DateUTC";
            String fieldStringD="Average Temperature,Average Dewpoint,Average Pressure,Average Wind Direction," +
                    "Average Wind Speed,Maximum Wind Gust,Average Humidity,Daily Rain";
            String endString=", °F, °F, inHg,, °, mph, mph, %, in,,, in,,";
            String endStringD=" °F, °F, inHg, °, mph, mph, %, in";
            String exString="Conditions,Clouds,SoftwareType,DateUTC,Daily Rain";
            String[] split=result.split(";");
            String[] dataCur=split[0].split(",");
            String[] dataDay=split[1].split(",");
            StringBuilder outCur=new StringBuilder();
            StringBuilder outDay=new StringBuilder();
            TextView text =(TextView) findViewById(R.id.text1);
            String station="";
            byte[] by=new byte[10];
            try {
                FileInputStream fis = openFileInput("station_file");
                int n= fis.read(by);
                fis.close();
                station = new String(by, "UTF-8");
            }
            catch (IOException e){
                System.out.println(e);
            }
            TextView text2 =(TextView) findViewById(R.id.textView2);
            text2.setText(station);


            String[] fields= fieldString.split(",");
            String[] fieldsD= fieldStringD.split(",");
            String[] ends= endString.split(",");
            String[] endsD= endStringD.split(",");
            List exclude= Arrays.asList(exString.split(","));
            try {
                int i = 0;
                for (String field : fields) {
                    if (!exclude.contains(field)) {
                        outCur.append(field);
                        outCur.append(": ");
                        outCur.append(dataCur[i]);
                        outCur.append(ends[i]);
                        outCur.append("\n");
                        i++;
                    }
                }
                int j = 0;
                for (String field : fieldsD) {
                    outDay.append(field);
                    outDay.append(": ");
                    outDay.append(dataDay[j]);
                    outDay.append(endsD[j]);
                    outDay.append("\n");
                    j++;
                }
                currentString = outCur.toString();
                dailyString = outDay.toString();
                text.setText(currentString);
            }
            catch(ArrayIndexOutOfBoundsException a) {
                stationError();
            }
        }


    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            new datagrab().execute("");
        } else {
            super.onBackPressed();
        }
    }
    public void stationError() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("error",true);
        startActivityForResult(intent,2);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = new Intent(this, SettingsActivity.class);
        int result=0;

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            intent.putExtra("error",false);
            startActivityForResult(intent,result);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == 1) {
                new datagrab().execute("");
            }
        }
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        TextView text =(TextView) findViewById(R.id.text1);
        if (id == R.id.nav_current) {
            text.setText(currentString.toString());
        } else if (id == R.id.nav_daily) {
            text.setText(dailyString.toString());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
