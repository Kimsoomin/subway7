package kr.mintech.subway7.controllers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import kr.mintech.subway7.R;
import kr.mintech.subway7.beans.StationBean;


public class SubwayListAdapter extends BaseAdapter
{
  private ArrayList<StationBean> items = new ArrayList<>();
  private Context context;
  
  
  public SubwayListAdapter(Context context)
  {
    this.context = context;
  }
  
  
  public void addAll(ArrayList<StationBean> beans)
  {
    items.clear();
    items.addAll(beans);
    notifyDataSetChanged();
  }
  
  
  public void clear()
  {
    items.clear();
    notifyDataSetChanged();
  }
  
  
  @Override
  public int getCount()
  {
    return items.size();
  }
  
  
  @Override
  public Object getItem(int position)
  {
    return items.get(position);
  }
  
  
  @Override
  public long getItemId(int position)
  {
    return position;
  }
  
  
  @SuppressLint("ViewHolder")
  @Override
  public View getView(int position, View convertView, ViewGroup parent)
  {
    StationBean bean = (StationBean) items.get(position);
    int resId = R.layout.list_item_station;
    convertView = LayoutInflater.from(parent.getContext()).inflate(resId, null);
    
    TextView startText = (TextView) convertView.findViewById(R.id.text_start);
    TextView finishText = (TextView) convertView.findViewById(R.id.text_finish);
    
    ImageView line = (ImageView) convertView.findViewById(R.id.station_image);
    TextView stationName = (TextView) convertView.findViewById(R.id.text_station_name);
    
    ImageView transferArrow = (ImageView) convertView.findViewById(R.id.image_right_arrow);
    ImageView transferStation = (ImageView) convertView.findViewById(R.id.transfer_station);
    LinearLayout container = (LinearLayout) convertView.findViewById(R.id.container);
    
    if (bean.line.contains("환승"))
    {
      try
      {
        line.setImageResource(SubwayManager.getInstance(context).getSubwayLineResourceId(bean.beforeLine));
        transferStation.setImageResource(SubwayManager.getInstance(context).getSubwayLineResourceId(bean.afterLine));
        transferStation.setVisibility(View.VISIBLE);
        transferArrow.setVisibility(View.VISIBLE);
        container.setBackgroundColor(Color.parseColor("#dce1e1"));
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    else
    {
      container.setBackgroundColor(Color.parseColor("#f4f4f4"));
      line.setImageResource(SubwayManager.getInstance(context).getSubwayLineResourceId(bean.line));
      transferStation.setVisibility(View.GONE);
      transferArrow.setVisibility(View.GONE);
    }
    
    if (position == 0)
    {
      startText.setVisibility(View.VISIBLE);
      finishText.setVisibility(View.GONE);
    }
    else if (position == items.size() - 1)
    {
      startText.setVisibility(View.GONE);
      finishText.setVisibility(View.VISIBLE);
    }
    else
    {
      startText.setVisibility(View.GONE);
      finishText.setVisibility(View.GONE);
    }
    
    if (Locale.getDefault().getLanguage().contains("ko"))
      stationName.setText(bean.nameKo);
    else
      stationName.setText(bean.nameCn);
    return convertView;
  }
}
