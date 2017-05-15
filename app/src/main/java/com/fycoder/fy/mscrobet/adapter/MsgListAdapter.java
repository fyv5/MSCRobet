package com.fycoder.fy.mscrobet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fycoder.fy.mscrobet.R;
import com.fycoder.fy.mscrobet.model.Msg;

import java.util.List;


/**
 * Created by fy on 2016/10/8.
 */
public class MsgListAdapter extends BaseAdapter{

    private LayoutInflater mLayoutInflater;
    private List<Msg> mDatas;

    public MsgListAdapter(Context context, List<Msg> mDatas){
        mLayoutInflater = LayoutInflater.from(context);
        this.mDatas = mDatas;
        Msg msg = new Msg("你好，我是BayMax，你可以给我发消息，也可以按住左下角的语音按钮和我聊天哦~",true);
        mDatas.add(msg);
    }
    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int i) {
        return mDatas.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemViewType(int position) {
        Msg msg = mDatas.get(position);
        return msg.getIsComing() ? 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Msg msg = mDatas.get(i);

        ViewHolder holder;
        if(view == null){
            holder = new ViewHolder();
            if(msg.getIsComing()) {
                view = mLayoutInflater.inflate(R.layout.chat_from_lyt, viewGroup, false);
                holder.message = (TextView) view.findViewById(R.id.message);
                view.setTag(holder);
            } else {
                view = mLayoutInflater.inflate(R.layout.chat_send_lyt, viewGroup, false);
                holder.message = (TextView) view.findViewById(R.id.message);
                view.setTag(holder);
            }
        } else {
            holder = (ViewHolder) view.getTag();
        }
        holder.message.setText(msg.getMessage());
        return view;
    }

    public void addItem(Msg msg){
        mDatas.add(msg);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public TextView message;
    }
}
