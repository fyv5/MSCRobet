package com.fycoder.fy.mscrobet.Utils;


import android.content.Context;
import android.widget.Toast;

import com.fycoder.fy.mscrobet.listener.ApiListener;
import com.fycoder.fy.mscrobet.model.Resp;
import com.google.gson.Gson;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import okhttp3.Call;

/**
 * Created by fy on 2016/10/6.
 */
public class TextUnderStandUtils {
    private static final String TAG = "TextUnderStandUtils";
    private Toast mToast;

    //语义理解对象
    private TextUnderstander mTextUnderstander;


    public TextUnderStandUtils(Context context) {
        mTextUnderstander = TextUnderstander.createTextUnderstander(context, null);
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }

    /**
     * 图灵机器人语义理解
     *
     * @param s
     * @param listener
     */
    public void request(String s, final ApiListener listener) {
        OkHttpUtils.post().url("http://www.tuling123.com/openapi/api").addParams("key", "da4e25aceb314f76839ea24684397a9e")
                .addParams("info", s).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e) {

            }

            @Override
            public void onResponse(String response) {
                Gson gson = new Gson();
                Resp res = gson.fromJson(response, Resp.class);
                listener.onSuccess(res.getText());
            }
        });

    }

    /**
     * 科大讯飞语义理解
     *
     * @param s 语音分析后传入服务器的String
     * @return 从服务器得到的String
     */
    public void request(String s, TextUnderstanderListener mTextUnderstanderListener) {
        int ret = 0;
        if (mTextUnderstander.isUnderstanding()) {
            mTextUnderstander.cancel();
        } else {
            ret = mTextUnderstander.understandText(s, mTextUnderstanderListener);
            if (ret != 0) {
                showTip("语义理解失败,错误码:" + ret);
            }
        }
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

}
