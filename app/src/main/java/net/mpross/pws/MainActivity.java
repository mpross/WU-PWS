package net.mpross.pws;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
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
    String currentString=new String();
    String dailyString=new String();
    String station="";
    static String viewSel="current";
    int units=0;
    LineGraphSeries<DataPoint> seriesT = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesD = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesP = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesWS = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesWG = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesH = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesR = new LineGraphSeries<>();
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

        try {
            FileInputStream fis = openFileInput("unit_file");
            fis.read(byU);
            fis.close();
            units=(int)byU[0];
        }
        catch (IOException e){
            System.out.println(e);
        }

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
            byte[] by=new byte[11];
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

                float[] temp = new float[(int)lines.length/2-1];
                float[] dew = new float[lines.length/2-1];
                float[] press = new float[lines.length/2-1];
                float[] windDeg = new float[lines.length/2-1];
                float[] windSpeed = new float[lines.length/2-1];
                float[] windGust = new float[lines.length/2-1];
                float[] hum = new float[lines.length/2-1];
                float[] precip = new float[lines.length/2-1];
                float[] tim=new float[lines.length/2-1];

                int m=0;

                float tempAvg = 0;
                float tempHigh=-1000;
                float tempLow=1000;
                float dewHigh=-1000;
                float dewLow=1000;
                float dewAvg = 0;
                float pressAvg = 0;
                String windDir="";
                float windDAvg = 0;
                float windSAvg = 0;
                float windG = 0;
                float humAvg = 0;
                float precipMax = 0;
                String timStamp="";
                int j = 1;
                for (String line : lines) {
                    String[] col = line.split(",");
                    if (col.length > 1 && j > 2) {
                        timStamp=col[0];
                        tim[(int)j/2-1]=Float.parseFloat(col[0].split(" ")[1].split(":")[0])+Float.parseFloat(col[0].split(" ")[1].split(":")[1])/60
                                +Float.parseFloat(col[0].split(" ")[1].split(":")[2])/3600;
                        if (Float.parseFloat(col[1]) > 0) {
                            if(units==0) {
                                temp[(int)j/2-1] = Float.parseFloat(col[1]);
                            }
                            else{
                                temp[(int)j/2-1] = (Float.parseFloat(col[1])-32.0f)*5.0f/9.0f;
                            }
                            if(temp[(int)j/2-1]<tempLow){
                                tempLow=temp[(int)j/2-1];
                            }
                            if(temp[(int)j/2-1]>tempHigh){
                                tempHigh=temp[(int)j/2-1];
                            }
                        }
                        if (Float.parseFloat(col[2]) > 0) {
                            if(units==0) {
                                dew[(int)j/2-1] = Float.parseFloat(col[2]);
                            }
                            else{
                                dew[(int)j/2-1] = (Float.parseFloat(col[2])-32.0f)*5.0f/9.0f;
                            }
                            if(dew[(int)j/2-1]<dewLow){
                                dewLow=dew[(int)j/2-1];
                            }
                            if(dew[(int)j/2-1]>dewHigh){
                                dewHigh=dew[(int)j/2-1];
                            }
                        }
                        if (Float.parseFloat(col[3]) > 0) {
                            if(units==0) {
                                press[(int)j/2-1] = Float.parseFloat(col[3]);
                            }
                            else {
                                press[(int)j/2-1] = Float.parseFloat(col[3])*3.38639f;
                            }
                        }
                        windDir=col[4];
                        if (Float.parseFloat(col[5]) > 0) {
                            windDeg[(int)j/2-1] = Float.parseFloat(col[5]);
                        }
                        if (Float.parseFloat(col[6]) > 0) {
                            if(units==0) {
                                windSpeed[(int)j/2-1] = Float.parseFloat(col[6]);
                            }
                            else{
                                windSpeed[(int)j/2-1] = Float.parseFloat(col[6])*0.44704f;
                            }
                        }
                        if (Float.parseFloat(col[7]) > 0) {
                            if(units==0) {
                                windGust[(int)j/2-1] = Float.parseFloat(col[7]);
                            }
                            else{
                                windGust[(int)j/2-1] = Float.parseFloat(col[7])*0.44704f;
                            }
                        }
                        if (Float.parseFloat(col[8]) > 0) {
                            hum[(int)j/2-1] = Float.parseFloat(col[8]);
                        }
                        if (Float.parseFloat(col[9]) > 0) {
                            if(units==1) {
                                precip[(int)j/2-1] = Float.parseFloat(col[9]);
                            }
                            else{
                                precip[(int)j/2-1] = Float.parseFloat(col[9])*25.4f;
                            }
                        }

                        tempAvg += temp[(int)j/2-1];
                        dewAvg += dew[(int)j/2-1];
                        pressAvg += press[(int)j/2-1];
                        windDAvg += windDeg[(int)j/2-1];
                        windSAvg += windSpeed[(int)j/2-1];
                        humAvg += hum[(int)j/2-1];

                        if (windGust[(int)j/2-1] > windG) {
                            windG = windGust[(int)j/2-1];
                        }

                        if (Float.parseFloat(col[12]) > precipMax) {
                            precipMax = Float.parseFloat(col[12]);
                        }


                    }
                    j++;
                }
                DataPoint[] tempData=new DataPoint[temp.length];
                DataPoint[] dewData=new DataPoint[dew.length];
                DataPoint[] pressData=new DataPoint[press.length];
                DataPoint[] windSData=new DataPoint[windSpeed.length];
                DataPoint[] windGData=new DataPoint[windGust.length];
                DataPoint[] humData=new DataPoint[hum.length];
                DataPoint[] rainData=new DataPoint[precip.length];
                m=0;
                for(float t:temp){
                    tempData[m] = new DataPoint(tim[m], t);
                    dewData[m] = new DataPoint(tim[m], dew[m]);
                    pressData[m]=new DataPoint(tim[m],press[m]);
                    windSData[m]=new DataPoint(tim[m],windSpeed[m]);
                    windGData[m]=new DataPoint(tim[m],windGust[m]);
                    humData[m]=new DataPoint(tim[m],hum[m]);
                    rainData[m]=new DataPoint(tim[m],precip[m]);
                    m++;
                }
                seriesT = new LineGraphSeries<>(tempData);
                seriesD = new LineGraphSeries<>(dewData);
                seriesP = new LineGraphSeries<>(pressData);
                seriesWS = new LineGraphSeries<>(windSData);
                seriesWG = new LineGraphSeries<>(windGData);
                seriesH = new LineGraphSeries<>(humData);
                seriesR = new LineGraphSeries<>(rainData);

                seriesD.setTitle("Dew Point");
                seriesT.setTitle("Temperature");
                seriesWS.setTitle("Wind Speed");
                seriesWG.setTitle("Wind Gust");
                seriesH.setTitle("Humidity");
                seriesP.setTitle("Pressure");
                seriesR.setTitle("Hourly Precipitation");

                tempAvg /= j / 2;
                dewAvg /= j / 2;
                pressAvg /= j / 2;
                windDAvg /= j / 2;
                windSAvg /= j / 2;
                humAvg /= j / 2;
                if(units==0){
                    outBuild.append(lines[lines.length - 2]);
                }
                else{
                    outBuild.append(timStamp);
                    outBuild.append(",");
                    outBuild.append(String.valueOf(Math.round(temp[temp.length-2]* 100.0) / 100.0));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(Math.round(dew[dew.length-2]* 100.0) / 100.0));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(Math.round(press[press.length-2]* 100.0) / 100.0));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(windDir));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(windDeg[windDeg.length-2]));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length-2]* 100.0) / 100.0));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(Math.round(windGust[windGust.length-2]* 100.0) / 100.0));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(hum[hum.length-2]));
                    outBuild.append(",");
                    outBuild.append(String.valueOf(Math.round(precip[precip.length-2]* 100.0) / 100.0));
                    outBuild.append(",,,,,,");
                }

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
            String fieldString="Date & Time,Temperature,Dewpoint,Pressure,Wind: \n" +
                    "     Direction,     Direction,     Speed," +
                    "     Gust,Humidity,Hourly Precip,Conditions,Clouds,Daily Rain,SoftwareType,DateUTC";
            String fieldStringD="Temperature: \n     Average,     High,     Low,Dewpoint: \n" +
                    "     Average,     High,     Low,Average Pressure,Average Wind Direction," +
                    "Average Wind Speed,Maximum Wind Gust,Average Humidity,Daily Rain";
            String endString = "";
            String endStringD = "";
            if(units==0) {
                endString = ", °F, °F, inHg,, °, mph, mph, %, in,,, in,,";
                endStringD = " °F, °F, °F, °F, °F, °F, inHg, °, mph, mph, %, in";
            }
            else{
                endString = ", °C, °C, kPa,, °, m/s, m/s, %, mm,,, mm,,";
                endStringD = " °C, °C, °C, °C, °C, °C, kPa, °, m/s, m/s, %, mm";
            }
            String exString="Conditions,Clouds,SoftwareType,DateUTC,Daily Rain";
            String[] split=result.split(";");
            String[] dataCur=split[0].split(",");
            String[] dataDay=split[1].split(",");
            StringBuilder outCur=new StringBuilder();
            StringBuilder outDay=new StringBuilder();
            TextView text =(TextView) findViewById(R.id.text1);
            String station="";
            byte[] by=new byte[11];
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

            GraphView graph = (GraphView) findViewById(R.id.graph);

            if (viewSel=="current") {

                text.setVisibility(View.VISIBLE);
                graph.setVisibility(View.GONE);
                text.setText(currentString.toString());
            } else if (viewSel=="daily") {

                text.setVisibility(View.VISIBLE);
                graph.setVisibility(View.GONE);
                text.setText(dailyString.toString());
            }
            else if (viewSel=="tempPlot"){

                text.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);
                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesT.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if (seriesT.getLowestValueY()<seriesD.getLowestValueY()) {
                    if(seriesT.getLowestValueY()>0) {
                        graph.getViewport().setMinY(seriesT.getLowestValueY() * .9);
                    }
                    else{
                        graph.getViewport().setMinY(seriesT.getLowestValueY() * 1.1);
                    }
                }
                else{
                    if(seriesD.getLowestValueY()>0) {
                        graph.getViewport().setMinY(seriesD.getLowestValueY() * .9);
                    }
                    else{
                        graph.getViewport().setMinY(seriesD.getLowestValueY() * 1.1);
                    }
                }
                if (seriesT.getHighestValueY()>seriesD.getHighestValueY()) {
                    if(seriesT.getHighestValueY()>0) {
                        graph.getViewport().setMaxY(seriesT.getHighestValueY() * 1.1);
                    }
                    else{
                        graph.getViewport().setMaxY(seriesT.getHighestValueY() * 0.9);
                    }
                }
                else{
                    if(seriesD.getHighestValueY()>0) {
                        graph.getViewport().setMaxY(seriesD.getHighestValueY() * 1.1);
                    }
                    else{
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
                            return super.formatLabel(value, isValueX)+" h";
                        } else {
                            if(units==0) {
                                return super.formatLabel(value, isValueX) + " °F";
                            }
                            else{
                                return super.formatLabel(value, isValueX) + " °C";
                            }
                        }
                    }
                });
                seriesD.setColor(Color.GRAY);
            }
            else if (viewSel=="pressPlot"){

                text.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesP.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if(seriesP.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesP.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesP.getLowestValueY() * 1.1);
                }
                if(seriesP.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesP.getHighestValueY() * 1.1);
                }
                else{
                    graph.getViewport().setMaxY(seriesP.getHighestValueY() * 0.9);
                }
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
                            return super.formatLabel(value, isValueX)+" h";
                        } else {
                            if(units==0) {
                                return super.formatLabel(value, isValueX) + " inHg";
                            }
                            else{
                                return super.formatLabel(value, isValueX) + " kPa";
                            }
                        }
                    }
                });
            }
            else if (viewSel=="windPlot"){
                text.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesT.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if (seriesWS.getLowestValueY()<seriesWG.getLowestValueY()) {
                    if(seriesWS.getLowestValueY()>0) {
                        graph.getViewport().setMinY(seriesWS.getLowestValueY() * .9);
                    }
                    else{
                        graph.getViewport().setMinY(seriesWS.getLowestValueY() * 1.1);
                    }
                }
                else{
                    if(seriesWG.getLowestValueY()>0) {
                        graph.getViewport().setMinY(seriesWG.getLowestValueY() * .9);
                    }
                    else{
                        graph.getViewport().setMinY(seriesWG.getLowestValueY() * 1.1);
                    }
                }
                if (seriesWS.getHighestValueY()>seriesWG.getHighestValueY()) {
                    if(seriesWS.getHighestValueY()>0) {
                        graph.getViewport().setMaxY(seriesWS.getHighestValueY() * 1.1);
                    }
                    else{
                        graph.getViewport().setMaxY(seriesWS.getHighestValueY() * 0.9);
                    }
                }
                else{
                    if(seriesWG.getHighestValueY()>0) {
                        graph.getViewport().setMaxY(seriesWG.getHighestValueY() * 1.1);
                    }
                    else{
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
                            return super.formatLabel(value, isValueX)+" h";
                        } else {
                            if(units==0) {
                                return super.formatLabel(value, isValueX) + " mph";
                            }
                            else{
                                return super.formatLabel(value, isValueX) + " m/s";
                            }
                        }
                    }
                });

                seriesWG.setColor(Color.GRAY);
            }
            else if (viewSel=="humPlot"){

                text.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesH.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if(seriesP.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesH.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesH.getLowestValueY() * 1.1);
                }
                if(seriesP.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesH.getHighestValueY() * 1.1);
                }
                else{
                    graph.getViewport().setMaxY(seriesH.getHighestValueY() * 0.9);
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
                            return super.formatLabel(value, isValueX)+" h";
                        } else {
                            return super.formatLabel(value, isValueX) + " %";
                        }
                    }
                });
            }
            else if (viewSel=="rainPlot"){

                text.setVisibility(View.GONE);
                graph.setVisibility(View.VISIBLE);

                graph.removeAllSeries();

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(0);
                graph.getViewport().setMaxX(seriesR.getHighestValueX());
                graph.getViewport().setYAxisBoundsManual(true);
                if(seriesP.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesR.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesR.getLowestValueY() * 1.1);
                }
                if(seriesP.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesR.getHighestValueY() * 1.1);
                }
                else{
                    graph.getViewport().setMaxY(seriesR.getHighestValueY() * 0.9);
                }
                graph.getLegendRenderer().setVisible(true);
                graph.getLegendRenderer().setTextSize(40f);
                graph.getLegendRenderer().setSpacing(30);
                graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

                graph.addSeries(seriesR);
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return super.formatLabel(value, isValueX)+" h";
                        } else {
                            if(units==0) {
                                return super.formatLabel(value, isValueX) + " in";
                            }
                            else{
                                return super.formatLabel(value, isValueX) + " mm";
                            }
                        }
                    }
                });
            }

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
                if (viewSel=="current") {
                    text.setText(currentString);
                }
                if (viewSel=="daily") {
                    text.setText(dailyString);
                }
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
            intent.putExtra("unit",units);
            startActivityForResult(intent,result);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        units=resultCode;
        try {
            FileOutputStream fos = openFileOutput("unit_file", Context.MODE_PRIVATE);
            fos.write(units);
            fos.close();
        }
        catch (IOException e){
            System.out.println(e);
        }
        new datagrab().execute("");
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        TextView text =(TextView) findViewById(R.id.text1);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        if (id == R.id.nav_current) {
            viewSel="current";
            text.setVisibility(View.VISIBLE);
            graph.setVisibility(View.GONE);
            text.setText(currentString.toString());
        } else if (id == R.id.nav_daily) {
            viewSel="daily";
            text.setVisibility(View.VISIBLE);
            graph.setVisibility(View.GONE);
            text.setText(dailyString.toString());
        }
        else if (id==R.id.nav_dailyPlot){
            viewSel="tempPlot";
            text.setVisibility(View.GONE);
            graph.setVisibility(View.VISIBLE);
            graph.removeAllSeries();

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(seriesT.getHighestValueX());
            graph.getViewport().setYAxisBoundsManual(true);
            if (seriesT.getLowestValueY()<seriesD.getLowestValueY()) {
                if(seriesT.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesT.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesT.getLowestValueY() * 1.1);
                }
            }
            else{
                if(seriesD.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesD.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesD.getLowestValueY() * 1.1);
                }
            }
            if (seriesT.getHighestValueY()>seriesD.getHighestValueY()) {
                if(seriesT.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesT.getHighestValueY() * 1.1);
                }
                else{
                    graph.getViewport().setMaxY(seriesT.getHighestValueY() * 0.9);
                }
            }
            else{
                if(seriesD.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesD.getHighestValueY() * 1.1);
                }
                else{
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
                        return super.formatLabel(value, isValueX)+" h";
                    } else {
                        if(units==0) {
                            return super.formatLabel(value, isValueX) + " °F";
                        }
                        else{
                            return super.formatLabel(value, isValueX) + " °C";
                        }
                    }
                }
            });
            seriesD.setColor(Color.GRAY);
        }
        else if (id==R.id.nav_pressPlot){
            viewSel="pressPlot";
            text.setVisibility(View.GONE);
            graph.setVisibility(View.VISIBLE);

            graph.removeAllSeries();

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(seriesP.getHighestValueX());
            graph.getViewport().setYAxisBoundsManual(true);
            if(seriesP.getLowestValueY()>0) {
                graph.getViewport().setMinY(seriesP.getLowestValueY() * .9);
            }
            else{
                graph.getViewport().setMinY(seriesP.getLowestValueY() * 1.1);
            }
            if(seriesP.getHighestValueY()>0) {
                graph.getViewport().setMaxY(seriesP.getHighestValueY() * 1.1);
            }
            else{
                graph.getViewport().setMaxY(seriesP.getHighestValueY() * 0.9);
            }
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
                        return super.formatLabel(value, isValueX)+" h";
                    } else {
                        if(units==0) {
                            return super.formatLabel(value, isValueX) + " inHg";
                        }
                        else{
                            return super.formatLabel(value, isValueX) + " kPa";
                        }
                    }
                }
            });
        }
        else if (id==R.id.nav_windPlot){
            viewSel="windPlot";
            text.setVisibility(View.GONE);
            graph.setVisibility(View.VISIBLE);

            graph.removeAllSeries();

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(seriesT.getHighestValueX());
            graph.getViewport().setYAxisBoundsManual(true);
            if (seriesWS.getLowestValueY()<seriesWG.getLowestValueY()) {
                if(seriesWS.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesWS.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesWS.getLowestValueY() * 1.1);
                }
            }
            else{
                if(seriesWG.getLowestValueY()>0) {
                    graph.getViewport().setMinY(seriesWG.getLowestValueY() * .9);
                }
                else{
                    graph.getViewport().setMinY(seriesWG.getLowestValueY() * 1.1);
                }
            }
            if (seriesWS.getHighestValueY()>seriesWG.getHighestValueY()) {
                if(seriesWS.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesWS.getHighestValueY() * 1.1);
                }
                else{
                    graph.getViewport().setMaxY(seriesWS.getHighestValueY() * 0.9);
                }
            }
            else{
                if(seriesWG.getHighestValueY()>0) {
                    graph.getViewport().setMaxY(seriesWG.getHighestValueY() * 1.1);
                }
                else{
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
                        return super.formatLabel(value, isValueX)+" h";
                    } else {
                        if(units==0) {
                            return super.formatLabel(value, isValueX) + " mph";
                        }
                        else{
                            return super.formatLabel(value, isValueX) + " m/s";
                        }
                    }
                }
            });

            seriesWG.setColor(Color.GRAY);
        }
        else if (id==R.id.nav_humPlot){
            viewSel="humPlot";
            text.setVisibility(View.GONE);
            graph.setVisibility(View.VISIBLE);

            graph.removeAllSeries();

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(seriesH.getHighestValueX());
            graph.getViewport().setYAxisBoundsManual(true);
            if(seriesP.getLowestValueY()>0) {
                graph.getViewport().setMinY(seriesH.getLowestValueY() * .9);
            }
            else{
                graph.getViewport().setMinY(seriesH.getLowestValueY() * 1.1);
            }
            if(seriesP.getHighestValueY()>0) {
                graph.getViewport().setMaxY(seriesH.getHighestValueY() * 1.1);
            }
            else{
                graph.getViewport().setMaxY(seriesH.getHighestValueY() * 0.9);
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
                        return super.formatLabel(value, isValueX)+" h";
                    } else {
                        return super.formatLabel(value, isValueX) + " %";
                    }
                }
            });
        }
        else if (id==R.id.nav_rainPlot){
            viewSel="rainPlot";
            text.setVisibility(View.GONE);
            graph.setVisibility(View.VISIBLE);

            graph.removeAllSeries();

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(seriesR.getHighestValueX());
            graph.getViewport().setYAxisBoundsManual(true);
            if(seriesP.getLowestValueY()>0) {
                graph.getViewport().setMinY(seriesR.getLowestValueY() * .9);
            }
            else{
                graph.getViewport().setMinY(seriesR.getLowestValueY() * 1.1);
            }
            if(seriesP.getHighestValueY()>0) {
                graph.getViewport().setMaxY(seriesR.getHighestValueY() * 1.1);
            }
            else{
                graph.getViewport().setMaxY(seriesR.getHighestValueY() * 0.9);
            }
            graph.getLegendRenderer().setVisible(true);
            graph.getLegendRenderer().setTextSize(40f);
            graph.getLegendRenderer().setSpacing(30);
            graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
            graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);

            graph.addSeries(seriesR);
            graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        return super.formatLabel(value, isValueX)+" h";
                    } else {
                        if(units==0) {
                            return super.formatLabel(value, isValueX) + " in";
                        }
                        else{
                            return super.formatLabel(value, isValueX) + " mm";
                        }
                    }
                }
            });
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        new datagrab().execute("");
        super.onConfigurationChanged(newConfig);
    }
}
