package kr.durian.duriancam.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.durian.duriancam.R;
import kr.durian.duriancam.data.SecureDetectedData;

/**
 * Created by sunyungkim on 16. 7. 31..
 */
public class SecureDetectedListDeleteAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<SecureDetectedData> data = new ArrayList<SecureDetectedData>();

    public SecureDetectedListDeleteAdapter(Context context, ArrayList<SecureDetectedData> data) {
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
        public ImageView mCheckImage;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        DetectedViewHolder holder;
        if (v == null) {
            holder = new DetectedViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.detected_delete_item_row, null);
            holder.mImageView = (ImageView) v.findViewById(R.id.icon_image);
            holder.mFileName = (TextView) v.findViewById(R.id.file_Name);
            holder.mDate = (TextView) v.findViewById(R.id.date);
            holder.mCheckImage = (ImageView)v.findViewById(R.id.del_check);
            v.setTag(holder);
        } else {
            holder = (DetectedViewHolder) v.getTag();
        }
        SecureDetectedData positionData = data.get(position);
        holder.mFileName.setText(getFileName(positionData.getImagePath()));
        holder.mDate.setText(getTimeString(positionData.getTime()));
        String path = positionData.getImagePath();
        if(positionData.getSelected() == 0){
            holder.mCheckImage.setImageResource(R.drawable.btn_contents_checkbox_normal);
        }else{
            holder.mCheckImage.setImageResource(R.drawable.btn_contents_checkbox_checked);
        }
        Glide.with(context)
                .load(path)
                .into(holder.mImageView);
        return v;
    }

    public ArrayList<SecureDetectedData> getData() {
        return data;
    }

    public String getFileName(String path) {
        int count = path.split(File.separator).length;
        String filename = "";
        if (count > 1) {
            filename = path.split(File.separator)[count - 1];
        }
        return filename;
    }


    public String getTimeString(long time) {
        Date now = new Date(time);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(now);
    }
}
