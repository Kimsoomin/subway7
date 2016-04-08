package kr.mintech.subway7;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import kr.mintech.subway7.beans.StationBean;
import kr.mintech.subway7.beans.StationBean.StationExitBean;
import kr.mintech.subway7.controllers.SubwayListAdapter;
import kr.mintech.subway7.controllers.SubwayManager;


public class SubwayStationsActivity extends ActionBarActivity
{
  private ListView listView;
  private SubwayListAdapter adapter;
  private TextView infoText;
  private ImageView imageX;
  
  private TextView startStationName, endStationName;
  private ImageView startStationImage, endStationImage;
  
  private double nearByLat, nearByLon;
  
  private String destName;
  private View exitsInfoView;
  
  
  @SuppressLint({ "SetJavaScriptEnabled", "InflateParams" })
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_subway_stations);
    @SuppressLint("InflateParams")
    View customActionBar = LayoutInflater.from(this).inflate(R.layout.custom_action_bar, null);
    TextView title = (TextView) customActionBar.findViewById(R.id.title);
    title.setText(getString(R.string.term_subway));
    getSupportActionBar().setCustomView(customActionBar);
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().hide();
    
    String infoString = getIntent().getStringExtra("stations_info");
    String jsonStations = getIntent().getStringExtra("stations_json");
    
    if (getIntent().hasExtra("dest_name"))
      destName = getIntent().getStringExtra("dest_name");
    
    nearByLat = getIntent().getDoubleExtra("near_by_lat", -1);
    if (nearByLat != -1)
      nearByLon = getIntent().getDoubleExtra("near_by_lon", -1);
    
    startStationName = (TextView) findViewById(R.id.start_station_name);
    endStationName = (TextView) findViewById(R.id.end_station_name);
    startStationImage = (ImageView) findViewById(R.id.start_station_image);
    endStationImage = (ImageView) findViewById(R.id.end_station_image);
    
    imageX = (ImageView) findViewById(R.id.image_x);
    imageX.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View arg0)
      {
        finish();
      }
    });
    
    infoText = (TextView) findViewById(R.id.text_stations_info);
    infoText.setText(infoString);
    
    listView = (ListView) findViewById(R.id.list_view);
    
    adapter = new SubwayListAdapter(SubwayStationsActivity.this);
    listView.setAdapter(adapter);
    parseStationsJson(jsonStations);
  }
  
  
  private void parseStationsJson(String stationsJSONString)
  {
    JSONArray stationsJsonArray;
    ArrayList<StationBean> stations = new ArrayList<>();
    try
    {
      
      stationsJsonArray = new JSONArray(stationsJSONString);
      for (int i = 0; i < stationsJsonArray.length(); i++)
      {
        StationBean bean = new StationBean();
        bean.setJSONObject(stationsJsonArray.getJSONObject(i));
        stations.add(bean);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    try
    {
      startStationImage.setImageResource(SubwayManager.getInstance(SubwayStationsActivity.this).getSubwayLineResourceId(stations.get(0).line));
      endStationImage.setImageResource(SubwayManager.getInstance(SubwayStationsActivity.this).getSubwayLineResourceId(stations.get(stations.size() - 1).line));
      if (Locale.getDefault().getLanguage().contains("ko"))
      {
        startStationName.setText(stations.get(0).nameKo);
        endStationName.setText(stations.get(stations.size() - 1).nameKo);
      }
      else
      {
        startStationName.setText(stations.get(0).nameCn);
        endStationName.setText(stations.get(stations.size() - 1).nameCn);
      }
      
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    if (nearByLat != -1 && !TextUtils.isEmpty(destName))
    {
      try
      {
        exitsInfoView = LayoutInflater.from(SubwayStationsActivity.this).inflate(R.layout.view_subway_exit_info, null);
        StationBean finalStationBean = stations.get(stations.size() - 1);
        finalStationBean = SubwayManager.getInstance(SubwayStationsActivity.this).findStation(finalStationBean.stationId);
        
        int nearByExit = 1;
        
        Log.w("WARN", "마지막 역 :" + finalStationBean.nameKo);
        Log.w("WARN", "마지막 역의 출구 사이즈 :" + finalStationBean.exits);
        
        ArrayList<StationExitBean> exits = new ArrayList<>();
        for (int i = 0; i < finalStationBean.exits.size(); i++)
        {
          float distance = distFrom(nearByLat, nearByLon, finalStationBean.exits.get(i).lat, finalStationBean.exits.get(i).lon);
          Log.w("WARN", "Distance:" + distance);
          finalStationBean.exits.get(i).distance = distance;
          exits.add(finalStationBean.exits.get(i));
        }
        
        Collections.sort(exits, myComparator);
        
        try
        {
          nearByExit = exits.get(0).exitTitle;
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        
        String nearString = destName + "과 가장 가까운 출구는 " + finalStationBean.nameKo + " <b>" + nearByExit + "</b>번 출구입니다.";
        if (!Locale.getDefault().getLanguage().contains("ko"))
          nearString = "离" + destName + "最近的出口是" + finalStationBean.nameCn + "站<b>" + nearByExit + "</b>号出口";
        TextView exitInfoText = (TextView) exitsInfoView.findViewById(R.id.text_exit_info);
        exitInfoText.setText(Html.fromHtml(nearString));
        listView.addFooterView(exitsInfoView);
      }
      catch (Exception e)
      {
      }
    }
    adapter.addAll(stations);
  }
  
  private final static Comparator<StationExitBean> myComparator = new Comparator<StationExitBean>()
  {
    public int compare(StationExitBean object1, StationExitBean object2)
    {
      return (int) (object1.distance - object2.distance);
    }
  };
  
  
  public static float distFrom(double lat1, double lng1, double lat2, double lng2)
  {
    double earthRadius = 6371000;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    float dist = (float) (earthRadius * c);
    return dist;
  }
  
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    int id = item.getItemId();
    if (id == android.R.id.home)
      finish();
    return super.onOptionsItemSelected(item);
  }
}
