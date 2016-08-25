package kr.durian.duriancam.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.durian.duriancam.R;
import kr.durian.duriancam.data.SecureDetectedData;
import kr.durian.duriancam.util.Config;

/**
 * Created by tbzm on 15. 10. 19.
 */
public class SecureDetectedListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<SecureDetectedData> data = new ArrayList<SecureDetectedData>();

    public SecureDetectedListAdapter(Context context, ArrayList<SecureDetectedData> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        if (data == null) {
            return null;
        }
        return data.get(position);
    }

    @Override
    public int getCount() {
        if (data == null) {
            return 0;
        }
        return data.size();
    }

    private static class DetectedViewHolder {
        public ImageView mImageView;
        public TextView mFileName;
        public TextView mDate;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        DetectedViewHolder holder;
        if (v == null) {
            holder = new DetectedViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.detected_item_row, null);
            holder.mImageView = (ImageView) v.findViewById(R.id.icon_image);
            holder.mFileName = (TextView) v.findViewById(R.id.file_Name);
            holder.mDate = (TextView) v.findViewById(R.id.date);
            v.setTag(holder);
        } else {
            holder = (DetectedViewHolder) v.getTag();
        }
        SecureDetectedData positionData = data.get(position);
        holder.mFileName.setText(Config.getFileName(positionData.getImagePath()));
        holder.mDate.setText(getTimeString(positionData.getTime()));
        String path = positionData.getImagePath();
        Glide.with(context)
                .load(path)
                .into(holder.mImageView);
        return v;
    }

    public ArrayList<SecureDetectedData> getData() {
        return data;
    }




    public String getTimeString(long time) {
        Date now = new Date(time);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(now);
    }
}
