package net.mpross.pws;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.widget.RemoteViews;

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
    Context con;
    String widText="Loading";

    public class datagrab extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] p1) {

            byte[] by = new byte[11];
            byte[] byU = new byte[1];
            CharSequence widgetText = "";
            //Reads station id from station file
            try {
                FileInputStream fis = con.openFileInput("station_file");
                int n = fis.read(by);
                fis.close();
                station = new String(by, "UTF-8");
            } catch (IOException e) {
                System.out.println(e);
            }

            try {
                FileInputStream fis = con.openFileInput("unit_file");
                fis.read(byU);
                fis.close();
                units = (int) byU[0];
            } catch (IOException e) {
                System.out.println(e);
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
                            } else {
                                temp[j / 2 - 1] = (Float.parseFloat(col[1]) - 32.0f) * 5.0f / 9.0f;
                                dew[j / 2 - 1] = (Float.parseFloat(col[2]) - 32.0f) * 5.0f / 9.0f;
                                press[j / 2 - 1] = Float.parseFloat(col[3]) * 33.8639f;
                            }
                        }
                        //If data is in metric
                        else {
                            if (units == 0) {
                                temp[j / 2 - 1] = (Float.parseFloat(col[1]) * 9.0f / 5.0f + 32.0f);
                                dew[j / 2 - 1] = (Float.parseFloat(col[2]) * 9.0f / 5.0f + 32.0f);
                                press[j / 2 - 1] = Float.parseFloat(col[3]) / 33.8639f;
                            } else {
                                temp[j / 2 - 1] = Float.parseFloat(col[1]);
                                dew[j / 2 - 1] = Float.parseFloat(col[2]);
                                press[j / 2 - 1] = Float.parseFloat(col[3]);
                            }
                        }
                    }
                    j++;
                }
                if (units == 0) {
                    outBuild.append(String.valueOf(Math.round(temp[temp.length - 2] * 100.0) / 100.0));
                    outBuild.append(" °F\r\n");
                    outBuild.append(String.valueOf(Math.round(dew[dew.length - 2] * 100.0) / 100.0));
                    outBuild.append(" °F\r\n");
                    outBuild.append(String.valueOf(Math.round(press[press.length - 2] * 100.0) / 100.0));
                    outBuild.append(" inHg\r\n");
                } else {
                    outBuild.append(String.valueOf(Math.round(temp[temp.length - 2] * 100.0) / 100.0));
                    outBuild.append(" °C\r\n");
                    outBuild.append(String.valueOf(Math.round(dew[dew.length - 2] * 100.0) / 100.0));
                    outBuild.append(" °C\r\n");
                    outBuild.append(String.valueOf(Math.round(press[press.length - 2] * 100.0) / 100.0));
                    outBuild.append(" hPa\r\n");
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
            return widgetText.toString();
        }
        @Override
        protected void onPostExecute(String result){
            widText=result;
        }
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {
        con=context;
        new datagrab().execute("");
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(con.getPackageName(), R.layout.simple_widget);
        views.setTextViewText(R.id.appwidget_text, widText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

