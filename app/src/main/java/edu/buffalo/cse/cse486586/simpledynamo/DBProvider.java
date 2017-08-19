





package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.content.Context;
import android.util.Log;

public class DBProvider extends SQLiteOpenHelper
{
    public static final String DatabaseName="SimpleDynamo";
    public static final String TableName="original";
    public static final String ColName1="key";
    public static final String ColName2="value";
    public static final String ColName3="msg_port";
    public static final String CreateTableQuery="create table "+TableName+"("+ColName1+" varchar(50) primary key, "+ColName2+" varchar(50) not null, "+ColName3+" varchar(50) not null);";
    SQLiteDatabase d;
    public DBProvider(Context ctx)throws SQLException
    {
        super(ctx,DatabaseName,null,1);
    }
    @Override
    public void onCreate(SQLiteDatabase db)throws SQLException
    {
        db.execSQL(CreateTableQuery);
        db.execSQL("create table flag(key varchar(10) primary key, value int)");
        Log.i("db provider","DB initialization done");
    }

    public void insertIntoDb(ContentValues cv,String x)throws SQLException
    {
        try
        {
            if(x.equals("flag"))
            {
                d = getWritableDatabase();
                long l=d.insertWithOnConflict("flag", null, cv,SQLiteDatabase.CONFLICT_REPLACE);
                if (l == -1)
                    Log.i("DbProvider", "error occured");
                else
                    Log.i("db provider","insert in DB done");
            }
            else
            {
                d = getWritableDatabase();
                long l = d.insertWithOnConflict(TableName, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
                if (l == -1)
                    Log.i("DbProvider", "error occured");
                else
                    Log.i("db provider","insert in DB done");
            }



        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public Cursor queryDb(String sel,String x)throws SQLException
    {
        Log.i("database","--------------QUERY STARTED---------------------");
        Cursor c;
        String s[]={ColName1,ColName2};
        d=getReadableDatabase();
        if(x.equals("flag"))
        {
            c=d.query("flag",null,"key"+"='"+sel+"'",null,null,null,null,null);
            c.moveToFirst();
            Log.i("key",(c.getCount()+""));
        }
        else
        {
            if(sel.equals("*"))
            {
                c=d.query(TableName,s,null,null,null,null,null,null);
                c.moveToFirst();
                Log.i("key",(c.getCount()+""));
            }


            else if(sel.equals("rep"))
            {
                int a = x.indexOf(",,");
                String pre1 = x.substring(0, a);
                String mport= x.substring(a + 2, x.length());

                String s1[]={ColName1,ColName2,ColName3};
                c=d.query(TableName,s1,"("+ColName3+"="+pre1+")OR("+ColName3+"="+mport+")",null,null,null,null,null);
                c.moveToFirst();
                Log.i("key",(c.getCount()+""));
            }


            else if(sel.equals("pre1"))
            {

                String s1[]={ColName1,ColName2,ColName3};
                c=d.query(TableName,s1,"("+ColName3+"="+x+")",null,null,null,null,null);
                c.moveToFirst();
                Log.i("key",(c.getCount()+""));
            }






            else
            {
                String s1[]={ColName1,ColName2,ColName3};
                c=d.query(TableName,s1,ColName1+"='"+sel+"'",null,null,null,null,null);
                c.moveToFirst();
                Log.i("key",(c.getCount()+""));
            }
        }

        Log.i("database","--------------QUERY COMPLETED---------------------");
        return c;
    }


    public void delete(String x)
    {
        if(x.equals("clean"))
        {
            d.delete(TableName,null,null);
            //d.delete(TableName,ColName1+"='"+x+"'",null);
        }
        else
        {
            //d.delete(TableName,null,null);
            d.delete(TableName,ColName1+"='"+x+"'",null);
        }

    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}