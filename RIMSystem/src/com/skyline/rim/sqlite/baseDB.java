package com.skyline.rim.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import static android.database.Cursor.FIELD_TYPE_FLOAT;
import static android.database.Cursor.FIELD_TYPE_INTEGER;
import static android.database.Cursor.FIELD_TYPE_STRING;
import static com.skyline.rim.globaldata.ConstData.OPENDB_READONLY;
import static com.skyline.rim.globaldata.ConstData.OPENDB_WRITEABLE;

/**
 * 本地数据库的操作基础类
 * Created by mft on 2017/11/1.
 */

public class baseDB {
    public SQLiteDatabase db = null;
    Context context = null;
    rimDBHelper dbHelper = null;

    public baseDB(Context context) {
        this.context = context;
        dbHelper = new rimDBHelper(context, "rim.db", null, 9);
    }

    /***
     *
     * @param model 打开数据库的模式
     */
    public void opendb(int model) {
        try {
            //得到一个可写的数据库  
            if (model == OPENDB_READONLY)
                db = dbHelper.getReadableDatabase();
            else if (model == OPENDB_WRITEABLE)
                db = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }

    /***
     * 此方法执行效率较低，建议不要在循环体里面使用
     * @param sql
     */
    public void execSQL(String sql) {
        opendb(OPENDB_WRITEABLE);
        db.execSQL(sql);
        closedb();
    }

    /***
     * 快速执行SQL语句，必须配合 closedb  和  opendb 一起使用
     * @param sql
     */
    public void execSQL1(String sql) {
        db.execSQL(sql);
    }

    /***
     * 查询数据库并返回JSON数据
     * 注意：查询条件中要携带项目id
     * @param sql 查询语句
     * @return json格式的查询结果
     */
    public String select(String sql) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            opendb(OPENDB_READONLY);
            stringBuilder.append("[");
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor == null) {
                closedb();
                return "";
            }

            while (cursor.moveToNext()) {
                stringBuilder.append("{");
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String fieldName = cursor.getColumnName(i);
                    String val = "";
                    int type = cursor.getType(i);
                    switch (type) {
                        case FIELD_TYPE_INTEGER:
                            val = cursor.getInt(i) + "";
                            break;
                        case FIELD_TYPE_STRING:
                            val = cursor.getString(i);
                            break;
                        case FIELD_TYPE_FLOAT:
                            val = cursor.getFloat(i) + "";
                            break;
                        default:
                            break;
                    }
                    stringBuilder.append("\"" + fieldName + "\":\"" + val + "\",");
                }
                //rVal = rVal.substring(0, rVal.length() - 1);
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.append("},");
            }

            //rVal = rVal.substring(0, rVal.length() - 1);
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("]");
            cursor.close();
            closedb();
        } catch (Exception e) {
            closedb();
            Log.i("tsdi", e.getMessage());
        }
        return stringBuilder.toString();
    }
    public void closedb() {
        if (db != null)
            db.close();
    }
}
