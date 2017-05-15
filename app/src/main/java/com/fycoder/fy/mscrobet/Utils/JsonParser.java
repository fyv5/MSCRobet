package com.fycoder.fy.mscrobet.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * json解析工具类
 * Created by fy on 2016/11/22.
 */
public class JsonParser {

    /**
     * 解析语音听写json
     * @param json 语音听写的json串
     * @return 返回听写结果字符串
     */
    public static String parseIatResult (String json){
        StringBuilder ret = new StringBuilder();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret.toString();
    }

    /**
     * 获得科大讯飞json中的name信息，用于自动拨号
     * @param jsonString
     * @return
     */
    public static String JsRusult(String jsonString){
        JSONObject jsonObject;
        String peopleName;
        StringBuffer ret = new StringBuffer();
        try{
            jsonObject=new JSONObject(jsonString);
            String strQuestion = jsonObject.getString("text");
            String strService = jsonObject.getString("service");
            if ("telephone".equals(strService)){
                peopleName = jsonObject.getJSONObject("semantic").getJSONObject("slots").getString("name");
                ret.append(jsonObject.getJSONObject("semantic").getJSONObject("slots").getString("name"));
                String operationStr = jsonObject.getString("operation");
                String phoneCode = "";
                phoneCode = jsonObject.getJSONObject("semantic").getJSONObject("slots").getString("code");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ret.toString();
    }
}
