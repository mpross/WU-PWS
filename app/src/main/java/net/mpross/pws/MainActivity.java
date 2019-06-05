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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.*;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    String currentString=new String(); //String for current stats
    String dailyString=new String(); //String for daily stats
    String rawData = new String();
    String station=""; //Weather station name
    static String viewSel="current";
    int units=0; // User unit choice
    int nordic=0; // Wind speed unit choice
    int nativeUnits=0; // Units the data is in
    int errSrcId=0; //0=originated from settings, 1=originated from date
    boolean errBool=false; //Error status

    //LineGraph time series
    LineGraphSeries<DataPoint> seriesT = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesD = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesP = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesWS = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesWG = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesWD = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesH = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesR = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesRD = new LineGraphSeries<>();

    static String day = new SimpleDateFormat("dd").format(Calendar.getInstance().getTime());
    static String month = new SimpleDateFormat("MM").format(Calendar.getInstance().getTime());
    static String year = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());
    static String calDate=day+","+month+","+year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView text =(TextView) findViewById(R.id.text1);
        GraphView graph = (GraphView) findViewById(R.id.graph);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true);
        
        graph.setVisibility(View.GONE);
        byte[] byU=new byte[1];
         //Unit selection 0=imperial, 1=metric

        // Reads unit selection to file
        try {
            FileInputStream fis = openFileInput("unit_file");
            fis.read(byU);
            fis.close();
            units = (int) byU[0];
        } catch (IOException e) {
            System.out.println(e);
        }
        // Reads wind speed unit selection to file
        try {
            FileInputStream fis = openFileInput("nordic_file");
            fis.read(byU);
            fis.close();
            nordic = (int) byU[0];
        } catch (IOException e) {
            System.out.println(e);
        }

        //Initiated data grab from WeatherUnderground

        new datagrab().execute("");

        //Settings menu
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    //Data grab from WeatherUnderground
    public class datagrab extends AsyncTask<String, Void,String>
    {

        @Override
        protected String doInBackground(String[] p1)
        {
            byte[] by=new byte[13];
            //Reads station id from station file
            try {
                FileInputStream fis = openFileInput("station_file");
                int n= fis.read(by);
                fis.close();
                station = new String(by, "UTF-8");
            }
            catch (IOException e){
                System.out.println(e);
            }


            GraphView graph = (GraphView) findViewById(R.id.graph);

            //Builds URL for data file
            StringBuilder url =new StringBuilder();
            url.append("https://www.wunderground.com/weatherstation/WXDailyHistory.asp?");
            url.append("ID="+station);
            url.append("&day="+day);
            url.append("&month="+month);
            url.append("&year="+year);
            url.append("&graphspan=day&format=1");
            String in;
            StringBuilder build = new StringBuilder();
            StringBuilder outBuild = new StringBuilder();
            try {

                //Removes non ASCII character from URL
                URL site = new URL(url.toString().replaceAll("\\P{Print}", ""));
                //Reads file
                BufferedReader data = new BufferedReader(
                        new InputStreamReader(site.openStream()));

                //Puts return character at end of every line

                while ((in = data.readLine()) != null)
                    build.append(in + "\r");
            }
            catch(Exception e){
                Context context = getApplicationContext();
                CharSequence toastText = "Connection Error";
                int duration = Toast.LENGTH_LONG;
                try {
                    Toast toast = Toast.makeText(context, toastText, duration);
                    toast.show();
                }
                catch (RuntimeException r){}
                System.out.println(e);
            }
            //Splits lines
            String[] lines = build.toString().split("\r");
            //Data vector initialization

            float[] temp = new float[1];
            float[] dew = new float[1];
            float[] press = new float[1];
            float[] windDeg = new float[1];
            float[] windSpeed = new float[1];
            float[] windGust = new float[1];
            float[] hum = new float[1];
            float[] precip = new float[1];
            float[] precipDay = new float[1];
            float[] tim = new float[1];

            if(lines.length / 2 - 1>1) {
                temp = new float[lines.length / 2 - 1];
                dew = new float[lines.length / 2 - 1];
                press = new float[lines.length / 2 - 1];
                windDeg = new float[lines.length / 2 - 1];
                windSpeed = new float[lines.length / 2 - 1];
                windGust = new float[lines.length / 2 - 1];
                hum = new float[lines.length / 2 - 1];
                precip = new float[lines.length / 2 - 1];
                precipDay = new float[lines.length / 2 - 1];
                tim = new float[lines.length / 2 - 1];
            }

            int m = 0;

            float tempAvg = 0;
            float tempHigh = -1000;
            float tempLow = 1000;
            float dewHigh = -1000;
            float dewLow = 1000;
            float dewAvg = 0;
            float pressAvg = 0;
            String windDir = "";
            float windDAvg = 0;
            float windSAvg = 0;
            float windG = 0;
            float humAvg = 0;
            float precipMax = 0;

            float lastHum = 0;
            float lastTemp = 0;
            float lastDew = 0;
            float lastPress = 0;

            String timStamp = "0";
            int j = 0;
            for (String line : lines) {
                //Splits lines into columns
                String[] col = line.split(",");
                //Reads data units from first line
                if (j == 1) {
                    if (col[1].equals("TemperatureF")) {
                        nativeUnits = 0;
                    } else {
                        nativeUnits = 1;
                    }
                }
                if (col.length > 1 && j > 1) {
                    timStamp = col[0];
                    //Time stamp to hours conversion
                    tim[j/2 - 1] = Float.parseFloat(col[0].split(" ")[1].split(":")[0]) + Float.parseFloat(col[0].split(" ")[1].split(":")[1]) / 60
                            + Float.parseFloat(col[0].split(" ")[1].split(":")[2]) / 3600;
                    //Drop out handling
                    if (Float.parseFloat(col[1]) > -50) {
                        //If data is in imperial
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                temp[j/2 - 1] = Float.parseFloat(col[1]);
                            } else {
                                temp[j/2 - 1] = (Float.parseFloat(col[1]) - 32.0f) * 5.0f / 9.0f;
                            }
                        }
                        //If data is in metric
                        else {
                            if (units == 0) {
                                temp[j/2 - 1] = (Float.parseFloat(col[1]) * 9.0f / 5.0f + 32.0f);
                            } else {
                                temp[j/2 - 1] = Float.parseFloat(col[1]);
                            }
                        }
                        if (temp[j/2 - 1] < tempLow) {
                            tempLow = temp[j/2 - 1];
                        }
                        if (temp[j/2 - 1] > tempHigh) {
                            tempHigh = temp[j/2 - 1];
                        }
                        lastTemp = temp[j/2 - 1];
                    }
                    //Drop outs just retain last value
                    else {
                        temp[j/2 - 1] = lastTemp;
                    }
                    if (Float.parseFloat(col[2]) > -50) {
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                dew[j/2 - 1] = Float.parseFloat(col[2]);
                            } else {
                                dew[j/2 - 1] = (Float.parseFloat(col[2]) - 32.0f) * 5.0f / 9.0f;
                            }
                        } else {
                            if (units == 0) {
                                dew[j/2 - 1] = (Float.parseFloat(col[2]) * 9.0f / 5.0f + 32.0f);
                            } else {
                                dew[j/2 - 1] = Float.parseFloat(col[2]);
                            }
                        }
                        if (dew[j/2 - 1] < dewLow) {
                            dewLow = dew[j/2 - 1];
                        }
                        if (dew[j/2 - 1] > dewHigh) {
                            dewHigh = dew[j/2 - 1];
                        }
                        lastDew = dew[j/2 - 1];
                    } else {
                        dew[j/2 - 1] = lastDew;
                    }
                    if (Float.parseFloat(col[3]) > 0) {
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                press[j/2 - 1] = Float.parseFloat(col[3]);
                            } else {
                                press[j/2 - 1] = Float.parseFloat(col[3]) * 33.8639f;
                            }
                        } else {
                            if (units == 0) {
                                press[j/2 - 1] = Float.parseFloat(col[3]) / 33.8639f;
                            } else {
                                press[j/2 - 1] = Float.parseFloat(col[3]);
                            }
                        }
                        lastPress = press[j/2 - 1];
                    } else {
                        press[j/2 - 1] = lastPress;
                    }
                    windDir = col[4];
                    try {
                        if (Float.parseFloat(col[5]) > 0) {
                            windDeg[j/2 - 1] = Float.parseFloat(col[5]);
                        }
                    }
                    catch (NumberFormatException n){
                        windDeg[j/2 - 1] = 0;
                    }
                    if (Float.parseFloat(col[6]) > 0) {
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                if (nordic == 0) {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]);
                                } else {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]) / 1.150779f;
                                }
                            } else {
                                if (nordic == 0) {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]) * 1.60934f;
                                } else {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]) * 0.44704f;
                                }
                            }
                        } else {
                            if (units == 0) {
                                if (nordic == 0) {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]) / 1.60934f;
                                } else {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]) / 1.852f;
                                }
                            } else {
                                if (nordic == 0) {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]);
                                } else {
                                    windSpeed[j/2 - 1] = Float.parseFloat(col[6]) * 0.277778f;
                                }
                            }
                        }
                    }
                    if (Float.parseFloat(col[7]) > 0) {
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                if (nordic == 0) {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]);
                                } else {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]) / 1.150779f;
                                }
                            } else {
                                if (nordic == 0) {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]) * 1.60934f;
                                } else {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]) * 0.44704f;
                                }
                            }
                        } else {
                            if (units == 0) {
                                if (nordic == 0) {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]) / 1.60934f;
                                } else {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]) / 1.852f;
                                }
                            } else {
                                if (nordic == 0) {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]);
                                } else {
                                    windGust[j/2 - 1] = Float.parseFloat(col[7]) * 0.277778f;
                                }
                            }
                        }
                    }
                    try{
                        hum[j/2 - 1] = Float.parseFloat(col[8]);
                        lastHum = hum[j/2 - 1];
                    }
                    catch (NumberFormatException n) {
                        hum[j/2 - 1] = lastHum;
                    }
                    if (Float.parseFloat(col[9]) > 0) {
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                precip[j/2 - 1] = Float.parseFloat(col[9]);
                            } else {
                                precip[j/2 - 1] = Float.parseFloat(col[9]) * 25.4f;
                            }
                        } else {
                            if (units == 0) {
                                precip[j/2 - 1] = Float.parseFloat(col[9]) / 25.4f;
                            } else {
                                precip[j/2 - 1] = Float.parseFloat(col[9]);
                            }
                        }
                    }
                    if (Float.parseFloat(col[12]) > 0) {
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                precipDay[j/2 - 1] = Float.parseFloat(col[12]);
                            } else {
                                precipDay[j/2 - 1] = Float.parseFloat(col[12]) * 25.4f;
                            }
                        } else {
                            if (units == 0) {
                                precipDay[j/2 - 1] = Float.parseFloat(col[12]) / 25.4f;
                            } else {
                                precipDay[j/2 - 1] = Float.parseFloat(col[12]);
                            }
                        }
                    }

                    tempAvg += temp[j/2 - 1];
                    dewAvg += dew[j/2 - 1];
                    pressAvg += press[j/2 - 1];
                    windDAvg += windDeg[j/2 - 1];
                    windSAvg += windSpeed[j/2 - 1];
                    humAvg += hum[j/2 - 1];

                    if (windGust[j/2 - 1] > windG) {
                        windG = windGust[j/2 - 1];
                    }

                    if (precipDay[j/2 - 1] > precipMax) {
                        precipMax = precipDay[j/2 - 1];
                    }
                }
                j++;
            }
            //Plot data vector creation
            DataPoint[] tempData = new DataPoint[temp.length];
            DataPoint[] dewData = new DataPoint[dew.length];
            DataPoint[] pressData = new DataPoint[press.length];
            DataPoint[] windDData = new DataPoint[windDeg.length];
            DataPoint[] windSData = new DataPoint[windSpeed.length];
            DataPoint[] windGData = new DataPoint[windGust.length];
            DataPoint[] humData = new DataPoint[hum.length];
            DataPoint[] rainData = new DataPoint[precip.length];
            DataPoint[] rainDayData = new DataPoint[precipDay.length];

            m = 0;
            for (float t : temp) {
                tempData[m] = new DataPoint(tim[m], t);
                dewData[m] = new DataPoint(tim[m], dew[m]);
                pressData[m] = new DataPoint(tim[m], press[m]);
                windSData[m] = new DataPoint(tim[m], windSpeed[m]);
                windGData[m] = new DataPoint(tim[m], windGust[m]);
                windDData[m] = new DataPoint(tim[m], windDeg[m]);
                humData[m] = new DataPoint(tim[m], hum[m]);
                rainData[m] = new DataPoint(tim[m], precip[m]);
                rainDayData[m] = new DataPoint(tim[m], precipDay[m]);
                m++;
            }
            seriesT = new LineGraphSeries<>(tempData);
            seriesD = new LineGraphSeries<>(dewData);
            seriesP = new LineGraphSeries<>(pressData);
            seriesWS = new LineGraphSeries<>(windSData);
            seriesWG = new LineGraphSeries<>(windGData);
            seriesWD = new LineGraphSeries<>(windDData);
            seriesH = new LineGraphSeries<>(humData);
            seriesR = new LineGraphSeries<>(rainData);
            seriesRD = new LineGraphSeries<>(rainDayData);

            seriesD.setTitle("Dew Point");
            seriesT.setTitle("Temperature");
            seriesWS.setTitle("Wind Speed");
            seriesWG.setTitle("Wind Gust");
            seriesWD.setTitle("Wind Direction");
            seriesH.setTitle("Humidity");
            seriesP.setTitle("Pressure");
            seriesR.setTitle("Hourly Precipitation");
            seriesRD.setTitle("Daily Precipitation");

            tempAvg /= (j/2) - 1;
            dewAvg /= (j/2) - 1;
            pressAvg /= (j/2) - 1;
            windDAvg /= (j/2) - 1;
            windSAvg /= (j/2) - 1;
            humAvg /= (j/2) - 1;

            try {
                //If native units are imperial
                if (nativeUnits == 0) {
                    //Last line of data file is already the correct format for displaying
                    if (nordic == 0) {
                        if (units == 0) {
                            if((lines.length - 2)>0) {
                                outBuild.append(lines[lines.length - 2]);
                            }
                            else{
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",");
                                outBuild.append(0);
                                outBuild.append(",,,,,,");
                            }
                        }
                        //If units are different from file then create correctly formatted string
                        else {
                            outBuild.append(timStamp);
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(temp[temp.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(dew[dew.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(press[press.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(windDir));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(windDeg[windDeg.length - 2]));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(hum[hum.length - 2]));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(precip[precip.length - 2] * 100.0) / 100.0));
                            outBuild.append(",,,,,,");
                        }
                    }
                    //If units are different from file then create correctly formatted string
                    else {
                        outBuild.append(timStamp);
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(temp[temp.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(dew[dew.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(press[press.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDir));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDeg[windDeg.length - 2]));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(hum[hum.length - 2]));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(precip[precip.length - 2] * 100.0) / 100.0));
                        outBuild.append(",,,,,,");
                    }
                }
                //If native units are metric
                else {
                    if (nordic == 0) {
                        if (units == 0) {
                            outBuild.append(timStamp);
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(temp[temp.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(dew[dew.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(press[press.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(windDir));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(windDeg[windDeg.length - 2]));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 2] * 100.0) / 100.0));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(hum[hum.length - 2]));
                            outBuild.append(",");
                            outBuild.append(String.valueOf(Math.round(precip[precip.length - 2] * 100.0) / 100.0));
                            outBuild.append(",,,,,,");
                        } else {
                            outBuild.append(lines[lines.length - 2]);
                        }
                    } else {
                        outBuild.append(timStamp);
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(temp[temp.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(dew[dew.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(press[press.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDir));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDeg[windDeg.length - 2]));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 2] * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(hum[hum.length - 2]));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(precip[precip.length - 2] * 100.0) / 100.0));
                        outBuild.append(",,,,,,");
                    }
                }
            }
            catch (ArrayIndexOutOfBoundsException a){
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",");
                outBuild.append(0);
                outBuild.append(",,,,,,");
            }
            //Build average string
            outBuild.append(";");
            outBuild.append(String.valueOf(Math.round(tempAvg * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(tempHigh * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(tempLow * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(dewAvg * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(dewHigh * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(dewLow * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(pressAvg * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(windDAvg * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(windSAvg * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(windG * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(humAvg * 100.0) / 100.0));
            outBuild.append(",");
            outBuild.append(String.valueOf(Math.round(precipMax * 100.0) / 100.0));
            //Return current status; average values
            outBuild.append("!");
            outBuild.append(build.toString());
            return outBuild.toString();
        }

        @Override
        protected void onPostExecute(String result) {
        //Label creation
        String fieldString = "Date & Time,Temperature,Dew,Pressure,Wind: \n" +
                "     Direction,     Direction,     Speed," +
                "     Gust,Humidity,Hourly Precip,Conditions,Clouds,Daily Rain,SoftwareType,DateUTC";
        String fieldStringD = "Temperature: \n     Average,     High,     Low,Dew: \n" +
                "     Average,     High,     Low,Average Pressure,Average Wind Direction," +
                "Average Wind Speed,Maximum Wind Gust,Average Humidity,Daily Rain";
        String endString = "";
        String endStringD = "";
        if (units == 0) {
            if(nordic==0) {
                endString = ", °F, °F, inHg,, °, mph, mph, %, in,,, in,,";
                endStringD = " °F, °F, °F, °F, °F, °F, inHg, °, mph, mph, %, in";
            }
            else{
                endString = ", °F, °F, inHg,, °, knots, knots, %, in,,, in,,";
                endStringD = " °F, °F, °F, °F, °F, °F, inHg, °, knots, knots, %, in";
            }
        } else {
            if(nordic==0) {
                endString = ", °C, °C, hPa,, °, km/h, km/h, %, mm,,, mm,,";
                endStringD = " °C, °C, °C, °C, °C, °C, hPa, °, km/h, km/h, %, mm";
            }
            else{
                endString = ", °C, °C, hPa,, °, m/s, m/s, %, mm,,, mm,,";
                endStringD = " °C, °C, °C, °C, °C, °C, hPa, °, m/s, m/s, %, mm";
            }
        }
        //Excluded labels that are included in data file
        String exString = "Conditions,Clouds,SoftwareType,DateUTC,Daily Rain";

        StringBuilder outCur = new StringBuilder();
        StringBuilder outDay = new StringBuilder();
        TextView text = (TextView) findViewById(R.id.text1);

        TextView temp = (TextView) findViewById(R.id.temp);
        TextView tempHigh = (TextView) findViewById(R.id.highTemp);
        TextView tempLow = (TextView) findViewById(R.id.lowTemp);
        TextView dew = (TextView) findViewById(R.id.dew);
        TextView dewHigh = (TextView) findViewById(R.id.highDew);
        TextView dewLow = (TextView) findViewById(R.id.lowDew);
        TextView press = (TextView) findViewById(R.id.press);
        TextView windLabel = (TextView) findViewById(R.id.windLabel);
        TextView windSpeed = (TextView) findViewById(R.id.windSpeed);
        TextView windGust = (TextView) findViewById(R.id.windGust);
        TextView windDir = (TextView) findViewById(R.id.windDir);
        TextView windDeg = (TextView) findViewById(R.id.windDeg);
        TextView rain = (TextView) findViewById(R.id.rain);
        TextView hum = (TextView) findViewById(R.id.hum);
        TextView time = (TextView) findViewById(R.id.date);
        ProgressBar humBar = (ProgressBar) findViewById(R.id.humBar);
        ImageView statusIcon=(ImageView) findViewById(R.id.statusIcon);
        ImageView windIcon=(ImageView) findViewById(R.id.windIcon);
        ImageView windDirIcon=(ImageView) findViewById(R.id.windDirIcon);
        ImageView snowIcon=(ImageView) findViewById(R.id.snowIcon);
        ImageView pressChange=(ImageView) findViewById(R.id.pressChange);

        String[] fields = fieldString.split(",");
        String[] fieldsD = fieldStringD.split(",");
        String[] ends = endString.split(",");
        String[] endsD = endStringD.split(",");
        List exclude = Arrays.asList(exString.split(","));
        try {
            rawData = result.split("!")[1];
        }
        catch (ArrayIndexOutOfBoundsException a){
            rawData="";
        }
        String[] split = result.split("!")[0].split(";");
        String[] dataCur = new String[fields.length];
        String[] dataDay = new String[fields.length];
        try {
            dataCur = split[0].split(",");
            dataDay = split[1].split(",");
        } catch (ArrayIndexOutOfBoundsException a) {
            throw a;
        }
        try {
            int i = 0;
            for (String field : fields) {
                if (!exclude.contains(field)) {
                    outCur.append(field);
                    outCur.append(": ");
                    if( i<= dataCur.length) {
                        outCur.append(dataCur[i]);
                    }
                    else{
                        outCur.append(" ");
                    }
                    outCur.append(ends[i]);
                    outCur.append("\n");

                    if(i==0 && dataCur[i].equals("0")) {
                        Context context = getApplicationContext();
                        CharSequence toastText = "Connection Error";
                        int duration = Toast.LENGTH_LONG;

                        Toast toast = Toast.makeText(context, toastText, duration);
                        toast.show();
                    }
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
            if (viewSel == "current") {
                text.setText(currentString);
            }
            if (viewSel == "daily") {
                text.setText(dailyString);
            }
        } catch (ArrayIndexOutOfBoundsException a) {
        }

        String station = "0";
        //Reads station from file
        byte[] by = new byte[11];
        try {
            FileInputStream fis = openFileInput("station_file");
            int n = fis.read(by);
            fis.close();
            station = new String(by, "UTF-8");
        } catch (IOException e) {
        }
        TextView text2 = (TextView) findViewById(R.id.textView2);
        try {
            text2.setText(station);
        }
        catch(NullPointerException n){
        }
        try {
            GraphView graph = (GraphView) findViewById(R.id.graph);
            GridLabelRenderer glr = graph.getGridLabelRenderer();
            glr.setPadding(32);
            //Veiw changing. Hides unused elements and makes visible selected elements
            if (viewSel == "current") {

                text.setVisibility(View.INVISIBLE);

                temp.setVisibility(View.VISIBLE);
                tempHigh.setVisibility(View.INVISIBLE);
                tempLow.setVisibility(View.INVISIBLE);
                dew.setVisibility(View.VISIBLE);
                dewHigh.setVisibility(View.INVISIBLE);
                dewLow.setVisibility(View.INVISIBLE);
                press.setVisibility(View.VISIBLE);
                windSpeed.setVisibility(View.VISIBLE);
                windGust.setVisibility(View.VISIBLE);
                windDir.setVisibility(View.VISIBLE);
                windDeg.setVisibility(View.VISIBLE);
                rain.setVisibility(View.VISIBLE);
                hum.setVisibility(View.VISIBLE);
                time.setVisibility(View.VISIBLE);
                humBar.setVisibility(View.VISIBLE);
                windLabel.setVisibility(View.VISIBLE);
                graph.setVisibility(View.GONE);
                windIcon.setVisibility(View.VISIBLE);
                statusIcon.setVisibility(View.VISIBLE);
                windDirIcon.setVisibility(View.VISIBLE);
                pressChange.setVisibility(View.VISIBLE);

                time.setText(currentString.split("\n")[0].split(": ")[1]);
                temp.setText(currentString.split("\n")[1].split(": ")[1]);
                dew.setText("Dew: "+currentString.split("\n")[2].split(": ")[1]);
                press.setText("Press: "+currentString.split("\n")[3].split(": ")[1]);
                windDir.setText(currentString.split("\n")[5].split(": ")[1]);
                windDeg.setText(currentString.split("\n")[6].split(": ")[1]);
                windSpeed.setText(currentString.split("\n")[7].split(": ")[1]);
                windGust.setText("Gust: " + currentString.split("\n")[8].split(": ")[1]);
                hum.setText("Humidity: " + currentString.split("\n")[9].split(": ")[1]);
                humBar.setProgress((int) Float.parseFloat(currentString.split("\n")[9].split(": ")[1].split(" %")[0]));
                rain.setText("Hourly Rain: " + currentString.split("\n")[10].split(": ")[1]);

                //Pressure derivative icon. If press-mean>10% mean then positive, < then negative, else null
                if(Float.parseFloat(currentString.split("\n")[3].split(": ")[1].split(" ")[0])-Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0])>0.01*Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0])){
                    pressChange.setImageResource(R.mipmap.press_change);
                }
                else if(Float.parseFloat(currentString.split("\n")[3].split(": ")[1].split(" ")[0])-Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0])<-0.01*Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0])){
                    pressChange.setImageResource(R.mipmap.press_change_n);
                }
                else{
                    pressChange.setImageResource(R.mipmap.press_null);
                }

                if((Float.parseFloat(currentString.split("\n")[10].split(": ")[1].split(" ")[0]))>0){
                    statusIcon.setImageResource(R.mipmap.rain_icon);
                }
                else if((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0]))>70 && units==0){
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                }
                else if((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0]))>20 && units==1){
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                }
                else{
                    statusIcon.setImageResource(R.mipmap.cloud_icon);
                }

                if((Float.parseFloat(currentString.split("\n")[8].split(": ")[1].split(" ")[0]))>4){
                    windIcon.setVisibility(View.VISIBLE);
                }
                else{
                    windIcon.setVisibility(View.INVISIBLE);
                }

                if((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0]))<32 && units==0){
                    snowIcon.setVisibility(View.VISIBLE);
                }
                else if((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0]))<0 && units==1){
                    snowIcon.setVisibility(View.VISIBLE);
                }
                else{
                    snowIcon.setVisibility(View.INVISIBLE);
                }
                try {
                    windDirIcon.setRotation((Float.parseFloat(currentString.split("\n")[6].split(": ")[1].split(" ")[0])));
                }
                catch (NumberFormatException n){
                    windDirIcon.setRotation(0);
                }
            } else if (viewSel == "daily") {

                text.setVisibility(View.INVISIBLE);
                temp.setVisibility(View.VISIBLE);
                tempHigh.setVisibility(View.VISIBLE);
                tempLow.setVisibility(View.VISIBLE);
                dew.setVisibility(View.VISIBLE);
                dewHigh.setVisibility(View.VISIBLE);
                dewLow.setVisibility(View.VISIBLE);
                press.setVisibility(View.VISIBLE);
                windSpeed.setVisibility(View.VISIBLE);
                windGust.setVisibility(View.VISIBLE);
                windDir.setVisibility(View.VISIBLE);
                windDeg.setVisibility(View.INVISIBLE);
                rain.setVisibility(View.VISIBLE);
                hum.setVisibility(View.VISIBLE);
                time.setVisibility(View.INVISIBLE);
                humBar.setVisibility(View.VISIBLE);
                windLabel.setVisibility(View.VISIBLE);
                graph.setVisibility(View.GONE);
                windIcon.setVisibility(View.VISIBLE);
                statusIcon.setVisibility(View.VISIBLE);
                windDirIcon.setVisibility(View.VISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                temp.setText(dailyString.split("\n")[1].split(": ")[1]);
                tempHigh.setText(dailyString.split("\n")[2].split(": ")[1]);
                tempLow.setText(dailyString.split("\n")[3].split(": ")[1]);
                dew.setText("Dew: "+dailyString.split("\n")[5].split(": ")[1]);
                dewHigh.setText(dailyString.split("\n")[6].split(": ")[1]);
                dewLow.setText(dailyString.split("\n")[7].split(": ")[1]);
                press.setText("Press: "+dailyString.split("\n")[8].split(": ")[1]);
                windDir.setText(dailyString.split("\n")[9].split(": ")[1]);
                windSpeed.setText(dailyString.split("\n")[10].split(": ")[1]);
                windGust.setText("Gust: " + dailyString.split("\n")[11].split(": ")[1]);
                hum.setText("Humidity: " + dailyString.split("\n")[12].split(": ")[1]);
                humBar.setProgress((int) Float.parseFloat(dailyString.split("\n")[12].split(": ")[1].split(" %")[0]));
                rain.setText("Daily Rain: " + dailyString.split("\n")[13].split(": ")[1]);
                if((Float.parseFloat(dailyString.split("\n")[13].split(": ")[1].split(" ")[0]))>0){
                    statusIcon.setImageResource(R.mipmap.rain_icon);
                }
                else if((Float.parseFloat(dailyString.split("\n")[2].split(": ")[1].split(" ")[0]))>70 && units==0){
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                }
                else if((Float.parseFloat(dailyString.split("\n")[2].split(": ")[1].split(" ")[0]))>20 && units==1){
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                }
                else{
                    statusIcon.setImageResource(R.mipmap.cloud_icon);
                }
                if((Float.parseFloat(dailyString.split("\n")[11].split(": ")[1].split(" ")[0]))>4){
                    windIcon.setVisibility(View.VISIBLE);
                }
                else{
                    windIcon.setVisibility(View.INVISIBLE);
                }
                if((Float.parseFloat(dailyString.split("\n")[1].split(": ")[1].split(" ")[0]))<32 && units==0){
                    snowIcon.setVisibility(View.VISIBLE);
                }
                else if((Float.parseFloat(dailyString.split("\n")[1].split(": ")[1].split(" ")[0]))<0 && units==1){
                    snowIcon.setVisibility(View.VISIBLE);
                }
                else{
                    snowIcon.setVisibility(View.INVISIBLE);
                }
                windDirIcon.setRotation((Float.parseFloat(dailyString.split("\n")[9].split(": ")[1].split(" ")[0])));
            } else if (viewSel == "tempPlot") {

                text.setVisibility(View.GONE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);
                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesT.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                //Plot range settings
                if (seriesT.getLowestValueY() < seriesD.getLowestValueY()) {
                    if (seriesT.getLowestValueY() > 0) {
                        graph.getViewport().setMinY(seriesT.getLowestValueY() * .9);
                    } else {
                        graph.getViewport().setMinY(seriesT.getLowestValueY() * 1.1);
                    }
                } else {
                    if (seriesD.getLowestValueY() > 0) {
                        graph.getViewport().setMinY(seriesD.getLowestValueY() * .9);
                    } else {
                        graph.getViewport().setMinY(seriesD.getLowestValueY() * 1.1);
                    }
                }
                if (seriesT.getHighestValueY() > seriesD.getHighestValueY()) {
                    if (seriesT.getHighestValueY() > 0) {
                        graph.getViewport().setMaxY(seriesT.getHighestValueY() * 1.1);
                    } else {
                        graph.getViewport().setMaxY(seriesT.getHighestValueY() * 0.9);
                    }
                } else {
                    if (seriesD.getHighestValueY() > 0) {
                        graph.getViewport().setMaxY(seriesD.getHighestValueY() * 1.1);
                    } else {
                        graph.getViewport().setMaxY(seriesD.getHighestValueY() * 0.9);
                    }
                }
                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesT);
                graph.addSeries(seriesD);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX) + " h";
                        } else {
                            if (units == 0) {
                                return super.formatLabel(value, isValueX) + " °F";
                            } else {
                                return super.formatLabel(value, isValueX) + " °C";
                            }
                        }
                    }
                });
                seriesD.setColor(Color.GRAY);
            } else if (viewSel == "pressPlot") {

                text.setVisibility(View.GONE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesP.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                graph.getViewport().setMinY(seriesP.getLowestValueY()*0.99);
                graph.getViewport().setMaxY(seriesP.getHighestValueY()*1.01);
                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesP);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX) + " h";
                        } else {
                            if (units == 0) {
                                return super.formatLabel(value, isValueX) + " inHg";
                            } else {
                                return super.formatLabel(value, isValueX) + " hPa";
                            }
                        }
                    }
                });

            } else if (viewSel == "windPlot") {
                text.setVisibility(View.GONE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesWS.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if (seriesWS.getLowestValueY() < seriesWG.getLowestValueY()) {
                    if (seriesWS.getLowestValueY() > 0) {
                        graph.getViewport().setMinY(seriesWS.getLowestValueY() * .9);
                    } else {
                        graph.getViewport().setMinY(seriesWS.getLowestValueY() * 1.1);
                    }
                } else {
                    if (seriesWG.getLowestValueY() > 0) {
                        graph.getViewport().setMinY(seriesWG.getLowestValueY() * .9);
                    } else {
                        graph.getViewport().setMinY(seriesWG.getLowestValueY() * 1.1);
                    }
                }
                if (seriesWS.getHighestValueY() > seriesWG.getHighestValueY()) {
                    if (seriesWS.getHighestValueY() > 0) {
                        graph.getViewport().setMaxY(seriesWS.getHighestValueY() * 1.1);
                    } else {
                        graph.getViewport().setMaxY(seriesWS.getHighestValueY() * 0.9);
                    }
                } else {
                    if (seriesWG.getHighestValueY() > 0) {
                        graph.getViewport().setMaxY(seriesWG.getHighestValueY() * 1.1);
                    } else {
                        graph.getViewport().setMaxY(seriesWG.getHighestValueY() * 0.9);
                    }
                }
                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesWS);
                graph.addSeries(seriesWG);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX) + " h";
                        } else {
                            if (units == 0) {
                                return super.formatLabel(value, isValueX) + " mph";
                            } else{
                                if(nordic==0){
                                    return super.formatLabel(value, isValueX) + " km/h";
                                }
                                else {
                                    return super.formatLabel(value, isValueX) + " m/s";
                                }
                            }
                        }
                    }
                });

                seriesWG.setColor(Color.GRAY);
            } else if (viewSel == "humPlot") {

                text.setVisibility(View.GONE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesH.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if (seriesH.getLowestValueY() > 50) {
                    graph.getViewport().setMinY(50);
                } else {
                    graph.getViewport().setMinY(0);
                }
                if (seriesH.getHighestValueY() < 50) {
                    graph.getViewport().setMaxY(50);
                } else {
                    graph.getViewport().setMaxY(105);
                }
                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesH);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX) + " h";
                        } else {
                            return super.formatLabel(value, isValueX) + " %";
                        }
                    }
                });
            } else if (viewSel == "windDPlot") {

                text.setVisibility(View.GONE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesWD.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);

                graph.getViewport().setMinY(0);
                graph.getViewport().setMaxY(360);

                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesWD);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX) + " h";
                        } else {
                            return super.formatLabel(value, isValueX) + " °";
                        }
                    }
                });
            } else if (viewSel == "rainPlot") {

                text.setVisibility(View.GONE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesR.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if (seriesR.getLowestValueY() < seriesRD.getLowestValueY()) {
                    if (seriesR.getLowestValueY() > 0) {
                        graph.getViewport().setMinY(seriesR.getLowestValueY() * .9);
                    } else {
                        graph.getViewport().setMinY(seriesR.getLowestValueY() * 1.1);
                    }
                } else {
                    if (seriesRD.getLowestValueY() > 0) {
                        graph.getViewport().setMinY(seriesRD.getLowestValueY() * .9);
                    } else {
                        graph.getViewport().setMinY(seriesRD.getLowestValueY() * 1.1);
                    }
                }
                if (seriesR.getHighestValueY() > seriesRD.getHighestValueY()) {
                    if (seriesR.getHighestValueY() > 0) {
                        graph.getViewport().setMaxY(seriesR.getHighestValueY() * 1.1);
                    } else {
                        graph.getViewport().setMaxY(seriesR.getHighestValueY() * 0.9);
                    }
                } else {
                    if (seriesD.getHighestValueY() > 0) {
                        graph.getViewport().setMaxY(seriesRD.getHighestValueY() * 1.1);
                    } else {
                        graph.getViewport().setMaxY(seriesRD.getHighestValueY() * 0.9);
                    }
                }
                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesR);
                graph.addSeries(seriesRD);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX) + " h";
                        } else {
                            if (units == 0) {
                                return super.formatLabel(value, isValueX) + " in";
                            } else {
                                return super.formatLabel(value, isValueX) + " mm";
                            }
                        }
                    }
                });
                seriesRD.setColor(Color.GRAY);
            } else if(viewSel=="rawData") {
                text.setVisibility(View.VISIBLE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.INVISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                text.setTextSize(16);
                text.setText(rawData);
            }

        } catch (ArrayIndexOutOfBoundsException a) {
            Context context = getApplicationContext();
            CharSequence toastText = "Connection Error";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, toastText, duration);
            toast.show();
            throw a;
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
    //Error handling that sends the user back to either settings or date selection.
    // If error is throw on date selection more than once the user will be sent to settings
    public void error() {
        if(errSrcId==0) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("error", true);
            startActivityForResult(intent, 2);
        }
        else{
            errBool=false;
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("error", true);
            startActivityForResult(intent, 2);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    //Menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent setIntent = new Intent(this, SettingsActivity.class);
        if (id==R.id.action_refresh){
            TextView text =(TextView) findViewById(R.id.text1);
            text.setTextSize(30);

            new datagrab().execute("");
        }
        if (id == R.id.action_settings) {
            setIntent.putExtra("error",false);
            setIntent.putExtra("unit",units);
            startActivityForResult(setIntent,0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        errSrcId=requestCode;
        if(requestCode==0){
            units=resultCode;
            try{
                nordic=data.getIntExtra("nordic",0);
                //Write unit code to file
                try {
                    FileOutputStream fos = openFileOutput("unit_file", Context.MODE_PRIVATE);
                    fos.write(units);
                    fos.close();
                }
                catch (IOException e){
                    System.out.println(e);
                }
                try {
                    FileOutputStream fos = openFileOutput("nordic_file", Context.MODE_PRIVATE);
                    fos.write(nordic);
                    fos.close();
                }
                catch (IOException e) {
                    System.out.println(e);
                }
            }
            catch(NullPointerException n){
                System.out.println(n);
            }
        }
        else{
            //Sets current date to selected date
            try{
                calDate=data.getStringExtra("calDate");
                day = calDate.split(",")[0];
                month = calDate.split(",")[1];
                year = calDate.split(",")[2];
            }
            //If error default to today
            catch(NullPointerException n) {
                day = new SimpleDateFormat("dd").format(Calendar.getInstance().getTime());
                month = new SimpleDateFormat("MM").format(Calendar.getInstance().getTime());
                year = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());
            }
        }
        //Regrabs data
        new datagrab().execute("");
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        //Navigation bar selection same as similar section in datagrab()
        int id = item.getItemId();
        TextView text =(TextView) findViewById(R.id.text1);
        TextView temp =(TextView) findViewById(R.id.temp);
        TextView tempHigh =(TextView) findViewById(R.id.highTemp);
        TextView tempLow =(TextView) findViewById(R.id.lowTemp);
        TextView dew =(TextView) findViewById(R.id.dew);
        TextView dewHigh =(TextView) findViewById(R.id.highDew);
        TextView dewLow =(TextView) findViewById(R.id.lowDew);
        TextView press =(TextView) findViewById(R.id.press);
        TextView windLabel =(TextView) findViewById(R.id.windLabel);
        TextView windSpeed =(TextView) findViewById(R.id.windSpeed);
        TextView windGust =(TextView) findViewById(R.id.windGust);
        TextView windDir =(TextView) findViewById(R.id.windDir);
        TextView windDeg =(TextView) findViewById(R.id.windDeg);
        TextView rain =(TextView) findViewById(R.id.rain);
        TextView hum =(TextView) findViewById(R.id.hum);
        TextView time =(TextView) findViewById(R.id.date);
        ProgressBar humBar=(ProgressBar) findViewById(R.id.humBar);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        ImageView statusIcon=(ImageView) findViewById(R.id.statusIcon);
        ImageView windIcon=(ImageView) findViewById(R.id.windIcon);
        ImageView windDirIcon=(ImageView) findViewById(R.id.windDirIcon);
        ImageView snowIcon=(ImageView) findViewById(R.id.snowIcon);
        ImageView pressChange=(ImageView) findViewById(R.id.pressChange);
        try {
            if (id == R.id.nav_current) {
                viewSel = "current";
                text.setVisibility(View.INVISIBLE);
                temp.setVisibility(View.VISIBLE);
                tempHigh.setVisibility(View.INVISIBLE);
                tempLow.setVisibility(View.INVISIBLE);
                dew.setVisibility(View.VISIBLE);
                dewHigh.setVisibility(View.INVISIBLE);
                dewLow.setVisibility(View.INVISIBLE);
                press.setVisibility(View.VISIBLE);
                windSpeed.setVisibility(View.VISIBLE);
                windGust.setVisibility(View.VISIBLE);
                windDir.setVisibility(View.VISIBLE);
                windDeg.setVisibility(View.VISIBLE);
                rain.setVisibility(View.VISIBLE);
                hum.setVisibility(View.VISIBLE);
                time.setVisibility(View.VISIBLE);
                humBar.setVisibility(View.VISIBLE);
                windLabel.setVisibility(View.VISIBLE);
                graph.setVisibility(View.GONE);
                windIcon.setVisibility(View.VISIBLE);
                statusIcon.setVisibility(View.VISIBLE);
                windDirIcon.setVisibility(View.VISIBLE);
                pressChange.setVisibility(View.VISIBLE);

                time.setText(currentString.split("\n")[0].split(": ")[1]);
                temp.setText(currentString.split("\n")[1].split(": ")[1]);
                dew.setText("Dew: " + currentString.split("\n")[2].split(": ")[1]);
                press.setText("Press: " + currentString.split("\n")[3].split(": ")[1]);
                windDir.setText(currentString.split("\n")[5].split(": ")[1]);
                windDeg.setText(currentString.split("\n")[6].split(": ")[1]);
                windSpeed.setText(currentString.split("\n")[7].split(": ")[1]);
                windGust.setText("Gust: " + currentString.split("\n")[8].split(": ")[1]);
                hum.setText("Humidity: " + currentString.split("\n")[9].split(": ")[1]);
                humBar.setProgress((int) Float.parseFloat(currentString.split("\n")[9].split(": ")[1].split(" %")[0]));
                rain.setText("Hourly Rain: " + currentString.split("\n")[10].split(": ")[1]);

                //Pressure derivative icon. If press-mean>10% mean then positive, < then negative, else null
                if (Float.parseFloat(currentString.split("\n")[3].split(": ")[1].split(" ")[0]) - Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0]) > 0.01 * Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0])) {
                    pressChange.setImageResource(R.mipmap.press_change);
                } else if (Float.parseFloat(currentString.split("\n")[3].split(": ")[1].split(" ")[0]) - Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0]) < -0.01 * Float.parseFloat(dailyString.split("\n")[8].split(": ")[1].split(" ")[0])) {
                    pressChange.setImageResource(R.mipmap.press_change_n);
                } else {
                    pressChange.setImageResource(R.mipmap.press_null);
                }

                if ((Float.parseFloat(currentString.split("\n")[10].split(": ")[1].split(" ")[0])) > 0) {
                    statusIcon.setImageResource(R.mipmap.rain_icon);
                } else if ((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0])) > 70 && units == 0) {
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                } else if ((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0])) > 20 && units == 1) {
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                } else {
                    statusIcon.setImageResource(R.mipmap.cloud_icon);
                }

                if ((Float.parseFloat(currentString.split("\n")[8].split(": ")[1].split(" ")[0])) > 4) {
                    windIcon.setVisibility(View.VISIBLE);
                } else {
                    windIcon.setVisibility(View.INVISIBLE);
                }

                if ((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0])) < 32 && units == 0) {
                    snowIcon.setVisibility(View.VISIBLE);
                } else if ((Float.parseFloat(currentString.split("\n")[1].split(": ")[1].split(" ")[0])) < 0 && units == 1) {
                    snowIcon.setVisibility(View.VISIBLE);
                } else {
                    snowIcon.setVisibility(View.INVISIBLE);
                }
                try {
                    windDirIcon.setRotation((Float.parseFloat(currentString.split("\n")[6].split(": ")[1].split(" ")[0])));
                }
                catch (NumberFormatException n){
                    windDirIcon.setRotation(0);
                }
            } else if (id == R.id.nav_daily) {
                viewSel = "daily";
                text.setVisibility(View.INVISIBLE);
                temp.setVisibility(View.VISIBLE);
                tempHigh.setVisibility(View.VISIBLE);
                tempLow.setVisibility(View.VISIBLE);
                dew.setVisibility(View.VISIBLE);
                dewHigh.setVisibility(View.VISIBLE);
                dewLow.setVisibility(View.VISIBLE);
                press.setVisibility(View.VISIBLE);
                windSpeed.setVisibility(View.VISIBLE);
                windGust.setVisibility(View.VISIBLE);
                windDir.setVisibility(View.VISIBLE);
                windDeg.setVisibility(View.INVISIBLE);
                rain.setVisibility(View.VISIBLE);
                hum.setVisibility(View.VISIBLE);
                time.setVisibility(View.INVISIBLE);
                humBar.setVisibility(View.VISIBLE);
                windLabel.setVisibility(View.VISIBLE);
                graph.setVisibility(View.GONE);
                windIcon.setVisibility(View.VISIBLE);
                statusIcon.setVisibility(View.VISIBLE);
                windDirIcon.setVisibility(View.VISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                temp.setText(dailyString.split("\n")[1].split(": ")[1]);
                tempHigh.setText(dailyString.split("\n")[2].split(": ")[1]);
                tempLow.setText(dailyString.split("\n")[3].split(": ")[1]);
                dew.setText("Dew: " + dailyString.split("\n")[5].split(": ")[1]);
                dewHigh.setText(dailyString.split("\n")[6].split(": ")[1]);
                dewLow.setText(dailyString.split("\n")[7].split(": ")[1]);
                press.setText("Press: " + dailyString.split("\n")[8].split(": ")[1]);
                windDir.setText(dailyString.split("\n")[9].split(": ")[1]);
                windSpeed.setText(dailyString.split("\n")[10].split(": ")[1]);
                windGust.setText("Gust: " + dailyString.split("\n")[11].split(": ")[1]);
                hum.setText("Humidity: " + dailyString.split("\n")[12].split(": ")[1]);
                humBar.setProgress((int) Float.parseFloat(dailyString.split("\n")[12].split(": ")[1].split(" %")[0]));
                rain.setText("Daily Rain: " + dailyString.split("\n")[13].split(": ")[1]);
                if ((Float.parseFloat(dailyString.split("\n")[13].split(": ")[1].split(" ")[0])) > 0) {
                    statusIcon.setImageResource(R.mipmap.rain_icon);
                } else if ((Float.parseFloat(dailyString.split("\n")[2].split(": ")[1].split(" ")[0])) > 70 && units == 0) {
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                } else if ((Float.parseFloat(dailyString.split("\n")[2].split(": ")[1].split(" ")[0])) > 20 && units == 1) {
                    statusIcon.setImageResource(R.mipmap.sun_icon);
                } else {
                    statusIcon.setImageResource(R.mipmap.cloud_icon);
                }
                if ((Float.parseFloat(dailyString.split("\n")[11].split(": ")[1].split(" ")[0])) > 4) {
                    windIcon.setVisibility(View.VISIBLE);
                } else {
                    windIcon.setVisibility(View.INVISIBLE);
                }
                if ((Float.parseFloat(dailyString.split("\n")[1].split(": ")[1].split(" ")[0])) < 32 && units == 0) {
                    snowIcon.setVisibility(View.VISIBLE);
                } else if ((Float.parseFloat(dailyString.split("\n")[1].split(": ")[1].split(" ")[0])) < 0 && units == 1) {
                    snowIcon.setVisibility(View.VISIBLE);
                } else {
                    snowIcon.setVisibility(View.INVISIBLE);
                }
                windDirIcon.setRotation((Float.parseFloat(dailyString.split("\n")[9].split(": ")[1].split(" ")[0])));
            } else if(id == R.id.nav_raw) {
                viewSel = "rawData";

                text.setVisibility(View.VISIBLE);
                temp.setVisibility(View.GONE);
                tempHigh.setVisibility(View.GONE);
                tempLow.setVisibility(View.GONE);
                dew.setVisibility(View.GONE);
                dewHigh.setVisibility(View.GONE);
                dewLow.setVisibility(View.GONE);
                press.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                windGust.setVisibility(View.GONE);
                windDir.setVisibility(View.GONE);
                windDeg.setVisibility(View.GONE);
                rain.setVisibility(View.GONE);
                hum.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                humBar.setVisibility(View.GONE);
                windLabel.setVisibility(View.GONE);
                graph.setVisibility(View.INVISIBLE);
                windIcon.setVisibility(View.INVISIBLE);
                statusIcon.setVisibility(View.INVISIBLE);
                windDirIcon.setVisibility(View.INVISIBLE);
                snowIcon.setVisibility(View.INVISIBLE);
                pressChange.setVisibility(View.INVISIBLE);

                text.setTextSize(16);
                text.setText(rawData);
            }else if(id == R.id.nav_paid) {
                try{
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=net.mpross.pwspaid")));
                }catch(android.content.ActivityNotFoundException a){
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=net.mpross.pwspaid")));
                }
            }

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        catch(ArrayIndexOutOfBoundsException a) {
            new datagrab().execute("");
            return true;
        }
    }

    // Orientation change handling
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        new datagrab().execute("");
    }
}
