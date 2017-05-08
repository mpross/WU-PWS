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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class Widget extends AppWidgetProvider {
    String station = ""; //Weather station name
    int units = 0; // User unit choice
    int nativeUnits = 0; // Units the data is in
    private RemoteViews views = new RemoteViews(null, R.layout.widget);

    static void updateAppWidget(Context icontext, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(icontext.getPackageName(), R.layout.widget);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        views = new RemoteViews(context.getPackageName(), R.layout.widget);

        byte[] by = new byte[11];
        byte[] byU = new byte[1]; //Unit selection 0=imperial, 1=metric
        //Reads station id from station file
        try {
            FileInputStream fis = context.openFileInput("station_file");
            int n = fis.read(by);
            fis.close();
            station = new String(by, "UTF-8");
        } catch (IOException e) {
            views.setTextViewText(R.id.appwidget_text, e.toString());
        }
//        try {
//            FileInputStream fis = context.openFileInput("unit_file");
//            fis.read(byU);
//            fis.close();
//            units = (int) byU[0];
//        } catch (IOException e) {
//            views.setTextViewText(R.id.appwidget_text, e.toString());
//        }

        for (int appWidgetId : appWidgetIds) {
            new widgDataGrab().execute("");
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    //Data grab from WeatherUnderground
    public class widgDataGrab extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] p1) {
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
                float temp = 0;
                float dew = 0;
                float press = 0;
                float windDeg = 0;
                float windSpeed = 0;
                float windGust = 0;
                float hum = 0;
                float precip = 0;
                float precipDay = 0;
                float tim = 0;
                String windDir = new String();

                int m = 0;

                String timStamp = "";

                String line = lines[lines.length - 2];
                //Splits lines into columns
                String[] col = line.split(",");
                //Reads data units from first line
                if (lines[1].split(",")[1].equals("TemperatureF")) {
                    nativeUnits = 0;
                } else {
                    nativeUnits = 1;
                }
                timStamp = col[0];
                //Drop out handling
                if (Float.parseFloat(col[1]) > -50) {
                    //If data is in imperial
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            temp = Float.parseFloat(col[1]);
                        } else {
                            temp = (Float.parseFloat(col[1]) - 32.0f) * 5.0f / 9.0f;
                        }
                    }
                    //If data is in metric
                    else {
                        if (units == 0) {
                            temp = (Float.parseFloat(col[1]) * 9.0f / 5.0f + 32.0f);
                        } else {
                            temp = Float.parseFloat(col[1]);
                        }
                    }
                }
                if (Float.parseFloat(col[2]) > -50) {
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            dew = Float.parseFloat(col[2]);
                        } else {
                            dew = (Float.parseFloat(col[2]) - 32.0f) * 5.0f / 9.0f;
                        }
                    } else {
                        if (units == 0) {
                            dew = (Float.parseFloat(col[2]) * 9.0f / 5.0f + 32.0f);
                        } else {
                            dew = Float.parseFloat(col[2]);
                        }
                    }
                }
                if (Float.parseFloat(col[3]) > 0) {
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            press = Float.parseFloat(col[3]);
                        } else {
                            press = Float.parseFloat(col[3]) * 33.8639f;
                        }
                    } else {
                        if (units == 0) {
                            press = Float.parseFloat(col[3]) / 33.8639f;
                        } else {
                            press = Float.parseFloat(col[3]);
                        }
                    }
                }
                windDir = col[4];
                if (Float.parseFloat(col[5]) > 0) {
                    windDeg = Float.parseFloat(col[5]);
                }
                if (Float.parseFloat(col[6]) > 0) {
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            windSpeed = Float.parseFloat(col[6]);
                        } else {
                            windSpeed = Float.parseFloat(col[6]) * 1.60934f;
                        }
                    } else {
                        if (units == 0) {
                            windSpeed = Float.parseFloat(col[6]) / 1.60934f;
                        } else {
                            windSpeed = Float.parseFloat(col[6]);
                        }
                    }
                }
                if (Float.parseFloat(col[7]) > 0) {
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            windGust = Float.parseFloat(col[7]);
                        } else {
                            windGust = Float.parseFloat(col[7]) * 1.60934f;
                        }
                    } else {
                        if (units == 0) {
                            windGust = Float.parseFloat(col[7]) / 1.60934f;
                        } else {
                            windGust = Float.parseFloat(col[7]);
                        }
                    }
                }
                if (Float.parseFloat(col[8]) > 0) {
                    hum = Float.parseFloat(col[8]);
                }
                if (Float.parseFloat(col[9]) > 0) {
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            precip = Float.parseFloat(col[9]);
                        } else {
                            precip = Float.parseFloat(col[9]) * 25.4f;
                        }
                    } else {
                        if (units == 0) {
                            precip = Float.parseFloat(col[9]) / 25.4f;
                        } else {
                            precip = Float.parseFloat(col[9]);
                        }
                    }
                }
                if (Float.parseFloat(col[12]) > 0) {
                    if (nativeUnits == 0) {
                        if (units == 0) {
                            precipDay = Float.parseFloat(col[12]);
                        } else {
                            precipDay = Float.parseFloat(col[12]) * 25.4f;
                        }
                    } else {
                        if (units == 0) {
                            precipDay = Float.parseFloat(col[12]) / 25.4f;
                        } else {
                            precipDay = Float.parseFloat(col[12]);
                        }
                    }
                }

                //If native units are imperial
                if (nativeUnits == 0) {
                    //Last line of data file is already the correct format for displaying
                    if (units == 0) {
                        outBuild.append(line);
                    }
                    //If units are different from file then create correctly formatted string
                    else {
                        outBuild.append(timStamp);
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(temp * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(dew * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(press * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDir));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDeg));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windSpeed * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windGust * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(hum));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(precip * 100.0) / 100.0));
                        outBuild.append(",,,,,,");
                    }
                }
                //If native units are metric
                else {
                    if (units == 0) {
                        outBuild.append(timStamp);
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(temp * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(dew * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(press * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDir));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(windDeg));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windSpeed * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(windGust * 100.0) / 100.0));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(hum));
                        outBuild.append(",");
                        outBuild.append(String.valueOf(Math.round(precip * 100.0) / 100.0));
                        outBuild.append(",,,,,,");
                    } else {
                        outBuild.append(line);
                    }
                }
                return outBuild.toString();

            } catch (IOException e) {
                return "";
            } catch (NetworkOnMainThreadException b) {
                return "";
            } catch (NumberFormatException n) {
                return "";
            } catch (ArrayIndexOutOfBoundsException a) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //Label creation
            String fieldString = "Date & Time,Temperature,Dewpoint,Pressure,Wind: \n" +
                    "     Direction,     Direction,     Speed," +
                    "     Gust,Humidity,Hourly Precip,Conditions,Clouds,Daily Rain,SoftwareType,DateUTC";
            String fieldStringD = "Temperature: \n     Average,     High,     Low,Dewpoint: \n" +
                    "     Average,     High,     Low,Average Pressure,Average Wind Direction," +
                    "Average Wind Speed,Maximum Wind Gust,Average Humidity,Daily Rain";
            String endString = "";
            String endStringD = "";
            if (units == 0) {
                endString = ", °F, °F, inHg,, °, mph, mph, %, in,,, in,,";
                endStringD = " °F, °F, °F, °F, °F, °F, inHg, °, mph, mph, %, in";
            } else {
                endString = ", °C, °C, hPa,, °, km/h, km/h, %, mm,,, mm,,";
                endStringD = " °C, °C, °C, °C, °C, °C, hPa, °, km/h, km/h, %, mm";
            }
            //Excluded labels that are included in data file
            String exString = "Conditions,Clouds,SoftwareType,DateUTC,Daily Rain";
            String[] split = result.split(";");
            String[] dataCur = new String[1];
            String[] dataDay = new String[1];
            try {
                dataCur = split[0].split(",");
            } catch (ArrayIndexOutOfBoundsException a) {
                views.setTextViewText(R.id.appwidget_text, a.toString());
            }
            StringBuilder outCur = new StringBuilder();
            String[] fields = fieldString.split(",");
            String[] fieldsD = fieldStringD.split(",");
            String[] ends = endString.split(",");
            String[] endsD = endStringD.split(",");
            List exclude = Arrays.asList(exString.split(","));
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
                views.setTextViewText(R.id.appwidget_text, outCur.toString());
            } catch (ArrayIndexOutOfBoundsException a) {
                views.setTextViewText(R.id.appwidget_text, a.toString());
            }
        }
    }

}

