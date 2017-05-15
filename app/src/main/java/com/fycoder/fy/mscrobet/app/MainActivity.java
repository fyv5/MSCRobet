package com.fycoder.fy.mscrobet.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fycoder.fy.mscrobet.R;
import com.fycoder.fy.mscrobet.Utils.JsonParser;
import com.fycoder.fy.mscrobet.Utils.TextUnderStandUtils;
import com.fycoder.fy.mscrobet.adapter.MsgListAdapter;
import com.fycoder.fy.mscrobet.listener.ApiListener;
import com.fycoder.fy.mscrobet.model.Msg;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvaluator;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ContactManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String action = "fy.broadcast.action";
    private static final String PREFER_NAME = "fy.share.dialog";
    private SharedPreferences mSharedPreferences;
    private EditText mMsgInput;
    private Button mMsgSend;
    private Button mVoice;
    private Button mPostUsers;
    private ListView mChatMsgListView;
    private List<Msg> mDatas = new ArrayList<>();
    private MsgListAdapter mAdapter;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = (String) intent.getExtras().get("message");
            boolean isComing = (boolean) intent.getExtras().get("isComing");
            Log.d(TAG, "接受到广播 " + s);
            mAdapter.addItem(new Msg(s, isComing));
        }
    };

    private Toast mToast;
    //语音识别
    private SpeechRecognizer mIat;
    //语音识别UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    //语义理解
    private TextUnderStandUtils mTextUnderStandUtils;
    //语音合成
    private SpeechSynthesizer mTts;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chatting_lyt);
        IntentFilter filter = new IntentFilter(action);
        registerReceiver(broadcastReceiver, filter);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mTextUnderStandUtils = new TextUnderStandUtils(this);
        mIat = SpeechRecognizer.createRecognizer(this, null);
        mTts = SpeechSynthesizer.createSynthesizer(this, null);
        mIatDialog = new RecognizerDialog(this, null);
        mSharedPreferences = getSharedPreferences(PREFER_NAME,
                Activity.MODE_PRIVATE);
        init();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        // 退出时释放连接
        mIat.cancel();
        mIat.destroy();
        mTts.stopSpeaking();
        // 退出时释放连接
        mTts.destroy();
    }

    /**
     * 初始化控件和点击事件
     */
    private void init() {
        mMsgInput = (EditText) findViewById(R.id.id_chat_msg);
        mMsgSend = (Button) findViewById(R.id.id_chat_send);
        mVoice = (Button) findViewById(R.id.yuyin);
        mPostUsers = (Button) findViewById(R.id.postUsers);
        mChatMsgListView = (ListView) findViewById(R.id.id_chat_listView);
        mAdapter = new MsgListAdapter(this, mDatas);
        mChatMsgListView.setAdapter(mAdapter);
        TextView header = new TextView(this);
        header.setHeight(20);
        mChatMsgListView.addHeaderView(header);
        mChatMsgListView.setSelection(mDatas.size());

        mPostUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postUsers(MainActivity.this);
            }
        });
        mMsgSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();//获取编辑器
                editor.putBoolean("isSpeak", false).commit();//提交修改
                String s = mMsgInput.getText().toString();
                Msg msg = new Msg(s, false);
                mAdapter.addItem(msg);
                mMsgInput.setText(null);
                mChatMsgListView.setSelection(mDatas.size());
                mTextUnderStandUtils.request(s, mApiListener);
            }
        });
        mVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();//获取编辑器
                editor.putBoolean("isSpeak", true).commit();//提交修改
                setIatParam();
                boolean isShowDialog = mSharedPreferences.getBoolean("iat_show", true);
                if (isShowDialog) {
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();
                    showTip("请开始说话");
                } else {
                    int ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        showTip("听写失败,错误码：" + ret);
                    } else {
                        showTip("请开始说话");
                    }
                }
            }
        });
    }

    private void postUsers(Context context) {

        //获取 ContactManager 实例化对象
        ContactManager mgr = ContactManager.createManager(context, mContactListener);
        //异步查询联系人接口，通过 onContactQueryFinish 接口回调
        mgr.asyncQueryAllContactsName();
        //获取联系人监听器。

    }
    private ContactManager.ContactListener mContactListener = new ContactManager.ContactListener() {
        int ret;
        @Override
        public void onContactQueryFinish(String contactInfos, boolean changeFlag) {
            //指定引擎类型
            mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            mIat.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
            ret = mIat.updateLexicon("contact", contactInfos, lexiconListener);
            if (ret != ErrorCode.SUCCESS) {
                Log.d(TAG, "上传联系人失败：" + ret);
                showTip("上传联系人失败");
            }
        }
    };
    //上传联系人监听器。
    private LexiconListener lexiconListener = new LexiconListener() {
        @Override
        public void onLexiconUpdated(String lexiconId, SpeechError error) {
            if (error != null) {
                Log.d(TAG, error.toString());
            } else {
                Log.d(TAG, "上传成功！");
                showTip("上传联系人成功");
            }
        }
    };

    /**
     * 设置语音识别参数
     */

    private void setIatParam() {
        mIat.setParameter(SpeechConstant.PARAMS, null);
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    /**
     * 设置语音合成参数
     */
    public void setTtsParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyu");
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
    }

    //听写UI监听器
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            dealResult(recognizerResult);
        }

        @Override
        public void onError(SpeechError speechError) {
            showTip(speechError.getPlainDescription(true));
        }
    };

    //语音识别监听器
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            showTip("当前正在说话，音量大小：" + i);
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }

        @Override
        public void onEndOfSpeech() {
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            Log.d(TAG, recognizerResult.getResultString());
            dealResult(recognizerResult);
        }

        @Override
        public void onError(SpeechError speechError) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            showTip(speechError.getPlainDescription(true));
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private boolean isCall(String resutlString) {
//        try {
//            JSONObject object = new JSONObject(resutlString);
//            if(object.getInt("sn") == 1){
//                JSONArray ws = object.getJSONArray("ws");
//                if (ws != null) {
//                    JSONObject cws = ws.getJSONObject(0);
//                    JSONObject cw = cws.getJSONObject("cw");
//                    if (cw != null){
//                        if ("命令".equals(cw.getString("w")))
//                            return true;
//                    }
//                }
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        if (resutlString.startsWith("命令")) {
            return true;
        }
        return false;
    }

    //图灵机器人语义理解监听器回调接口
    private ApiListener mApiListener = new ApiListener() {
        @Override
        public void onSuccess(String response) {
            Msg msg = new Msg(response, true);
            mAdapter.addItem(msg);
            mChatMsgListView.setSelection(mDatas.size());
            Log.d(TAG, "图灵机器人" + msg.getMessage());
            if (mSharedPreferences.getBoolean("isSpeak", false)) {
                setTtsParam();
                mTts.startSpeaking(response, mSynthesizerListener);
            }
        }
        @Override
        public void onFail() {
            showTip("发送失败");
        }
    };

    //科大讯飞语义理解的监听器回调接口
    private TextUnderstanderListener mTextUnderstanderListener = new TextUnderstanderListener() {
        @Override
        public void onResult(UnderstanderResult understanderResult) {
            if (null != understanderResult) {
                // 显示
                String text = understanderResult.getResultString();
                if (!TextUtils.isEmpty(text)) {
                    Log.d(TAG, "onResult2: " + text);
                    String name = JsonParser.JsRusult(text);
                    Log.d(TAG, "name" + name);
                    callPeople(name);
                }
            } else {
                Log.d(TAG, "understander result:null");
                showTip("识别结果不正确。");
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            // 文本语义不能使用回调错误码14002，请确认您下载sdk时是否勾选语义场景和私有语义的发布
            showTip("onError Code：" + speechError.getErrorCode());
        }
    };

    //科大讯飞语音合成
    private SynthesizerListener mSynthesizerListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            Log.d(TAG, "onSpeakBegin: 开始合成");
        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakPaused() {

        }

        @Override
        public void onSpeakResumed() {

        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            Log.d(TAG, "onCompleted: 结束合成");
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    //工具方法显示Toast
    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void dealResult(RecognizerResult recognizerResult) {
        String text = JsonParser.parseIatResult(recognizerResult.getResultString());
        boolean ls = false;
        String sn = null;
        try {
            JSONObject resultJson = new JSONObject(recognizerResult.getResultString());
            sn = resultJson.optString("sn");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        String result = resultBuffer.toString();
        Log.d(TAG, "onResult1: " + result);
        try {
            JSONObject object = new JSONObject(recognizerResult.getResultString());
            ls = object.getBoolean("ls");
            Log.d(TAG, "ls " + ls);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //判断是否语音识别结束
        if (ls) {
            //判断是否是语音拨打电话
            //如果是“命令给XX打电话”就使用科大讯飞语音拨号，否则使用图灵机器人
            if (isCall(result)) {
                Msg msg = new Msg(result, false);
                mAdapter.addItem(msg);
                mChatMsgListView.setSelection(mDatas.size());
                mTextUnderStandUtils.request(result, mTextUnderstanderListener);
            } else {
                Msg msg = new Msg(result, false);
                mAdapter.addItem(msg);
                mChatMsgListView.setSelection(mDatas.size());
                mTextUnderStandUtils.request(result, mApiListener);
            }
        }
    }

    /**
     * 根据名字打电话，设计通讯录查找和拨号
     *
     * @param name
     */
    public void callPeople(String name) {
        //获取电话号码
        Log.d(TAG, "name=" + name);
        String contactNumber = "";
        ContentResolver cr = getContentResolver();
        Cursor pCur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,

                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + "=?",
                new String[]{name}, null);
        if (pCur.moveToFirst()) {
            contactNumber = pCur
                    .getString(pCur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            pCur.close();
        }
        Log.d(TAG, "电话号码 " + contactNumber);
        //根据电话号码打电话
        if (contactNumber == null || contactNumber.equals("")) {
            showTip("没有此联系人");
        } else {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contactNumber));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
