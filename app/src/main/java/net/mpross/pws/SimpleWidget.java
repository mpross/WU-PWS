package net.mpross.pws;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.RemoteViews;

import net.mpross.pws.util.IabHelper;
import net.mpross.pws.util.IabResult;
import net.mpross.pws.util.Inventory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Implementation of App Widget functionality.
 */
public class SimpleWidget extends AppWidgetProvider {
    String station = ""; //Weather station name
    int units = 0; // User unit choice
    int nativeUnits = 0; // Units the data is in
    int nordic=0; // Wind speed unit choice
    Context con;
    public String widText="Loading";

    public static final String ACTION_AUTO_UPDATE = "AUTO_UPDATE";

    public class datagrab extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] p1) {

            byte[] by = new byte[13];
            byte[] byU = new byte[1];
            CharSequence widgetText = "";
            //Reads station id from station file
            try {
                FileInputStream fis = con.openFileInput("station_file");
                int n = fis.read(by);
                fis.close();
                station = new String(by, "UTF-8");
            } catch (IOException e) {
                widgetText = e.toString();
            }

            try {
                FileInputStream fis = con.openFileInput("unit_file");
                fis.read(byU);
                fis.close();
                units = (int) byU[0];
            } catch (IOException e) {
                widgetText = e.toString();
            }
            try {
                FileInputStream fis = con.openFileInput("nordic_file");
                fis.read(byU);
                fis.close();
                nordic = (int) byU[0];
            } catch (IOException e) {
                widgetText = e.toString();
            }

            String day = new SimpleDateFormat("dd").format(Calendar.getInstance().getTime());
            String month = new SimpleDateFormat("MM").format(Calendar.getInstance().getTime());
            String year = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());

            //Builds URL for data file
            StringBuilder url = new StringBuilder();
            url.append("https://www.wunderground.com/weatherstation/WXDailyHistory.asp?");
            url.append("ID=" + station);
            url.append("&day=" + day);
            url.append("&month=" + month);
            url.append("&year=" + year);
            url.append("&graphspan=day&format=1");

            try {

                //Removes non ASCII character from URL
                URL site = new URL(url.toString().replaceAll("\\P{Print}", ""));
                //Reads file
                BufferedReader data = new BufferedReader(
                        new InputStreamReader(site.openStream()));
                //Puts return character at end of every line
                String in;
                StringBuilder build = new StringBuilder();
                StringBuilder outBuild = new StringBuilder();
                while ((in = data.readLine()) != null)
                    build.append(in + "\r");
                //Splits lines
                String[] lines = build.toString().split("\r");

                //Data vector initialization
                float[] temp = new float[lines.length / 2 - 1];
                float[] dew = new float[lines.length / 2 - 1];
                float[] press = new float[lines.length / 2 - 1];
                float[] windDeg = new float[lines.length / 2 - 1];
                float[] windSpeed = new float[lines.length / 2 - 1];
                float[] windGust = new float[lines.length / 2 - 1];
                float[] hum = new float[lines.length / 2 - 1];
                float[] precip = new float[lines.length / 2 - 1];
                float[] precipDay = new float[lines.length / 2 - 1];
                float[] tim = new float[lines.length / 2 - 1];

                int m = 0;

                String timStamp = "";
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
                        tim[j / 2 - 1] = Float.parseFloat(col[0].split(" ")[1].split(":")[0]) + Float.parseFloat(col[0].split(" ")[1].split(":")[1]) / 60
                                + Float.parseFloat(col[0].split(" ")[1].split(":")[2]) / 3600;
                        //Drop out handling
                        //If data is in imperial
                        if (nativeUnits == 0) {
                            if (units == 0) {
                                temp[j / 2 - 1] = Float.parseFloat(col[1]);
                                dew[j / 2 - 1] = Float.parseFloat(col[2]);
                                press[j / 2 - 1] = Float.parseFloat(col[3]);
                                windSpeed[j / 2 - 1] = Float.parseFloat(col[6]);
                                windGust[j / 2 - 1] = Float.parseFloat(col[7]);
                                precip[j / 2 - 1] = Float.parseFloat(col[9]);
                            } else {
                                temp[j / 2 - 1] = (Float.parseFloat(col[1]) - 32.0f) * 5.0f / 9.0f;
                                dew[j / 2 - 1] = (Float.parseFloat(col[2]) - 32.0f) * 5.0f / 9.0f;
                                press[j / 2 - 1] = Float.parseFloat(col[3]) * 33.8639f;
                                precip[j / 2 - 1] = Float.parseFloat(col[9]) * 25.4f;
                                if(nordic==0) {
                                    windSpeed[j / 2 - 1] = Float.parseFloat(col[6]) * 1.60934f;
                                    windGust[j / 2 - 1] = Float.parseFloat(col[7]) * 1.60934f;
                                }
                                else{
                                    windSpeed[j / 2 - 1] = Float.parseFloat(col[6]) * 0.44704f;
                                    windGust[j / 2 - 1] = Float.parseFloat(col[7]) * 0.44704f;
                                }
                            }
                        }
                        //If data is in metric
                        else {
                            if (units == 0) {
                                temp[j / 2 - 1] = (Float.parseFloat(col[1]) * 9.0f / 5.0f + 32.0f);
                                dew[j / 2 - 1] = (Float.parseFloat(col[2]) * 9.0f / 5.0f + 32.0f);
                                press[j / 2 - 1] = Float.parseFloat(col[3]) / 33.8639f;
                                windSpeed[j / 2 - 1] = Float.parseFloat(col[6])/1.60934f;
                                windGust[j / 2 - 1] = Float.parseFloat(col[7])/ 1.60934f;
                                precip[j / 2 - 1] = Float.parseFloat(col[9])/ 25.4f;
                            } else {
                                temp[j / 2 - 1] = Float.parseFloat(col[1]);
                                dew[j / 2 - 1] = Float.parseFloat(col[2]);
                                press[j / 2 - 1] = Float.parseFloat(col[3]);
                                precip[j / 2 - 1] = Float.parseFloat(col[9]);
                                if(nordic==0) {
                                    windSpeed[j / 2 - 1] = Float.parseFloat(col[6]);
                                    windGust[j / 2 - 1] = Float.parseFloat(col[7]);
                                }
                                else {
                                    windSpeed[j / 2 - 1] = Float.parseFloat(col[6])*0.277778f;
                                    windGust[j / 2 - 1] = Float.parseFloat(col[7])*0.277778f;
                                }
                            }
                        }
                        hum[j /2-1] = Float.parseFloat(col[8]);
                    }
                    j++;
                }
                if (units == 0) {
                    outBuild.append("PWS:\n");
                    outBuild.append(String.valueOf(Math.round(temp[temp.length -1] * 100.0) / 100.0));
                    outBuild.append(" °F  ");
                    outBuild.append(String.valueOf(Math.round(dew[dew.length - 1] * 100.0) / 100.0));
                    outBuild.append(" °F\n");
                    outBuild.append(String.valueOf(Math.round(press[press.length - 1] * 100.0) / 100.0));
                    outBuild.append(" inHg\n");
                    outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 1] * 100.0) / 100.0));
                    outBuild.append(" mph   ");
                    outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 1] * 100.0) / 100.0));
                    outBuild.append(" mph\n");
                    outBuild.append(String.valueOf(Math.round(precip[precip.length - 1] * 100.0) / 100.0));
                    outBuild.append(" in.   ");
                    outBuild.append(String.valueOf(Math.round(hum[hum.length - 1] * 100.0) / 100.0));
                    outBuild.append(" %");

                } else {
                    if(nordic==0) {
                        outBuild.append(String.valueOf(Math.round(temp[temp.length - 1] * 100.0) / 100.0));
                        outBuild.append(" °C\t");
                        outBuild.append(String.valueOf(Math.round(dew[dew.length - 1] * 100.0) / 100.0));
                        outBuild.append(" °C dew");
                        outBuild.append(String.valueOf(Math.round(press[press.length - 1] * 100.0) / 100.0));
                        outBuild.append(" hPa\n");
                        outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 1] * 100.0) / 100.0));
                        outBuild.append(" km/h\t");
                        outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 1] * 100.0) / 100.0));
                        outBuild.append(" km/h gust\n");
                        outBuild.append(String.valueOf(Math.round(precip[precip.length - 1] * 100.0) / 100.0));
                        outBuild.append(" mm\t");
                        outBuild.append(String.valueOf(Math.round(hum[hum.length - 1] * 100.0) / 100.0));
                        outBuild.append(" %");
                    }
                    else{
                        outBuild.append("PWS:\n");
                        outBuild.append(String.valueOf(Math.round(temp[temp.length - 1] * 100.0) / 100.0));
                        outBuild.append(" °C\n");
                        outBuild.append(String.valueOf(Math.round(dew[dew.length - 1] * 100.0) / 100.0));
                        outBuild.append(" °C dew\n");
                        outBuild.append(String.valueOf(Math.round(press[press.length - 1] * 100.0) / 100.0));
                        outBuild.append(" hPa\n");
                        outBuild.append(String.valueOf(Math.round(windSpeed[windSpeed.length - 1] * 100.0) / 100.0));
                        outBuild.append(" m/s\n");
                        outBuild.append(String.valueOf(Math.round(windGust[windGust.length - 1] * 100.0) / 100.0));
                        outBuild.append(" m/s gust\n");
                        outBuild.append(String.valueOf(Math.round(precip[precip.length - 1] * 100.0) / 100.0));
                        outBuild.append(" mm rain\n");
                        outBuild.append(String.valueOf(Math.round(hum[hum.length - 1] * 100.0) / 100.0));
                        outBuild.append(" % hum\n");
                    }
                }
                widgetText = outBuild.toString();
            } catch (IOException e) {
                widgetText = e.toString();
                System.out.println(e);
            } catch (NetworkOnMainThreadException b) {
                widgetText = b.toString();
                System.out.println(b);
            } catch (NumberFormatException n) {
                widgetText = n.toString();
                System.out.println(n);
            } catch (ArrayIndexOutOfBoundsException a) {
                widgetText = a.toString();
                System.out.println(a);
            }
            widText=widgetText.toString();
            return widgetText.toString();
        }
        @Override
        protected void onPostExecute(String result){
            widText=result;
            RemoteViews remoteViews = new RemoteViews(con.getPackageName(), R.layout.simple_widget);
            ComponentName thisWidget = new ComponentName( con, SimpleWidget.class );
            remoteViews.setTextViewText(R.id.appwidget_text, widText);

        }
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {
        con=context;
        new datagrab().execute("");
        RemoteViews views = new RemoteViews(con.getPackageName(), R.layout.simple_widget);
            views.setTextViewText(R.id.appwidget_text, widText);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    @Override
    public void onEnabled(Context context) {
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.startAlarm();
    }

    @Override
    public void onDisabled(Context context) {
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.stopAlarm();
    }
    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        if(intent.getAction().equals(ACTION_AUTO_UPDATE))
        {
            con=context;
            new datagrab().execute("");
        }
    }
}

