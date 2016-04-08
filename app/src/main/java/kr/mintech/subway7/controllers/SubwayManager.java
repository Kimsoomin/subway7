package kr.mintech.subway7.controllers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import kr.mintech.subway7.beans.StationBean;

public class SubwayManager
{
  private static SubwayManager instance = null;
  private Context context;
  public ArrayList<StationBean> stations = new ArrayList<StationBean>();
  
  
  public static SubwayManager getInstance(Context context)
  {
    if (instance == null)
    {
      synchronized (SubwayManager.class)
      {
        if (instance == null)
          instance = new SubwayManager(context);
      }
    }
    return instance;
  }
  
  
  private SubwayManager(Context context)
  {
    this.context = context;
  }
  
  
  public void loadAllStations(Context context)
  {
    try
    {
      InputStream is = context.getAssets().open("subway_data.js");
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      String jsString = new String(buffer, "UTF-8");
      
      String[] jsStrings = jsString.split("\n");
      
      String stationsString = jsStrings[1];
      stationsString = stationsString.replaceFirst("station_ids = ", "");
      JSONArray stations = new JSONArray(stationsString);
      
      String stationsInfoString = jsStrings[0];
      stationsInfoString = stationsInfoString.replaceFirst("stations = ", "");
      
      JSONObject stationInfoJSONObject = new JSONObject(stationsInfoString);
      
      ArrayList<StationBean> tempArray = new ArrayList<StationBean>();
      for (int i = 0; i < stations.length(); i++)
      {
        try
        {
          JSONObject obj = stationInfoJSONObject.getJSONObject(stations.getString(i));
          StationBean bean = new StationBean();
          bean.setJSONObject(obj);
          tempArray.add(bean);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
      Log.w("WARN", "Stations size : " + tempArray.size());
      this.stations.clear();
      this.stations.addAll(tempArray);
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  
  public double getLatitudeWithSubwayId(String stationId)
  {
    StationBean bean = null;
    for (int i = 0; i < stations.size(); i++)
    {
      if (stations.get(i).stationId.equals(stationId))
        bean = stations.get(i);
    }
    
    if (bean == null)
      return -1;
    else
      return bean.lat;
  }
  
  
  public double getLongitudeWithSubwayId(String stationId)
  {
    StationBean bean = null;
    for (int i = 0; i < stations.size(); i++)
    {
      if (stations.get(i).stationId.equals(stationId))
        bean = stations.get(i);
    }
    
    if (bean == null)
      return -1;
    else
      return bean.lon;
  }
  
  public StationBean findStation(String stationId)
  {
    StationBean bean = null;
    for (int i = 0; i < stations.size(); i++)
    {
      if (stations.get(i).stationId.equals(stationId))
        bean = stations.get(i);
    }
    
    return bean;
  }  
  
  public StationBean findStationWithTransfer(String stationId)
  {
    StationBean bean = null;
    for (int i = 0; i < stations.size(); i++)
    {
      if (stations.get(i).stationId.equals(stationId))
        bean = stations.get(i);
    }
    
    for (int i = 0; i < stations.size(); i++)
    {
      if (stations.get(i).nameKo.equals(bean.nameKo))
        if (stations.get(i).line.contains("환승"))
          bean = stations.get(i);
    }
    
    return bean;
  }
  
  
  //검색 시 사용 
  public ArrayList<StationBean> getSubwayStationsWithTitle(final String title)
  {
    ArrayList<StationBean> tempStations = new ArrayList<StationBean>();
    //이름이 포함된 역을 모두 가져옴
    for (int i = 0; i < stations.size(); i++)
    {
      if ((!TextUtils.isEmpty(stations.get(i).nameKo) && stations.get(i).nameKo.contains(title)) || (!TextUtils.isEmpty(stations.get(i).nameCn) && stations.get(i).nameCn.contains(title)))
      {
        if (stations.get(i).isDuplicate)
          continue;
        
        boolean isConatin = false;
        for (int j = 0; j < tempStations.size(); j++)
        {
          if (tempStations.get(j).nameKo.equals(stations.get(i).nameKo))
            isConatin = true;
        }
        
        if (!isConatin)
          tempStations.add(stations.get(i));
        else
        {
          if (stations.get(i).line.contains("환승"))
          {
            for (int j = 0; j < tempStations.size(); j++)
            {
              if (stations.get(i).nameKo.equals(tempStations.get(j).nameKo))
                tempStations.remove(j);
            }
            tempStations.add(stations.get(i));
          }
        }
      }
    }
    
    return tempStations;
  }
  
  
  public ArrayList<StationBean> getAllSubwayStations()
  {
    return stations;
  }
  
  
  public String getStationId(String stationName)
  {
    ArrayList<StationBean> sameNameStations = new ArrayList<StationBean>();
    String stationId = "";
    for (int i = 0; i < stations.size(); i++)
    {
      if (stations.get(i).nameKo.equals(stationName))
        sameNameStations.add(stations.get(i));
    }
    
    if (sameNameStations.size() == 1)
      return sameNameStations.get(1).stationId;
    else
    {
      for (int i = 0; i < sameNameStations.size(); i++)
      {
        if (TextUtils.isEmpty(sameNameStations.get(i).stationId))
          stationId = sameNameStations.get(i).stationId;
        
        if (stationId.length() < sameNameStations.get(i).stationId.length())
          stationId = sameNameStations.get(i).stationId;
      }
    }
    return stationId;
  }
  
  
  public int getSubwayLineResourceId(String line)
  {
    Log.w("WARN", "line: " + line);
    int resourceId = context.getResources().getIdentifier("drawable/icon_subway_line1", null, context.getPackageName());
    try
    {
      if (line.equals("2"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line2", null, context.getPackageName());
      else if (line.equals("3"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line3", null, context.getPackageName());
      else if (line.equals("4"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line4", null, context.getPackageName());
      else if (line.equals("5"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line5", null, context.getPackageName());
      else if (line.equals("6"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line6", null, context.getPackageName());
      else if (line.equals("7"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line7", null, context.getPackageName());
      else if (line.equals("8"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line8", null, context.getPackageName());
      else if (line.equals("9"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_line9", null, context.getPackageName());
      else if (line.equals("공항철도"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_linea", null, context.getPackageName());
      else if (line.equals("분당선"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_lineb", null, context.getPackageName());
      else if (line.equals("에버라인"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_lineever", null, context.getPackageName());
      else if (line.equals("경춘선"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_linegc", null, context.getPackageName());
      else if (line.equals("경의중앙선"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_linegj", null, context.getPackageName());
      else if (line.equals("인천1호선"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_lineinc1", null, context.getPackageName());
      else if (line.equals("신분당선"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_linesb", null, context.getPackageName());
      else if (line.equals("수인선"))
        resourceId = context.getResources().getIdentifier("drawable/icon_subway_linesuin", null, context.getPackageName());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return resourceId;
  }
}
